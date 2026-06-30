package com.legalaid.dispute;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "disputes")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ── Cross-package → plain UUID ───────────────────────────
    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "raised_by", nullable = false)
    private UUID raisedBy;

    // ── Dispute details ──────────────────────────────────────
    @Column(nullable = false, length = 200)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;          // amount in dispute

    @Column(name = "resolution_deadline")
    private Instant resolutionDeadline;

    // ── Status ───────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DisputeStatus status = DisputeStatus.AWAITING_RESPONSE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private DisputePriority priority = DisputePriority.MEDIUM;

    // ── Resolution ───────────────────────────────────────────
    // Admin who resolved the dispute
    @Column(name = "resolved_by")
    private UUID resolvedBy;

    // Admin who is mediating
    @Column(name = "mediator_id")
    private UUID mediatorId;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

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