package com.legalaid.review;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_helpful_votes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewHelpfulVote {

    // ── Composite PK — one vote per user per review ──────────
    @EmbeddedId
    private ReviewHelpfulVoteId id;

    @Column(name = "voted_at", nullable = false)
    @Builder.Default
    private Instant votedAt = Instant.now();

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ReviewHelpfulVoteId implements java.io.Serializable {
        @Column(name = "review_id")
        private UUID reviewId;

        @Column(name = "user_id")
        private UUID userId;
    }
}