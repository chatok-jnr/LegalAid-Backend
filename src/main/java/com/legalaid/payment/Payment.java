package com.legalaid.payment;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ── Cross-package → plain UUID ───────────────────────────
    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    // ── Amounts ──────────────────────────────────────────────
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "platform_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal platformFee = BigDecimal.ZERO;

    // ── Payment details ──────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentMethod method = PaymentMethod.BKASH;

    // bKash / Nagad transaction ID — submitted by client
    @Column(name = "txn_id", length = 255)
    private String txnId;

    // Mobile number used to send the payment
    @Column(name = "sender_number", length = 20)
    private String senderNumber;

    // ── Status ───────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING_VERIFICATION;

    // Admin who verified the TxnID
    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}