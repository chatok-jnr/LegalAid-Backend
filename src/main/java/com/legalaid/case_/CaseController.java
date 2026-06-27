package com.legalaid.case_;

import com.legalaid.case_.dto.*;
import com.legalaid.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    // ── GET /api/cases ────────────────────────────────────────
    // Returns all cases visible to the authenticated user
    // (owned + assigned + invited)
    @GetMapping
    public ResponseEntity<ApiResponse<List<CaseSummaryResponse>>> getMyCases(
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<List<CaseSummaryResponse>> body = ApiResponse.success(
                caseService.getMyCases(userId)
        );
        return ResponseEntity.ok(body);
    }

    // ── GET /api/cases/:id ────────────────────────────────────
    // Any user with access can view full detail
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CaseResponse>> getCaseDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<CaseResponse> body = ApiResponse.success(
                caseService.getCaseDetail(id, userId)
        );
        return ResponseEntity.ok(body);
    }

    // ── POST /api/cases ───────────────────────────────────────
    // CLIENT or lawyer with access can create
    @PostMapping
    public ResponseEntity<ApiResponse<CaseResponse>> createCase(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateCaseRequest request) {

        ApiResponse<CaseResponse> body = ApiResponse.success(
                caseService.createCase(userId, request)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // ── PUT /api/cases/:id ────────────────────────────────────
    // Owner or EDITOR access required — partial update
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CaseResponse>> updateCase(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateCaseRequest request) {

        ApiResponse<CaseResponse> body = ApiResponse.success(
                caseService.updateCase(id, userId, request)
        );
        return ResponseEntity.ok(body);
    }

    // ── DELETE /api/cases/:id ─────────────────────────────────
    // Owner only — soft delete
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCase(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        caseService.deleteCase(id, userId);

        ApiResponse<Void> body = ApiResponse.success(null);
        return ResponseEntity.ok(body);
    }

    // ── POST /api/cases/:id/access ────────────────────────────
    // Owner only — invite a user to the case
    @PostMapping("/{id}/access")
    public ResponseEntity<ApiResponse<Void>> grantAccess(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody GrantAccessRequest request) {

        caseService.grantAccess(id, userId, request);

        ApiResponse<Void> body = ApiResponse.success(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // ── DELETE /api/cases/:id/access/:userId ──────────────────
    // Owner only — remove a user's access
    @DeleteMapping("/{id}/access/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> revokeAccess(
            @PathVariable UUID id,
            @PathVariable UUID targetUserId,
            @AuthenticationPrincipal UUID userId) {

        caseService.revokeAccess(id, userId, targetUserId);

        ApiResponse<Void> body = ApiResponse.success(null);
        return ResponseEntity.ok(body);
    }

    // ── PUT /api/cases/:id/milestones/:mId ───────────────────
    // Owner or EDITOR — toggle milestone done/undone
    @PutMapping("/{id}/milestones/{milestoneId}")
    public ResponseEntity<ApiResponse<CaseResponse.MilestoneDto>> toggleMilestone(
            @PathVariable UUID id,
            @PathVariable UUID milestoneId,
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<CaseResponse.MilestoneDto> body = ApiResponse.success(
                caseService.toggleMilestone(id, milestoneId, userId)
        );
        return ResponseEntity.ok(body);
    }
}