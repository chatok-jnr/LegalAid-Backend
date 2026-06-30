package com.legalaid.admin;

import com.legalaid.admin.dto.PlatformStatsResponse;
import com.legalaid.admin.dto.VerifyLawyerRequest;
import com.legalaid.common.response.ApiResponse;
import com.legalaid.lawyer.LawyerProfile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ADMIN')")  // all endpoints in this controller = ADMIN only
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ── GET /api/admin/lawyers/pending ────────────────────────
    // Lawyers who submitted docs — waiting for review
    @GetMapping("/lawyers/pending")
    public ResponseEntity<ApiResponse<Page<LawyerProfile>>> getPendingVerifications(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        ApiResponse<Page<LawyerProfile>> body = ApiResponse.success(
                adminService.getPendingVerifications(page, size)
        );
        return ResponseEntity.ok(body);
    }

    // ── PUT /api/admin/lawyers/:id/verify ─────────────────────
    // Approve or reject a lawyer's verification
    @PutMapping("/lawyers/{id}/verify")
    public ResponseEntity<ApiResponse<Void>> verifyLawyer(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID adminId,
            @Valid @RequestBody VerifyLawyerRequest request) {

        adminService.verifyLawyer(id, adminId, request);
        ApiResponse<Void> body = ApiResponse.success(null);
        return ResponseEntity.ok(body);
    }

    // ── GET /api/admin/payments/pending ───────────────────────
    // Payments waiting for TxnID verification
    @GetMapping("/payments/pending")
    public ResponseEntity<ApiResponse<List<?>>> getPendingPayments() {

        ApiResponse<List<?>> body = ApiResponse.success(
                adminService.getPendingPayments()
        );
        return ResponseEntity.ok(body);
    }

    // ── GET /api/admin/payouts/pending ────────────────────────
    // Payout requests waiting to be sent
    @GetMapping("/payouts/pending")
    public ResponseEntity<ApiResponse<List<?>>> getPendingPayouts() {

        ApiResponse<List<?>> body = ApiResponse.success(
                adminService.getPendingPayouts()
        );
        return ResponseEntity.ok(body);
    }

    // ── PUT /api/admin/payouts/:id/process ───────────────────
    // Mark payout as completed — money sent manually
    @PutMapping("/payouts/{id}/process")
    public ResponseEntity<ApiResponse<Void>> processPayout(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID adminId) {

        adminService.processPayout(id, adminId);
        ApiResponse<Void> body = ApiResponse.success(null);
        return ResponseEntity.ok(body);
    }

    // ── PUT /api/admin/payouts/:id/reject ────────────────────
    @PutMapping("/payouts/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectPayout(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID adminId) {

        adminService.rejectPayout(id, adminId);
        ApiResponse<Void> body = ApiResponse.success(null);
        return ResponseEntity.ok(body);
    }

    // ── GET /api/admin/stats ──────────────────────────────────
    // Platform dashboard stats
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PlatformStatsResponse>> getPlatformStats() {

        ApiResponse<PlatformStatsResponse> body = ApiResponse.success(
                adminService.getPlatformStats()
        );
        return ResponseEntity.ok(body);
    }
}