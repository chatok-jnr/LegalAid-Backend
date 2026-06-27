package com.legalaid.lawyer;

import com.legalaid.common.response.ApiResponse;
import com.legalaid.lawyer.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lawyers")
@RequiredArgsConstructor
public class LawyerController {

    private final LawyerService lawyerService;

    // ── GET /api/lawyers ─────────────────────────────────────
    // Public — no auth needed
    // Query params: category, city, minRating, page, size, sort
    @GetMapping
    public ResponseEntity<ApiResponse<Page<LawyerPublicResponse>>> searchLawyers(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double minRating,
            @PageableDefault(size = 12) Pageable pageable
    ) {
        ApiResponse<Page<LawyerPublicResponse>> body = ApiResponse.success(
                lawyerService.searchLawyers(category, city, minRating, pageable)
        );

        return ResponseEntity.ok(body);
    }

    // ── GET /api/lawyers/me ──────────────────────────────────
    // Own full profile — lawyer only
    // NOTE: /me must be declared BEFORE /{id} so Spring
    // doesn't try to parse "me" as a UUID path variable
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('LAWYER')")
    public ResponseEntity<ApiResponse<LawyerProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UUID userId
    ) {
        ApiResponse<LawyerProfileResponse> body = ApiResponse.success(lawyerService.getMyProfile(userId));
        return ResponseEntity.ok(body);
    }

    // ── GET /api/lawyers/me/stats ────────────────────────────
    @GetMapping("/me/stats")
    @PreAuthorize("hasAuthority('LAWYER')")
    public ResponseEntity<ApiResponse<LawyerStatsResponse>> getMyStats(
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<LawyerStatsResponse> body = ApiResponse.success(
                lawyerService.getMyStats(userId)
        );
        return ResponseEntity.ok(body);
    }

    // ── GET /api/lawyers/:id ─────────────────────────────────
    // Public — no auth needed
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LawyerPublicResponse>> getPublicProfile(
            @PathVariable UUID id) {

        ApiResponse<LawyerPublicResponse> body = ApiResponse.success(
                lawyerService.getPublicProfile(id)
        );
        return ResponseEntity.ok(body);
    }

    // ── PUT /api/lawyers/me/onboarding ───────────────────────
    // Step 1 of verification flow.
    // CLIENT role allowed because role is still CLIENT during onboarding.
    // Can be re-submitted if rejected.
    @PutMapping("/me/onboarding")
    @PreAuthorize("hasAuthority('CLIENT') or hasAuthority('LAWYER')")
    public ResponseEntity<ApiResponse<LawyerProfileResponse>> completeOnboarding(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody OnboardingRequest request) {

        ApiResponse<LawyerProfileResponse> body = ApiResponse.success(
                lawyerService.completeOnboarding(userId, request)
        );
        return ResponseEntity.ok(body);
    }

    // ── POST /api/lawyers/me/verify ──────────────────────────
    // Step 2 of verification flow.
    // Upload bar cert + NID → sets verification_status = PENDING.
    // Admin then reviews in admin package.
    @PostMapping("/me/verify")
    @PreAuthorize("hasAuthority('CLIENT') or hasAuthority('LAWYER')")
    public ResponseEntity<ApiResponse<Void>> submitVerification(
            @AuthenticationPrincipal UUID userId,
            @RequestBody List<@Valid VerificationDocRequest> docs) {

        lawyerService.submitVerificationDocs(userId, docs);

        ApiResponse<Void> body = ApiResponse.success(null);
        return ResponseEntity.ok(body);
    }
}
