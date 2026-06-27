package com.legalaid.contract;

import com.legalaid.common.response.ApiResponse;
import com.legalaid.contract.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    // ── GET /api/contracts ────────────────────────────────────
    // Returns all contracts for the authenticated user
    // (as client or as lawyer)
    @GetMapping
    public ResponseEntity<ApiResponse<List<ContractSummaryResponse>>> getMyContracts(
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<List<ContractSummaryResponse>> body = ApiResponse.success(
                contractService.getMyContracts(userId)
        );
        return ResponseEntity.ok(body);
    }

    // ── GET /api/contracts/:id ────────────────────────────────
    // Both client and lawyer can view their shared contract
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContractDetailResponse>> getContractDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<ContractDetailResponse> body = ApiResponse.success(
                contractService.getContractDetail(id, userId)
        );
        return ResponseEntity.ok(body);
    }

    // ── POST /api/contracts ───────────────────────────────────
    // Client hires a lawyer by selecting a service
    @PostMapping
    public ResponseEntity<ApiResponse<ContractDetailResponse>> createContract(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateContractRequest request) {

        ApiResponse<ContractDetailResponse> body = ApiResponse.success(
                contractService.createContract(userId, request)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // ── PUT /api/contracts/:id/accept — lawyer accepts ────────
    // PENDING_LAWYER → PENDING_PAYMENT
    @PutMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<ContractDetailResponse>> acceptContract(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<ContractDetailResponse> body = ApiResponse.success(
                contractService.acceptContract(id, userId)
        );
        return ResponseEntity.ok(body);
    }

    // ── PUT /api/contracts/:id/decline — lawyer declines ──────
    // PENDING_LAWYER → CANCELLED
    @PutMapping("/{id}/decline")
    public ResponseEntity<ApiResponse<ContractDetailResponse>> declineContract(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<ContractDetailResponse> body = ApiResponse.success(
                contractService.declineContract(id, userId)
        );
        return ResponseEntity.ok(body);
    }

    // ── PUT /api/contracts/:id/complete — client confirms ─────
    // ACTIVE → COMPLETED + escrow RELEASED
    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<ContractDetailResponse>> completeContract(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<ContractDetailResponse> body = ApiResponse.success(
                contractService.completeContract(id, userId)
        );
        return ResponseEntity.ok(body);
    }

    // ── PUT /api/contracts/:id/cancel ─────────────────────────
    // Client → any non-terminal status
    // Lawyer → PENDING_LAWYER only
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ContractDetailResponse>> cancelContract(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<ContractDetailResponse> body = ApiResponse.success(
                contractService.cancelContract(id, userId)
        );
        return ResponseEntity.ok(body);
    }

    // ── PUT /api/contracts/:id/milestones/:mId ────────────────
    // Toggle milestone done/undone — ACTIVE contracts only
    @PutMapping("/{id}/milestones/{milestoneId}")
    public ResponseEntity<ApiResponse<ContractDetailResponse.MilestoneDto>> toggleMilestone(
            @PathVariable UUID id,
            @PathVariable UUID milestoneId,
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<ContractDetailResponse.MilestoneDto> body = ApiResponse.success(
                contractService.toggleMilestone(id, milestoneId, userId)
        );
        return ResponseEntity.ok(body);
    }
}