package com.legalaid.review;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ── Cross-package → plain UUID ───────────────────────────
    // One review per contract — enforced by unique constraint
    @Column(name = "contract_id", nullable = false, unique = true)
    private UUID contractId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    // References users.id (lawyer's user ID)
    @Column(name = "lawyer_id", nullable = false)
    private UUID lawyerId;

    // ── Review content ───────────────────────────────────────
    @Column(nullable = false)
    private Integer rating;              // 1-5

    @Column(columnDefinition = "TEXT")
    private String comment;

    // Lawyer's reply — set after submission
    @Column(columnDefinition = "TEXT")
    private String reply;

    @Column(name = "helpful_count", nullable = false)
    @Builder.Default
    private Integer helpfulCount = 0;   // auto-updated by DB trigger

    // ── Soft delete ──────────────────────────────────────────
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}