package com.legalaid.lawyer;

import com.legalaid.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lawyer_profiles")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LawyerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ── Link to users table ──────────────────────────────────
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    // ── Profile fields ───────────────────────────────────────
    @Column(length = 150)
    private String title;                    // "Senior Advocate"

    @Column(name = "bar_id", unique = true, length = 100)
    private String barId;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "office_address", columnDefinition = "TEXT")
    private String officeAddress;

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "fee_min", precision = 10, scale = 2)
    private BigDecimal feeMin;

    @Column(name = "fee_max", precision = 10, scale = 2)
    private BigDecimal feeMax;

    // ── Verification ─────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    // ── Onboarding ───────────────────────────────────────────
    @Column(name = "onboarding_completed", nullable = false)
    private Boolean onboardingCompleted;

    // ── Rating (auto-updated by DB trigger) ──────────────────
    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount;

    // ── Timestamps ───────────────────────────────────────────
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
