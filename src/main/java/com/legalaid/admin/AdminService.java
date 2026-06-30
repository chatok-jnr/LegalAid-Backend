package com.legalaid.admin;

import com.legalaid.admin.dto.PlatformStatsResponse;
import com.legalaid.admin.dto.VerifyLawyerRequest;
import com.legalaid.contract.ContractRepository;
import com.legalaid.contract.ContractStatus;
import com.legalaid.dispute.DisputeRepository;
import com.legalaid.dispute.DisputeStatus;
import com.legalaid.lawyer.LawyerProfile;
import com.legalaid.lawyer.repositories.LawyerRepository;
import com.legalaid.lawyer.VerificationStatus;
import com.legalaid.lawyer.dto.LawyerProfileResponse;
import com.legalaid.notification.NotificationService;
import com.legalaid.payment.PaymentRepository;
import com.legalaid.payment.PaymentStatus;
import com.legalaid.payment.PayoutRepository;
import com.legalaid.payment.PayoutStatus;
import com.legalaid.service.repositories.ServiceRepository;
import com.legalaid.user.User;
import com.legalaid.user.UserRepository;
import com.legalaid.user.UserRole;
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
public class AdminService {

    private final UserRepository         userRepository;
    private final LawyerRepository       lawyerRepository;
    private final ServiceRepository      serviceRepository;
    private final ContractRepository     contractRepository;
    private final PaymentRepository      paymentRepository;
    private final PayoutRepository       payoutRepository;
    private final DisputeRepository      disputeRepository;
    private final NotificationService    notificationService;

    // ── GET /api/admin/lawyers/pending ────────────────────────
    // Lawyers who have submitted verification docs
    public Page<LawyerProfile> getPendingVerifications(int page, int size) {
        return lawyerRepository.findAllByVerificationStatus(
                VerificationStatus.PENDING,
                PageRequest.of(page, Math.min(size, 50)));
    }

    // ── PUT /api/admin/lawyers/:id/verify ─────────────────────
    // Approve → role = LAWYER, verified_at = now
    // Reject  → verification_status = REJECTED, notify with reason
    @Transactional
    public void verifyLawyer(UUID lawyerProfileId,
                             UUID adminId,
                             VerifyLawyerRequest request) {

        LawyerProfile profile = lawyerRepository.findById(lawyerProfileId)
                .orElseThrow(() -> new RuntimeException("Lawyer profile not found"));

        if (profile.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new RuntimeException(
                    "Lawyer is not pending verification. Status: "
                            + profile.getVerificationStatus());
        }

        User user = userRepository.findActiveById(profile.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getDecision() == VerifyLawyerRequest.Decision.APPROVE) {
            // Grant lawyer role
            profile.setVerificationStatus(VerificationStatus.APPROVED);
            profile.setVerifiedAt(Instant.now());
            lawyerRepository.save(profile);

            user.setRole(UserRole.LAWYER);
            userRepository.save(user);

            // Notify lawyer — @Async
            notificationService.notifyVerificationApproved(user.getId());

        } else {
            // Reject
            if (request.getRejectionReason() == null
                    || request.getRejectionReason().isBlank()) {
                throw new RuntimeException(
                        "Rejection reason is required when rejecting a lawyer");
            }

            profile.setVerificationStatus(VerificationStatus.REJECTED);
            lawyerRepository.save(profile);

            // Notify lawyer with reason — @Async
            notificationService.notifyVerificationRejected(
                    user.getId(), request.getRejectionReason());
        }
    }

    // ── GET /api/admin/payments/pending ───────────────────────
    // Payments waiting for TxnID verification
    // Note: verify/reject endpoints already in PaymentController (ADMIN guarded)
    public List<?> getPendingPayments() {
        return paymentRepository
                .findAllByStatusOrderByCreatedAtAsc(PaymentStatus.PENDING_VERIFICATION);
    }

    // ── GET /api/admin/payouts/pending ────────────────────────
    // Payout requests waiting to be processed
    public List<?> getPendingPayouts() {
        return payoutRepository
                .findAllByStatusOrderByRequestedAtAsc(PayoutStatus.REQUESTED);
    }

    // ── PUT /api/admin/payouts/:id/process ───────────────────
    // Admin marks a payout as processed (money sent manually)
    @Transactional
    public void processPayout(UUID payoutId, UUID adminId) {
        var payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Payout not found"));

        if (payout.getStatus() != PayoutStatus.REQUESTED
                && payout.getStatus() != PayoutStatus.PROCESSING) {
            throw new RuntimeException(
                    "Payout cannot be processed — status: " + payout.getStatus());
        }

        payout.setStatus(PayoutStatus.COMPLETED);
        payout.setProcessedBy(adminId);
        payout.setProcessedAt(Instant.now());
        payoutRepository.save(payout);
    }

    // ── PUT /api/admin/payouts/:id/reject ────────────────────
    @Transactional
    public void rejectPayout(UUID payoutId, UUID adminId) {
        var payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Payout not found"));

        payout.setStatus(PayoutStatus.REJECTED);
        payout.setProcessedBy(adminId);
        payout.setProcessedAt(Instant.now());
        payoutRepository.save(payout);
    }

    // ── GET /api/admin/stats ──────────────────────────────────
    // Platform overview stats for admin dashboard
    public PlatformStatsResponse getPlatformStats() {
        // User counts
        long totalUsers   = userRepository.count();
        long totalClients = userRepository.countByRole(UserRole.CLIENT);
        long totalLawyers = userRepository.countByRole(UserRole.LAWYER);
        long totalAdmins  = userRepository.countByRole(UserRole.ADMIN);

        // Lawyer counts
        long pendingVerif = lawyerRepository
                .countByVerificationStatus(VerificationStatus.PENDING);
        long approvedLawyers = lawyerRepository
                .countByVerificationStatus(VerificationStatus.APPROVED);

        // Service counts
        long totalServices  = serviceRepository.countByDeletedAtIsNull();
        long activeServices = serviceRepository
                .countByIsActiveTrueAndDeletedAtIsNull();

        // Contract counts
        long totalContracts     = contractRepository.countByDeletedAtIsNull();
        long activeContracts    = contractRepository
                .countByStatusAndDeletedAtIsNull(ContractStatus.ACTIVE);
        long completedContracts = contractRepository
                .countByStatusAndDeletedAtIsNull(ContractStatus.COMPLETED);
        long disputedContracts  = contractRepository
                .countByStatusAndDeletedAtIsNull(ContractStatus.DISPUTED);
        long cancelledContracts = contractRepository
                .countByStatusAndDeletedAtIsNull(ContractStatus.CANCELLED);

        // Payment stats
        long totalPayments   = paymentRepository.count();
        long pendingPayments = paymentRepository
                .countByStatus(PaymentStatus.PENDING_VERIFICATION);
        var totalRevenue     = paymentRepository.sumPlatformFees();
        var pendingPayouts   = payoutRepository.sumRequestedPayouts();

        // Dispute counts
        long openDisputes = disputeRepository.countByStatusInAndDeletedAtIsNull(
                List.of(DisputeStatus.AWAITING_RESPONSE,
                        DisputeStatus.OPEN,
                        DisputeStatus.UNDER_REVIEW));
        long resolvedDisputes = disputeRepository
                .countByStatusAndDeletedAtIsNull(DisputeStatus.RESOLVED);

        return PlatformStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalClients(totalClients)
                .totalLawyers(totalLawyers)
                .totalAdmins(totalAdmins)
                .pendingVerifications(pendingVerif)
                .approvedLawyers(approvedLawyers)
                .totalServices(totalServices)
                .activeServices(activeServices)
                .totalContracts(totalContracts)
                .activeContracts(activeContracts)
                .completedContracts(completedContracts)
                .disputedContracts(disputedContracts)
                .cancelledContracts(cancelledContracts)
                .totalPayments(totalPayments)
                .pendingPayments(pendingPayments)
                .totalRevenue(totalRevenue)
                .totalPayoutsPending(pendingPayouts)
                .openDisputes(openDisputes)
                .resolvedDisputes(resolvedDisputes)
                .build();
    }
}