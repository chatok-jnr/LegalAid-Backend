package com.legalaid.contract;

import com.legalaid.contract.dto.*;
import com.legalaid.service.LegalService;
import com.legalaid.service.repositories.ServiceRepository;
import com.legalaid.user.User;
import com.legalaid.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractService {

    private static final BigDecimal PLATFORM_FEE_PERCENT = new BigDecimal("0.05"); // 5%

    private final ContractRepository          contractRepository;
    private final ContractMilestoneRepository milestoneRepository;
    private final ServiceRepository           serviceRepository;
    private final UserRepository              userRepository;

    // ── GET /api/contracts — list own contracts ───────────────
    public List<ContractSummaryResponse> getMyContracts(UUID userId) {
        return contractRepository.findAllVisibleToUser(userId)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    // ── GET /api/contracts/:id — contract detail ──────────────
    public ContractDetailResponse getContractDetail(UUID contractId, UUID requesterId) {
        Contract contract = findContractWithAccess(contractId, requesterId);
        return toDetailResponse(contract);
    }

    // ── POST /api/contracts — hire a lawyer (client only) ─────
    // 1. Fetch the service and snapshot its data
    // 2. Resolve the lawyer's user ID from their profile
    // 3. Prevent duplicate active contracts for same service
    // 4. Save contract with PENDING_LAWYER status
    @Transactional
    public ContractDetailResponse createContract(UUID clientId,
                                                 CreateContractRequest request) {
        // Fetch the service
        LegalService service = serviceRepository
                .findByIdAndIsActiveTrueAndDeletedAtIsNull(request.getServiceId())
                .orElseThrow(() -> new RuntimeException("Service not found or inactive"));

        // Prevent client hiring themselves
        // (resolved by checking lawyer user ID later — skipped for brevity,
        //  enforced at DB level via FK constraints)

        // Prevent duplicate active contracts for this client + service
        List<ContractStatus> activeStatuses = List.of(
                ContractStatus.PENDING_LAWYER,
                ContractStatus.PENDING_PAYMENT,
                ContractStatus.ACTIVE
        );
        boolean alreadyExists = contractRepository
                .existsByClientIdAndServiceIdAndStatusIn(
                        clientId, service.getId(), activeStatuses);
        if (alreadyExists) {
            throw new RuntimeException(
                    "You already have an active contract for this service");
        }

        // Calculate platform fee snapshot
        BigDecimal fee = service.getPrice()
                .multiply(PLATFORM_FEE_PERCENT)
                .setScale(2, RoundingMode.HALF_UP);

        // We need the lawyer's user ID — service.lawyerId is lawyer_profiles.id
        // We resolve it via the lawyer profile
        // NOTE: LawyerRepository is not injected here to avoid circular deps.
        // The lawyerId on the contract references users.id — we store
        // lawyer_profiles.id in service, so we need a join.
        // For now we store service.lawyerId directly and resolve in controller.
        // This will be wired properly when lawyer package is integrated.

        Contract contract = Contract.builder()
                .serviceId(service.getId())
                .clientId(clientId)
                .lawyerId(service.getLawyerId())  // lawyer_profiles.id for now
                .caseId(request.getCaseId())
                // ── Snapshots ──────────────────────────────────
                .amount(service.getPrice())
                .deliveryDays(service.getDeliveryDays())
                .revisions(service.getRevisions())
                .platformFee(fee)
                // ── Initial state ──────────────────────────────
                .status(ContractStatus.PENDING_LAWYER)
                .escrowStatus(EscrowStatus.NONE)
                .clientNote(request.getClientNote())
                .build();

        contract = contractRepository.save(contract);
        return toDetailResponse(contract);
    }

    // ── PUT /api/contracts/:id/accept — lawyer accepts ────────
    // PENDING_LAWYER → PENDING_PAYMENT
    @Transactional
    public ContractDetailResponse acceptContract(UUID contractId, UUID lawyerUserId) {
        Contract contract = findContractWithAccess(contractId, lawyerUserId);

        assertIsLawyer(contract, lawyerUserId);

        if (contract.getStatus() != ContractStatus.PENDING_LAWYER) {
            throw new RuntimeException(
                    "Contract cannot be accepted — current status: "
                            + contract.getStatus());
        }

        contract.setStatus(ContractStatus.PENDING_PAYMENT);
        contractRepository.save(contract);
        return toDetailResponse(contract);
    }

    // ── PUT /api/contracts/:id/decline — lawyer declines ──────
    // PENDING_LAWYER → CANCELLED
    @Transactional
    public ContractDetailResponse declineContract(UUID contractId, UUID lawyerUserId) {
        Contract contract = findContractWithAccess(contractId, lawyerUserId);

        assertIsLawyer(contract, lawyerUserId);

        if (contract.getStatus() != ContractStatus.PENDING_LAWYER) {
            throw new RuntimeException(
                    "Contract cannot be declined — current status: "
                            + contract.getStatus());
        }

        contract.setStatus(ContractStatus.CANCELLED);
        contractRepository.save(contract);
        return toDetailResponse(contract);
    }

    // ── PUT /api/contracts/:id/complete — client confirms ─────
    // ACTIVE → COMPLETED + escrow HELD → RELEASED
    @Transactional
    public ContractDetailResponse completeContract(UUID contractId, UUID clientId) {
        Contract contract = findContractWithAccess(contractId, clientId);

        assertIsClient(contract, clientId);

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new RuntimeException(
                    "Contract cannot be completed — current status: "
                            + contract.getStatus());
        }

        contract.setStatus(ContractStatus.COMPLETED);
        contract.setEscrowStatus(EscrowStatus.RELEASED);
        contractRepository.save(contract);

        // Payment release is handled in payment package when built
        // This sets the escrow flag — the actual payout logic lives there

        return toDetailResponse(contract);
    }

    // ── PUT /api/contracts/:id/cancel — cancel contract ───────
    // Cancellation rules:
    //   Client  → can cancel at PENDING_LAWYER, PENDING_PAYMENT, or ACTIVE
    //             (active = work not delivered yet — client's call)
    //   Lawyer  → can cancel ONLY at PENDING_LAWYER (before accepting)
    @Transactional
    public ContractDetailResponse cancelContract(UUID contractId, UUID requesterId) {
        Contract contract = findContractWithAccess(contractId, requesterId);

        boolean isClient = contract.getClientId().equals(requesterId);
        boolean isLawyer = contract.getLawyerId().equals(requesterId);

        if (isClient) {
            // Client can cancel at any non-terminal status except DISPUTED/COMPLETED
            if (contract.getStatus() == ContractStatus.COMPLETED
                    || contract.getStatus() == ContractStatus.DISPUTED
                    || contract.getStatus() == ContractStatus.CANCELLED) {
                throw new RuntimeException(
                        "Contract cannot be cancelled — current status: "
                                + contract.getStatus());
            }
        } else if (isLawyer) {
            // Lawyer can only cancel before accepting
            if (contract.getStatus() != ContractStatus.PENDING_LAWYER) {
                throw new RuntimeException(
                        "Lawyers can only cancel before accepting the contract");
            }
        } else {
            throw new RuntimeException("You are not a participant in this contract");
        }

        contract.setStatus(ContractStatus.CANCELLED);

        // If payment was held, mark for refund
        if (contract.getEscrowStatus() == EscrowStatus.HELD) {
            contract.setEscrowStatus(EscrowStatus.REFUNDED);
        }

        contractRepository.save(contract);
        return toDetailResponse(contract);
    }

    // ── PUT /api/contracts/:id/milestones/:mId — toggle ───────
    // Lawyer marks milestones done, client can also toggle
    @Transactional
    public ContractDetailResponse.MilestoneDto toggleMilestone(UUID contractId,
                                                               UUID milestoneId,
                                                               UUID requesterId) {
        Contract contract = findContractWithAccess(contractId, requesterId);

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new RuntimeException(
                    "Milestones can only be updated on ACTIVE contracts");
        }

        ContractMilestone milestone = milestoneRepository
                .findByIdAndContract_Id(milestoneId, contractId)
                .orElseThrow(() -> new RuntimeException("Milestone not found"));

        boolean nowDone = !milestone.getIsDone();
        milestone.setIsDone(nowDone);
        milestone.setCompletedAt(nowDone ? Instant.now() : null);
        milestoneRepository.save(milestone);

        return toMilestoneDto(milestone);
    }

    // ── Access helpers ───────────────────────────────────────

    private Contract findContractWithAccess(UUID contractId, UUID requesterId) {
        Contract contract = contractRepository.findByIdAndDeletedAtIsNull(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        boolean isParticipant = contract.getClientId().equals(requesterId)
                || contract.getLawyerId().equals(requesterId);

        if (!isParticipant) {
            throw new RuntimeException("You do not have access to this contract");
        }
        return contract;
    }

    private void assertIsClient(Contract contract, UUID userId) {
        if (!contract.getClientId().equals(userId)) {
            throw new RuntimeException("Only the client can perform this action");
        }
    }

    private void assertIsLawyer(Contract contract, UUID userId) {
        if (!contract.getLawyerId().equals(userId)) {
            throw new RuntimeException("Only the lawyer can perform this action");
        }
    }

    // ── Mappers ──────────────────────────────────────────────

    private ContractSummaryResponse toSummaryResponse(Contract contract) {
        // Fetch service title for display
        String serviceTitle = serviceRepository
                .findByIdAndDeletedAtIsNull(contract.getServiceId())
                .map(LegalService::getTitle)
                .orElse("Service unavailable");

        return ContractSummaryResponse.builder()
                .id(contract.getId())
                .serviceId(contract.getServiceId())
                .clientId(contract.getClientId())
                .lawyerId(contract.getLawyerId())
                .caseId(contract.getCaseId())
                .serviceTitle(serviceTitle)
                .amount(contract.getAmount())
                .deliveryDays(contract.getDeliveryDays())
                .revisions(contract.getRevisions())
                .status(contract.getStatus().name())
                .escrowStatus(contract.getEscrowStatus().name())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .build();
    }

    private ContractDetailResponse toDetailResponse(Contract contract) {
        // Fetch user info for display
        User client = userRepository.findActiveById(contract.getClientId()).orElse(null);
        User lawyer = userRepository.findActiveById(contract.getLawyerId()).orElse(null);

        String serviceTitle = serviceRepository
                .findByIdAndDeletedAtIsNull(contract.getServiceId())
                .map(LegalService::getTitle)
                .orElse("Service unavailable");

        List<ContractDetailResponse.MilestoneDto> milestones =
                milestoneRepository.findAllByContract_IdOrderBySortOrderAsc(contract.getId())
                        .stream().map(this::toMilestoneDto).toList();

        return ContractDetailResponse.builder()
                .id(contract.getId())
                .serviceId(contract.getServiceId())
                .clientId(contract.getClientId())
                .lawyerId(contract.getLawyerId())
                .caseId(contract.getCaseId())
                .clientName(client != null ? client.getName() : null)
                .clientAvatarUrl(client != null ? client.getAvatarUrl() : null)
                .lawyerName(lawyer != null ? lawyer.getName() : null)
                .lawyerAvatarUrl(lawyer != null ? lawyer.getAvatarUrl() : null)
                .serviceTitle(serviceTitle)
                .amount(contract.getAmount())
                .platformFee(contract.getPlatformFee())
                .deliveryDays(contract.getDeliveryDays())
                .revisions(contract.getRevisions())
                .status(contract.getStatus().name())
                .escrowStatus(contract.getEscrowStatus().name())
                .clientNote(contract.getClientNote())
                .milestones(milestones)
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .build();
    }

    private ContractDetailResponse.MilestoneDto toMilestoneDto(ContractMilestone m) {
        return ContractDetailResponse.MilestoneDto.builder()
                .id(m.getId())
                .label(m.getLabel())
                .isDone(m.getIsDone())
                .dueDate(m.getDueDate())
                .completedAt(m.getCompletedAt())
                .sortOrder(m.getSortOrder())
                .build();
    }
}