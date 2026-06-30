package com.legalaid.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewHelpfulVoteRepository
        extends JpaRepository<ReviewHelpfulVote, ReviewHelpfulVote.ReviewHelpfulVoteId> {

    boolean existsByIdReviewIdAndIdUserId(UUID reviewId, UUID userId);

    void deleteByIdReviewIdAndIdUserId(UUID reviewId, UUID userId);

    long countByIdReviewId(UUID reviewId);
}