package com.legalaid.dispute;

import com.legalaid.contract.Contract;
import com.legalaid.contract.ContractRepository;
import com.legalaid.contract.ContractStatus;
import com.legalaid.contract.EscrowStatus;
import com.legalaid.dispute.dto.*;
import com.legalaid.notification.NotificationService;
import com.legalaid.payment.PaymentService;
import com.legalaid.user.User;
import com.legalaid.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeRepository         disputeRepository;
    private final DisputeEvidenceRepository evidenceRepository;
    private final DisputeMessageRepository  messageRepository;
    private final DisputeTimelineRepository timelineRepository;
    private final ContractRepository        contractRepository;
    private final UserRepository            userRepository;
    private final PaymentService            paymentService;
    private final NotificationService       notificationService;

    // ── GET /api/disputes — own disputes ──────────────────────
    public List<DisputeSummaryResponse> getMyDisputes(UUID userId) {
        return disputeRepository.findAllVisibleToUser(userId)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    // ── GET /api/disputes/:id — dispute detail ────────────────
    public DisputeResponse getDisputeDetail(UUID disputeId, UUID requesterId) {
        Dispute dispute = findDisputeWithAccess(disputeId, requesterId);
        return toDetailResponse(dispute);
    }

    // ── POST /api/disputes — raise a dispute ──────────────────
    // Rules:
    // 1. Only on ACTIVE contracts
    // 2. Both client and lawyer can raise
    // 3. No duplicate open disputes on same contract
    // 4. Raises: contract→DISPUTED, payment→FROZEN
    @Transactional
    public DisputeResponse raiseDispute(UUID userId,
                                        RaiseDisputeRequest request) {
        Contract contract = contractRepository
                .findByIdAndDeletedAtIsNull(request.getContractId())
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        // Only participants can raise
        boolean isParticipant = contract.getClientId().equals(userId)
                || contract.getLawyerId().equals(userId);
        if (!isParticipant) {
            throw new RuntimeException(
                    "Only contract participants can raise a dispute");
        }

        // Contract must be ACTIVE
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new RuntimeException(
                    "Disputes can only be raised on active contracts. " +
                            "Current status: " + contract.getStatus());
        }

        // No duplicate open disputes
        boolean hasOpenDispute = disputeRepository.existsByContractIdAndStatusIn(
                contract.getId(),
                List.of(DisputeStatus.AWAITING_RESPONSE,
                        DisputeStatus.OPEN,
                        DisputeStatus.UNDER_REVIEW));
        if (hasOpenDispute) {
            throw new RuntimeException(
                    "There is already an open dispute on this contract");
        }

        // Create dispute
        Dispute dispute = Dispute.builder()
                .contractId(contract.getId())
                .raisedBy(userId)
                .reason(request.getReason())
                .description(request.getDescription())
                .amount(request.getAmount())
                .priority(request.getPriority() != null
                        ? request.getPriority() : DisputePriority.MEDIUM)
                .resolutionDeadline(request.getResolutionDeadline())
                .status(DisputeStatus.AWAITING_RESPONSE)
                .build();

        dispute = disputeRepository.save(dispute);

        // Freeze contract + payment
        contract.setStatus(ContractStatus.DISPUTED);
        contract.setEscrowStatus(EscrowStatus.FROZEN);
        contractRepository.save(contract);
        paymentService.freezePayment(contract.getId());

        // Add opening timeline event
        addTimeline(dispute, DisputeEventType.RAISED, userId,
                "Dispute raised: " + request.getReason());

        // Notify the other party — @Async
        UUID otherPartyId = contract.getClientId().equals(userId)
                ? contract.getLawyerId()
                : contract.getClientId();
        notificationService.notifyDisputeRaised(
                otherPartyId, dispute.getId(), contract.getId());

        return toDetailResponse(dispute);
    }

    // ── POST /api/disputes/:id/evidence — upload evidence ────
    // Both participants can upload evidence
    // Frontend uploads file to Cloudinary first, then sends the URL here
    @Transactional
    public DisputeResponse addEvidence(UUID disputeId,
                                       UUID userId,
                                       String fileUrl,
                                       String cloudinaryId,
                                       String fileName) {
        Dispute dispute = findDisputeWithAccess(disputeId, userId);
        assertNotResolved(dispute);

        evidenceRepository.save(DisputeEvidence.builder()
                .dispute(dispute)
                .uploadedBy(userId)
                .url(fileUrl)
                .cloudinaryId(cloudinaryId)
                .name(fileName)
                .build());

        addTimeline(dispute, DisputeEventType.EVIDENCE_ADDED, userId,
                "Evidence uploaded: " + fileName);

        return toDetailResponse(dispute);
    }

    // ── POST /api/disputes/:id/messages — send message ────────
    // Both participants and admin can message in dispute thread
    @Transactional
    public DisputeResponse sendMessage(UUID disputeId,
                                       UUID senderId,
                                       DisputeMessageRequest request) {
        Dispute dispute = findDisputeWithAccess(disputeId, senderId);
        assertNotResolved(dispute);

        messageRepository.save(DisputeMessage.builder()
                .dispute(dispute)
                .senderId(senderId)
                .text(request.getText())
                .build());

        return toDetailResponse(dispute);
    }

    // ── PUT /api/disputes/:id/respond — other party responds ──
    // When the other party responds, status moves AWAITING_RESPONSE → OPEN
    @Transactional
    public DisputeResponse respondToDispute(UUID disputeId,
                                            UUID responderId,
                                            RespondDisputeRequest request) {
        Dispute dispute = findDisputeWithAccess(disputeId, responderId);

        // Cannot respond to your own dispute
        if (dispute.getRaisedBy().equals(responderId)) {
            throw new RuntimeException(
                    "The party who raised the dispute cannot respond to it. " +
                            "Please wait for the other party.");
        }

        assertNotResolved(dispute);

        // Save response as a message
        messageRepository.save(DisputeMessage.builder()
                .dispute(dispute)
                .senderId(responderId)
                .text(request.getResponse())
                .build());

        // Move status to OPEN once other party responds
        if (dispute.getStatus() == DisputeStatus.AWAITING_RESPONSE) {
            dispute.setStatus(DisputeStatus.OPEN);
            disputeRepository.save(dispute);
        }

        // Determine event type based on who responded
        Contract contract = contractRepository
                .findByIdAndDeletedAtIsNull(dispute.getContractId())
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        DisputeEventType eventType = contract.getLawyerId().equals(responderId)
                ? DisputeEventType.LAWYER_RESPONDED
                : DisputeEventType.CLIENT_RESPONDED;

        addTimeline(dispute, eventType, responderId, "Response submitted.");

        return toDetailResponse(dispute);
    }

    // ── PUT /api/admin/disputes/:id/resolve — admin resolves ──
    // Admin makes final decision:
    // REFUND_CLIENT → cancel contract + refund payment to client
    // RELEASE_TO_LAWYER → complete contract + release payment to lawyer
    @Transactional
    public DisputeResponse resolveDispute(UUID disputeId,
                                          UUID adminId,
                                          ResolveDisputeRequest request) {
        Dispute dispute = disputeRepository.findByIdAndDeletedAtIsNull(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found"));

        if (dispute.getStatus() == DisputeStatus.RESOLVED
                || dispute.getStatus() == DisputeStatus.CLOSED) {
            throw new RuntimeException(
                    "Dispute is already resolved");
        }

        Contract contract = contractRepository
                .findByIdAndDeletedAtIsNull(dispute.getContractId())
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        // Apply decision
        if (request.getDecision() ==
                ResolveDisputeRequest.DisputeDecision.REFUND_CLIENT) {
            contract.setStatus(ContractStatus.CANCELLED);
            contract.setEscrowStatus(EscrowStatus.REFUNDED);
            contractRepository.save(contract);
            paymentService.refundPayment(contract.getId());

            notificationService.notifyPaymentRefunded(
                    contract.getClientId(),
                    contract.getId(),
                    contract.getAmount().toString());

        } else {
            // RELEASE_TO_LAWYER
            contract.setStatus(ContractStatus.COMPLETED);
            contract.setEscrowStatus(EscrowStatus.RELEASED);
            contractRepository.save(contract);
            paymentService.releasePayment(contract.getId());

            notificationService.notifyPaymentReleased(
                    contract.getLawyerId(),
                    contract.getId(),
                    contract.getAmount()
                            .subtract(contract.getPlatformFee()).toString());
        }

        // Close the dispute
        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolvedBy(adminId);
        dispute.setResolutionNote(request.getResolutionNote());
        dispute.setResolvedAt(Instant.now());
        disputeRepository.save(dispute);

        addTimeline(dispute, DisputeEventType.RESOLVED, adminId,
                "Dispute resolved: " + request.getResolutionNote());

        // Notify both parties — @Async
        notificationService.notifyDisputeResolved(
                contract.getClientId(),
                dispute.getId(),
                request.getResolutionNote());
        notificationService.notifyDisputeResolved(
                contract.getLawyerId(),
                dispute.getId(),
                request.getResolutionNote());

        return toDetailResponse(dispute);
    }

    // ── Admin: get all disputes paginated ─────────────────────
    public Page<DisputeSummaryResponse> getAllDisputes(DisputeStatus status,
                                                       int page,
                                                       int size) {
        int cappedSize = Math.min(size, 50);
        PageRequest pageable = PageRequest.of(page, cappedSize);

        Page<Dispute> disputes = (status != null)
                ? disputeRepository.findAllByStatusAndDeletedAtIsNull(status, pageable)
                : disputeRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc(pageable);

        return disputes.map(this::toSummaryResponse);
    }

    // ── Access helpers ───────────────────────────────────────

    private Dispute findDisputeWithAccess(UUID disputeId, UUID userId) {
        Dispute dispute = disputeRepository.findByIdAndDeletedAtIsNull(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found"));

        Contract contract = contractRepository
                .findByIdAndDeletedAtIsNull(dispute.getContractId())
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        boolean isParticipant = contract.getClientId().equals(userId)
                || contract.getLawyerId().equals(userId);

        if (!isParticipant) {
            throw new RuntimeException(
                    "You do not have access to this dispute");
        }
        return dispute;
    }

    private void assertNotResolved(Dispute dispute) {
        if (dispute.getStatus() == DisputeStatus.RESOLVED
                || dispute.getStatus() == DisputeStatus.CLOSED) {
            throw new RuntimeException(
                    "This dispute has already been resolved");
        }
    }

    // ── Timeline helper ──────────────────────────────────────
    private void addTimeline(Dispute dispute,
                             DisputeEventType type,
                             UUID actorId,
                             String text) {
        timelineRepository.save(DisputeTimeline.builder()
                .dispute(dispute)
                .type(type)
                .actorId(actorId)
                .text(text)
                .build());
    }

    // ── Mappers ──────────────────────────────────────────────

    private DisputeSummaryResponse toSummaryResponse(Dispute dispute) {
        return DisputeSummaryResponse.builder()
                .id(dispute.getId())
                .contractId(dispute.getContractId())
                .raisedBy(dispute.getRaisedBy())
                .reason(dispute.getReason())
                .amount(dispute.getAmount())
                .status(dispute.getStatus().name())
                .priority(dispute.getPriority().name())
                .resolutionDeadline(dispute.getResolutionDeadline())
                .createdAt(dispute.getCreatedAt())
                .updatedAt(dispute.getUpdatedAt())
                .build();
    }

    private DisputeResponse toDetailResponse(Dispute dispute) {
        User raisedByUser = userRepository
                .findActiveById(dispute.getRaisedBy()).orElse(null);

        List<DisputeResponse.EvidenceDto> evidence =
                evidenceRepository.findAllByDispute_IdOrderByUploadedAtAsc(dispute.getId())
                        .stream()
                        .map(e -> {
                            User uploader = userRepository
                                    .findActiveById(e.getUploadedBy()).orElse(null);
                            return DisputeResponse.EvidenceDto.builder()
                                    .id(e.getId())
                                    .uploadedBy(e.getUploadedBy())
                                    .uploadedByName(uploader != null
                                            ? uploader.getName() : null)
                                    .url(e.getUrl())
                                    .name(e.getName())
                                    .uploadedAt(e.getUploadedAt())
                                    .build();
                        })
                        .toList();

        List<DisputeResponse.MessageDto> messages =
                messageRepository.findAllByDispute_IdOrderByCreatedAtAsc(dispute.getId())
                        .stream()
                        .map(m -> {
                            User sender = userRepository
                                    .findActiveById(m.getSenderId()).orElse(null);
                            return DisputeResponse.MessageDto.builder()
                                    .id(m.getId())
                                    .senderId(m.getSenderId())
                                    .senderName(sender != null
                                            ? sender.getName() : null)
                                    .text(m.getText())
                                    .createdAt(m.getCreatedAt())
                                    .build();
                        })
                        .toList();

        List<DisputeResponse.TimelineDto> timeline =
                timelineRepository.findAllByDispute_IdOrderByCreatedAtAsc(dispute.getId())
                        .stream()
                        .map(t -> DisputeResponse.TimelineDto.builder()
                                .type(t.getType().name())
                                .text(t.getText())
                                .actorId(t.getActorId())
                                .createdAt(t.getCreatedAt())
                                .build())
                        .toList();

        return DisputeResponse.builder()
                .id(dispute.getId())
                .contractId(dispute.getContractId())
                .raisedBy(dispute.getRaisedBy())
                .raisedByName(raisedByUser != null ? raisedByUser.getName() : null)
                .reason(dispute.getReason())
                .description(dispute.getDescription())
                .amount(dispute.getAmount())
                .status(dispute.getStatus().name())
                .priority(dispute.getPriority().name())
                .resolutionDeadline(dispute.getResolutionDeadline())
                .resolvedBy(dispute.getResolvedBy())
                .resolutionNote(dispute.getResolutionNote())
                .resolvedAt(dispute.getResolvedAt())
                .evidence(evidence)
                .messages(messages)
                .timeline(timeline)
                .createdAt(dispute.getCreatedAt())
                .updatedAt(dispute.getUpdatedAt())
                .build();
    }
}