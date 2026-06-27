package com.legalaid.payment;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payouts")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ── Cross-package → plain UUID ───────────────────────────
    // References users.id (lawyer's user ID)
    @Column(name = "lawyer_id", nullable = false)
    private UUID lawyerId;

    // ── Payout details ───────────────────────────────────────
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // Destination — lawyer fills one of these
    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;

    @Column(name = "bank_account", length = 50)
    private String bankAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentMethod method = PaymentMethod.BKASH;

    // ── Status ───────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PayoutStatus status = PayoutStatus.REQUESTED;

    // Admin who processed this payout
    @Column(name = "processed_by")
    private UUID processedBy;

    @CreatedDate
    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}