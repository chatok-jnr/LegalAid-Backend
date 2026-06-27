package com.legalaid.contract;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contracts")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ── Cross-package → plain UUID ───────────────────────────
    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "lawyer_id", nullable = false)
    private UUID lawyerId;               // references users.id

    // Optional — links contract to a case
    @Column(name = "case_id")
    private UUID caseId;

    // ── Snapshotted service data at creation time ─────────────
    // These are copied from the service when the contract is created.
    // If the lawyer later edits their service, existing contracts
    // are NOT affected.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;           // snapshot of service.price

    @Column(name = "delivery_days")
    private Integer deliveryDays;        // snapshot of service.deliveryDays

    @Column(name = "revisions")
    private Integer revisions;           // snapshot of service.revisions

    @Column(name = "platform_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal platformFee = BigDecimal.ZERO;  // 5% deducted on release

    // ── Status ───────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ContractStatus status = ContractStatus.PENDING_LAWYER;

    @Enumerated(EnumType.STRING)
    @Column(name = "escrow_status", nullable = false, length = 20)
    @Builder.Default
    private EscrowStatus escrowStatus = EscrowStatus.NONE;

    // ── Optional client note when hiring ────────────────────
    @Column(name = "client_note", columnDefinition = "TEXT")
    private String clientNote;

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