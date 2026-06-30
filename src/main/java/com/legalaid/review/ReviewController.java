package com.legalaid.review;

import com.legalaid.common.response.ApiResponse;
import com.legalaid.review.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // ── POST /api/reviews ─────────────────────────────────────
    // Client submits review after contract is COMPLETED
    // One review per contract — enforced in service + DB unique constraint
    @PostMapping("/api/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody SubmitReviewRequest request) {

        ApiResponse<ReviewResponse> body = ApiResponse.success(
                reviewService.submitReview(userId, request)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // ── GET /api/lawyers/:id/reviews ──────────────────────────
    // Public — paginated review list for a lawyer
    // Includes average rating and total count in response
    // Query params: page (default 0), size (default 10, max 20)
    @GetMapping("/api/lawyers/{lawyerId}/reviews")
    public ResponseEntity<ApiResponse<ReviewPageResponse>> getLawyerReviews(
            @PathVariable UUID lawyerId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<ReviewPageResponse> body = ApiResponse.success(
                reviewService.getLawyerReviews(lawyerId, page, size, userId)
        );
        return ResponseEntity.ok(body);
    }

    // ── PUT /api/reviews/:id/reply ────────────────────────────
    // Lawyer replies to a review on their profile
    // Can be called multiple times — each call updates the reply
    @PutMapping("/api/reviews/{id}/reply")
    public ResponseEntity<ApiResponse<ReviewResponse>> replyToReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ReplyReviewRequest request) {

        ApiResponse<ReviewResponse> body = ApiResponse.success(
                reviewService.replyToReview(id, userId, request)
        );
        return ResponseEntity.ok(body);
    }

    // ── POST /api/reviews/:id/helpful ────────────────────────
    // Toggle helpful vote — any authenticated user
    // If already voted → removes vote (toggle behaviour)
    @PostMapping("/api/reviews/{id}/helpful")
    public ResponseEntity<ApiResponse<ReviewResponse>> toggleHelpful(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<ReviewResponse> body = ApiResponse.success(
                reviewService.toggleHelpful(id, userId)
        );
        return ResponseEntity.ok(body);
    }
}