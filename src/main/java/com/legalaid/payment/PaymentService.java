package com.legalaid.payment;

import com.legalaid.contract.Contract;
import com.legalaid.contract.ContractRepository;
import com.legalaid.contract.ContractStatus;
import com.legalaid.contract.EscrowStatus;
import com.legalaid.notification.NotificationService;
import com.legalaid.payment.dto.*;
import com.legalaid.service.repositories.ServiceRepository;
import com.legalaid.user.User;
import com.legalaid.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository     paymentRepository;
    private final PayoutRepository      payoutRepository;
    private final ContractRepository    contractRepository;
    private final ServiceRepository     serviceRepository;
    private final UserRepository        userRepository;
    private final NotificationService notificationService;

    // ── POST /api/payments — client records bKash payment ────
    // Flow:
    // 1. Client sends bKash payment to LegalAid merchant number
    // 2. Client submits TxnID + sender number here
    // 3. Status = PENDING_VERIFICATION
    // 4. Admin verifies → status = HELD → contract = ACTIVE
    @Transactional
    public PaymentResponse recordPayment(UUID clientId,
                                         RecordPaymentRequest request) {
        // Fetch and validate contract
        Contract contract = contractRepository
                .findByIdAndDeletedAtIsNull(request.getContractId())
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        // Only the client of this contract can record payment
        if (!contract.getClientId().equals(clientId)) {
            throw new RuntimeException("You are not the client of this contract");
        }

        // Contract must be in PENDING_PAYMENT state
        if (contract.getStatus() != ContractStatus.PENDING_PAYMENT) {
            throw new RuntimeException(
                    "Payment cannot be recorded — contract status: "
                            + contract.getStatus());
        }

        // Prevent duplicate payment submissions for same contract
        boolean alreadySubmitted = paymentRepository
                .existsByContractIdAndStatusIn(
                        contract.getId(),
                        List.of(PaymentStatus.PENDING_VERIFICATION,
                                PaymentStatus.HELD));
        if (alreadySubmitted) {
            throw new RuntimeException(
                    "A payment has already been submitted for this contract");
        }

        // Prevent duplicate TxnID across all payments
        if (paymentRepository.findByTxnId(request.getTxnId()).isPresent()) {
            throw new RuntimeException(
                    "This Transaction ID has already been used");
        }

        Payment payment = Payment.builder()
                .contractId(contract.getId())
                .clientId(clientId)
                .amount(contract.getAmount())
                .platformFee(contract.getPlatformFee())
                .method(request.getMethod() != null
                        ? request.getMethod() : PaymentMethod.BKASH)
                .txnId(request.getTxnId())
                .senderNumber(request.getSenderNumber())
                .status(PaymentStatus.PENDING_VERIFICATION)
                .build();

        payment = paymentRepository.save(payment);
        return toPaymentResponse(payment);
    }

    // ── GET /api/payments — own payment history ───────────────
    public List<PaymentResponse> getMyPayments(UUID clientId) {
        return paymentRepository
                .findAllByClientIdOrderByCreatedAtDesc(clientId)
                .stream()
                .map(this::toPaymentResponse)
                .toList();
    }

    // ── GET /api/payments/:id/invoice — get invoice ───────────
    public InvoiceResponse getInvoice(UUID paymentId, UUID requesterId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // Only the client of this payment can get the invoice
        if (!payment.getClientId().equals(requesterId)) {
            throw new RuntimeException(
                    "You do not have access to this invoice");
        }

        Contract contract = contractRepository
                .findByIdAndDeletedAtIsNull(payment.getContractId())
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        User client = userRepository.findActiveById(payment.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        User lawyer = userRepository.findActiveById(contract.getLawyerId())
                .orElse(null);

        String serviceTitle = serviceRepository
                .findByIdAndDeletedAtIsNull(contract.getServiceId())
                .map(s -> s.getTitle())
                .orElse("Service unavailable");

        return InvoiceResponse.builder()
                .invoiceNumber(payment.getId())
                .issuedAt(payment.getCreatedAt())
                .clientId(client.getId())
                .clientName(client.getName())
                .clientEmail(client.getEmail())
                .lawyerId(contract.getLawyerId())
                .lawyerName(lawyer != null ? lawyer.getName() : null)
                .contractId(contract.getId())
                .serviceTitle(serviceTitle)
                .subtotal(payment.getAmount().subtract(payment.getPlatformFee()))
                .platformFee(payment.getPlatformFee())
                .total(payment.getAmount())
                .paymentMethod(payment.getMethod().name())
                .txnId(payment.getTxnId())
                .paymentStatus(payment.getStatus().name())
                .build();
    }

    // ── Admin: verify payment ─────────────────────────────────
    // Called by admin after manually checking TxnID with bKash
    // On success: payment HELD + contract ACTIVE
    @Transactional
    public PaymentResponse verifyPayment(UUID paymentId, UUID adminId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.PENDING_VERIFICATION) {
            throw new RuntimeException(
                    "Payment is not pending verification — status: "
                            + payment.getStatus());
        }

        // Mark payment as held in escrow
        payment.setStatus(PaymentStatus.HELD);
        payment.setVerifiedBy(adminId);
        payment.setVerifiedAt(Instant.now());
        paymentRepository.save(payment);

        // Activate the contract
        Contract contract = contractRepository
                .findByIdAndDeletedAtIsNull(payment.getContractId())
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        contract.setStatus(ContractStatus.ACTIVE);
        contract.setEscrowStatus(EscrowStatus.HELD);
        contractRepository.save(contract);

        // Notification
        notificationService.notifyPaymentReceived(contract.getLawyerId(), contract.getId(), payment.getAmount().toString());

        return toPaymentResponse(payment);
    }

    // ── Admin: reject payment ─────────────────────────────────
    // TxnID was invalid or amount was wrong
    @Transactional
    public PaymentResponse rejectPayment(UUID paymentId, UUID adminId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.PENDING_VERIFICATION) {
            throw new RuntimeException(
                    "Payment is not pending verification — status: "
                            + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setVerifiedBy(adminId);
        payment.setVerifiedAt(Instant.now());
        paymentRepository.save(payment);

        // Notification
        notificationService.notifyPaymentRejected(payment.getClientId(), payment.getContractId());

        return toPaymentResponse(payment);
    }

    // ── Internal: release payment on contract completion ──────
    // Called from ContractService when client marks complete
    // payment HELD → RELEASED + contract escrow RELEASED
    @Transactional
    public void releasePayment(UUID contractId) {
        Payment payment = paymentRepository.findByContractId(contractId)
                .orElseThrow(() -> new RuntimeException(
                        "No payment found for contract: " + contractId));

        if (payment.getStatus() != PaymentStatus.HELD) {
            throw new RuntimeException(
                    "Payment cannot be released — status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.RELEASED);
        payment.setReleasedAt(Instant.now());
        paymentRepository.save(payment);
    }

    // ── Internal: freeze payment on dispute ──────────────────
    // Called from DisputeService when a dispute is raised
    // payment HELD → FROZEN
    @Transactional
    public void freezePayment(UUID contractId) {
        Payment payment = paymentRepository.findByContractId(contractId)
                .orElseThrow(() -> new RuntimeException(
                        "No payment found for contract: " + contractId));

        if (payment.getStatus() != PaymentStatus.HELD) {
            throw new RuntimeException(
                    "Payment cannot be frozen — status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.FROZEN);
        paymentRepository.save(payment);
    }

    // ── Internal: refund payment on cancellation ─────────────
    // Called from ContractService when contract is cancelled
    // payment HELD/FROZEN → REFUNDED
    @Transactional
    public void refundPayment(UUID contractId) {
        paymentRepository.findByContractId(contractId).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.HELD
                    || payment.getStatus() == PaymentStatus.FROZEN) {
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
            }
        });
    }

    // ── POST /api/payouts/request — lawyer requests payout ────
    // Lawyer can only request payout if they have enough
    // released earnings and no pending payout in progress
    @Transactional
    public PayoutResponse requestPayout(UUID lawyerUserId,
                                        RequestPayoutRequest request) {
        // Validate destination — one of mobile or bank must be provided
        if (request.getMobileNumber() == null && request.getBankAccount() == null) {
            throw new RuntimeException(
                    "Either mobile number or bank account is required");
        }

        // Prevent duplicate payout requests while one is in progress
        boolean hasPending = payoutRepository.existsByLawyerIdAndStatusIn(
                lawyerUserId,
                List.of(PayoutStatus.REQUESTED, PayoutStatus.PROCESSING));
        if (hasPending) {
            throw new RuntimeException(
                    "You already have a payout in progress. " +
                            "Wait for it to complete before requesting another.");
        }

        // Check available balance
        BigDecimal totalEarned = paymentRepository
                .sumReleasedEarningsForLawyer(lawyerUserId);
        BigDecimal alreadyPaidOut = payoutRepository
                .sumCompletedPayoutsForLawyer(lawyerUserId);
        BigDecimal availableBalance = totalEarned.subtract(alreadyPaidOut);

        if (request.getAmount().compareTo(availableBalance) > 0) {
            throw new RuntimeException(
                    "Requested amount exceeds available balance. " +
                            "Available: " + availableBalance + " BDT");
        }

        Payout payout = Payout.builder()
                .lawyerId(lawyerUserId)
                .amount(request.getAmount())
                .mobileNumber(request.getMobileNumber())
                .bankAccount(request.getBankAccount())
                .method(request.getMethod())
                .status(PayoutStatus.REQUESTED)
                .build();

        payout = payoutRepository.save(payout);
        return toPayoutResponse(payout);
    }

    // ── GET /api/payouts — lawyer's payout history ────────────
    public List<PayoutResponse> getMyPayouts(UUID lawyerUserId) {
        return payoutRepository
                .findAllByLawyerIdOrderByRequestedAtDesc(lawyerUserId)
                .stream()
                .map(this::toPayoutResponse)
                .toList();
    }

    // ── GET /api/payments/balance — lawyer available balance ──
    public BigDecimal getAvailableBalance(UUID lawyerUserId) {
        BigDecimal totalEarned = paymentRepository
                .sumReleasedEarningsForLawyer(lawyerUserId);
        BigDecimal alreadyPaidOut = payoutRepository
                .sumCompletedPayoutsForLawyer(lawyerUserId);
        return totalEarned.subtract(alreadyPaidOut);
    }

    // ── Mappers ──────────────────────────────────────────────

    private PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .contractId(payment.getContractId())
                .clientId(payment.getClientId())
                .amount(payment.getAmount())
                .platformFee(payment.getPlatformFee())
                .method(payment.getMethod().name())
                .txnId(payment.getTxnId())
                .senderNumber(payment.getSenderNumber())
                .status(payment.getStatus().name())
                .verifiedBy(payment.getVerifiedBy())
                .verifiedAt(payment.getVerifiedAt())
                .releasedAt(payment.getReleasedAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private PayoutResponse toPayoutResponse(Payout payout) {
        return PayoutResponse.builder()
                .id(payout.getId())
                .lawyerId(payout.getLawyerId())
                .amount(payout.getAmount())
                .mobileNumber(payout.getMobileNumber())
                .bankAccount(payout.getBankAccount())
                .method(payout.getMethod().name())
                .status(payout.getStatus().name())
                .processedBy(payout.getProcessedBy())
                .requestedAt(payout.getRequestedAt())
                .processedAt(payout.getProcessedAt())
                .build();
    }
}