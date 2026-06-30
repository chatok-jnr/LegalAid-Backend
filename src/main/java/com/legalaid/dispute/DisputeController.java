package com.legalaid.dispute;

import com.legalaid.common.response.ApiResponse;
import com.legalaid.dispute.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    // ── GET /api/disputes ─────────────────────────────────────
    // All disputes visible to the authenticated user
    @GetMapping("/api/disputes")
    public ResponseEntity<ApiResponse<List<DisputeSummaryResponse>>> getMyDisputes(
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<List<DisputeSummaryResponse>> body = ApiResponse.success(
                disputeService.getMyDisputes(userId)
        );
        return ResponseEntity.ok(body);
    }

    // ── GET /api/disputes/:id ─────────────────────────────────
    // Full dispute detail — participants only
    @GetMapping("/api/disputes/{id}")
    public ResponseEntity<ApiResponse<DisputeResponse>> getDisputeDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<DisputeResponse> body = ApiResponse.success(
                disputeService.getDisputeDetail(id, userId)
        );
        return ResponseEntity.ok(body);
    }

    // ── POST /api/disputes ────────────────────────────────────
    // Raise a dispute on an ACTIVE contract
    // Both client and lawyer can raise
    @PostMapping("/api/disputes")
    public ResponseEntity<ApiResponse<DisputeResponse>> raiseDispute(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody RaiseDisputeRequest request) {

        ApiResponse<DisputeResponse> body = ApiResponse.success(
                disputeService.raiseDispute(userId, request)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // ── POST /api/disputes/:id/evidence ───────────────────────
    // Upload evidence — frontend uploads to Cloudinary first,
    // then sends the URL here
    @PostMapping("/api/disputes/{id}/evidence")
    public ResponseEntity<ApiResponse<DisputeResponse>> addEvidence(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId,
            @RequestParam @NotBlank String fileUrl,
            @RequestParam(required = false) String cloudinaryId,
            @RequestParam(required = false) String fileName) {

        ApiResponse<DisputeResponse> body = ApiResponse.success(
                disputeService.addEvidence(id, userId, fileUrl, cloudinaryId, fileName)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // ── POST /api/disputes/:id/messages ───────────────────────
    // Send message in dispute thread — both participants
    @PostMapping("/api/disputes/{id}/messages")
    public ResponseEntity<ApiResponse<DisputeResponse>> sendMessage(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody DisputeMessageRequest request) {

        ApiResponse<DisputeResponse> body = ApiResponse.success(
                disputeService.sendMessage(id, userId, request)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // ── PUT /api/disputes/:id/respond ─────────────────────────
    // The OTHER party responds to the dispute
    // Moves status AWAITING_RESPONSE → OPEN
    @PutMapping("/api/disputes/{id}/respond")
    public ResponseEntity<ApiResponse<DisputeResponse>> respondToDispute(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody RespondDisputeRequest request) {

        ApiResponse<DisputeResponse> body = ApiResponse.success(
                disputeService.respondToDispute(id, userId, request)
        );
        return ResponseEntity.ok(body);
    }

    // ── GET /api/admin/disputes ───────────────────────────────
    // Admin — all disputes paginated, filterable by status
    @GetMapping("/api/admin/disputes")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Page<DisputeSummaryResponse>>> getAllDisputes(
            @RequestParam(required = false) DisputeStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        ApiResponse<Page<DisputeSummaryResponse>> body = ApiResponse.success(
                disputeService.getAllDisputes(status, page, size)
        );
        return ResponseEntity.ok(body);
    }

    // ── PUT /api/admin/disputes/:id/resolve ───────────────────
    // Admin issues final decision
    // REFUND_CLIENT → cancel + refund
    // RELEASE_TO_LAWYER → complete + release payment
    @PutMapping("/api/admin/disputes/{id}/resolve")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<DisputeResponse>> resolveDispute(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID adminId,
            @Valid @RequestBody ResolveDisputeRequest request) {

        ApiResponse<DisputeResponse> body = ApiResponse.success(
                disputeService.resolveDispute(id, adminId, request)
        );
        return ResponseEntity.ok(body);
    }
}