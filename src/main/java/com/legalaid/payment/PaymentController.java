package com.legalaid.payment;

import com.legalaid.common.response.ApiResponse;
import com.legalaid.payment.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ═══════════════════════════════════════════════════════
    //  PAYMENT ENDPOINTS
    // ═══════════════════════════════════════════════════════

    // ── POST /api/payments ────────────────────────────────────
    // Client submits bKash TxnID after sending money manually
    @PostMapping("/api/payments")
    @PreAuthorize("hasAuthority('CLIENT')")
    public ResponseEntity<ApiResponse<PaymentResponse>> recordPayment(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody RecordPaymentRequest request) {

        ApiResponse<PaymentResponse> body = ApiResponse.success(
                paymentService.recordPayment(userId, request)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // ── GET /api/payments ─────────────────────────────────────
    // Client's own payment history
    @GetMapping("/api/payments")
    @PreAuthorize("hasAuthority('CLIENT')")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getMyPayments(
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<List<PaymentResponse>> body = ApiResponse.success(
                paymentService.getMyPayments(userId)
        );
        return ResponseEntity.ok(body);
    }

    // ── GET /api/payments/:id/invoice ─────────────────────────
    // Client downloads invoice for a specific payment
    @GetMapping("/api/payments/{id}/invoice")
    @PreAuthorize("hasAuthority('CLIENT')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<InvoiceResponse> body = ApiResponse.success(
                paymentService.getInvoice(id, userId)
        );
        return ResponseEntity.ok(body);
    }

    // ── GET /api/payments/balance ─────────────────────────────
    // Lawyer checks available payout balance
    // Balance = total released earnings - total completed payouts
    @GetMapping("/api/payments/balance")
    @PreAuthorize("hasAuthority('LAWYER')")
    public ResponseEntity<ApiResponse<BigDecimal>> getAvailableBalance(
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<BigDecimal> body = ApiResponse.success(
                paymentService.getAvailableBalance(userId)
        );
        return ResponseEntity.ok(body);
    }

    // ═══════════════════════════════════════════════════════
    //  PAYOUT ENDPOINTS
    // ═══════════════════════════════════════════════════════

    // ── POST /api/payouts/request ─────────────────────────────
    // Lawyer requests a withdrawal of their released earnings
    @PostMapping("/api/payouts/request")
    @PreAuthorize("hasAuthority('LAWYER')")
    public ResponseEntity<ApiResponse<PayoutResponse>> requestPayout(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody RequestPayoutRequest request) {

        ApiResponse<PayoutResponse> body = ApiResponse.success(
                paymentService.requestPayout(userId, request)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // ── GET /api/payouts ──────────────────────────────────────
    // Lawyer's full payout history
    @GetMapping("/api/payouts")
    @PreAuthorize("hasAuthority('LAWYER')")
    public ResponseEntity<ApiResponse<List<PayoutResponse>>> getMyPayouts(
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<List<PayoutResponse>> body = ApiResponse.success(
                paymentService.getMyPayouts(userId)
        );
        return ResponseEntity.ok(body);
    }

    // ═══════════════════════════════════════════════════════
    //  ADMIN ENDPOINTS
    // ═══════════════════════════════════════════════════════

    // ── PUT /api/admin/payments/:id/verify ────────────────────
    // Admin manually verifies TxnID and activates escrow
    // On success: payment HELD + contract ACTIVE
    @PutMapping("/api/admin/payments/{id}/verify")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID adminId) {

        ApiResponse<PaymentResponse> body = ApiResponse.success(
                paymentService.verifyPayment(id, adminId)
        );
        return ResponseEntity.ok(body);
    }

    // ── PUT /api/admin/payments/:id/reject ────────────────────
    // Admin rejects TxnID — client must resubmit
    @PutMapping("/api/admin/payments/{id}/reject")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> rejectPayment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID adminId) {

        ApiResponse<PaymentResponse> body = ApiResponse.success(
                paymentService.rejectPayment(id, adminId)
        );
        return ResponseEntity.ok(body);
    }
}