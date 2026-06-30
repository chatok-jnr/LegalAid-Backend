package com.legalaid.review;

import com.legalaid.contract.Contract;
import com.legalaid.contract.ContractRepository;
import com.legalaid.contract.ContractStatus;
import com.legalaid.notification.NotificationService;
import com.legalaid.review.dto.*;
import com.legalaid.user.User;
import com.legalaid.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository            reviewRepository;
    private final ReviewAspectRepository      aspectRepository;
    private final ReviewTagRepository         tagRepository;
    private final ReviewHelpfulVoteRepository helpfulVoteRepository;
    private final ContractRepository          contractRepository;
    private final UserRepository              userRepository;
    private final NotificationService         notificationService;

    // ── POST /api/reviews — submit review ────────────────────
    // Rules:
    // 1. Only CLIENT can submit
    // 2. Contract must be COMPLETED
    // 3. One review per contract
    @Transactional
    public ReviewResponse submitReview(UUID clientId,
                                       SubmitReviewRequest request) {
        // Validate contract
        Contract contract = contractRepository
                .findByIdAndDeletedAtIsNull(request.getContractId())
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        // Only the client of this contract can review
        if (!contract.getClientId().equals(clientId)) {
            throw new RuntimeException(
                    "Only the client of this contract can submit a review");
        }

        // Contract must be completed
        if (contract.getStatus() != ContractStatus.COMPLETED) {
            throw new RuntimeException(
                    "Reviews can only be submitted after the contract is completed");
        }

        // One review per contract
        if (reviewRepository.existsByContractIdAndDeletedAtIsNull(
                request.getContractId())) {
            throw new RuntimeException(
                    "You have already reviewed this contract");
        }

        // Save review
        Review review = Review.builder()
                .contractId(request.getContractId())
                .clientId(clientId)
                .lawyerId(contract.getLawyerId())
                .rating(request.getRating())
                .comment(request.getComment())
                .helpfulCount(0)
                .build();

        review = reviewRepository.save(review);

        // Save aspects
        if (request.getAspects() != null) {
            for (SubmitReviewRequest.AspectRating ar : request.getAspects()) {
                aspectRepository.save(ReviewAspect.builder()
                        .review(review)
                        .aspect(ar.getAspect())
                        .rating(ar.getRating())
                        .build());
            }
        }

        // Save tags
        if (request.getTags() != null) {
            for (String tag : request.getTags()) {
                tagRepository.save(ReviewTag.builder()
                        .review(review)
                        .tag(tag)
                        .build());
            }
        }

        // Notify lawyer — @Async so it never blocks the response
        String clientName = userRepository.getUserNameById(clientId)
                        .orElse("A Client");
        notificationService.notifyReviewReceived(
                contract.getLawyerId(),
                review.getId(),
                clientName,
                request.getRating()
        );

        return toResponse(review, clientId);
    }

    // ── GET /api/lawyers/:id/reviews — public review list ────
    // Paginated, newest first
    // lawyerUserId = users.id of the lawyer
    public ReviewPageResponse getLawyerReviews(UUID lawyerUserId,
                                               int page,
                                               int size,
                                               UUID requesterId) {
        int cappedSize = Math.min(size, 20);
        Pageable pageable = PageRequest.of(page, cappedSize);

        Page<Review> reviewPage = reviewRepository
                .findAllByLawyerIdAndDeletedAtIsNull(lawyerUserId, pageable);

        double avgRating = reviewRepository
                .averageRatingByLawyerId(lawyerUserId);
        long totalReviews = reviewRepository
                .countByLawyerIdAndDeletedAtIsNull(lawyerUserId);

        List<ReviewResponse> responses = reviewPage.getContent()
                .stream()
                .map(r -> toResponse(r, requesterId))
                .toList();

        return ReviewPageResponse.builder()
                .reviews(responses)
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .page(page)
                .size(cappedSize)
                .averageRating(avgRating)
                .totalReviews(totalReviews)
                .build();
    }

    // ── PUT /api/reviews/:id/reply — lawyer replies ───────────
    // Lawyer can reply once — subsequent calls update the reply
    @Transactional
    public ReviewResponse replyToReview(UUID reviewId,
                                        UUID lawyerUserId,
                                        ReplyReviewRequest request) {
        Review review = reviewRepository
                .findByIdAndLawyerIdAndDeletedAtIsNull(reviewId, lawyerUserId)
                .orElseThrow(() -> new RuntimeException(
                        "Review not found or you are not the lawyer"));

        review.setReply(request.getReply());
        review = reviewRepository.save(review);

        // Notify client of reply — @Async
        String lawyerName = userRepository.getUserNameById(lawyerUserId)
                .orElse("The lawyer");
        notificationService.notifyReviewReplied(
                review.getClientId(),
                review.getId(),
                lawyerName
        );

        return toResponse(review, lawyerUserId);
    }

    // ── POST /api/reviews/:id/helpful — toggle helpful vote ──
    // Any authenticated user can vote helpful
    // Toggling — if already voted, removes the vote
    @Transactional
    public ReviewResponse toggleHelpful(UUID reviewId, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        boolean alreadyVoted = helpfulVoteRepository
                .existsByIdReviewIdAndIdUserId(reviewId, userId);

        if (alreadyVoted) {
            // Remove vote
            helpfulVoteRepository.deleteByIdReviewIdAndIdUserId(reviewId, userId);
        } else {
            // Add vote
            ReviewHelpfulVote vote = ReviewHelpfulVote.builder()
                    .id(new ReviewHelpfulVote.ReviewHelpfulVoteId(reviewId, userId))
                    .build();
            helpfulVoteRepository.save(vote);
        }

        // Re-fetch to get updated helpful_count (updated by DB trigger)
        review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        return toResponse(review, userId);
    }

    // ── Mapper ───────────────────────────────────────────────
    private ReviewResponse toResponse(Review review, UUID requesterId) {
        User client = userRepository
                .findActiveById(review.getClientId()).orElse(null);

        List<ReviewResponse.AspectDto> aspects = aspectRepository
                .findAllByReview_Id(review.getId())
                .stream()
                .map(a -> ReviewResponse.AspectDto.builder()
                        .aspect(a.getAspect())
                        .rating(a.getRating())
                        .build())
                .toList();

        List<String> tags = tagRepository
                .findAllByReview_Id(review.getId())
                .stream()
                .map(ReviewTag::getTag)
                .toList();

        // Check if requester voted helpful
        boolean votedHelpful = requesterId != null
                && helpfulVoteRepository.existsByIdReviewIdAndIdUserId(
                review.getId(), requesterId);

        return ReviewResponse.builder()
                .id(review.getId())
                .contractId(review.getContractId())
                .clientId(review.getClientId())
                .clientName(client != null ? client.getName() : null)
                .clientAvatarUrl(client != null ? client.getAvatarUrl() : null)
                .lawyerId(review.getLawyerId())
                .rating(review.getRating())
                .comment(review.getComment())
                .reply(review.getReply())
                .helpfulCount(review.getHelpfulCount())
                .votedHelpful(votedHelpful)
                .aspects(aspects)
                .tags(tags)
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}