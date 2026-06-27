package com.legalaid.case_.entitys;

import com.legalaid.case_.enums.CaseAccessRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "case_access",
        uniqueConstraints = @UniqueConstraint(columnNames = {"case_id", "user_id"})
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CaseAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    // ── Same-package → @ManyToOne ────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private LegalCase legalCase;

    // ── Cross-package → plain UUID ───────────────────────────
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Who sent this invite - audit trail
    @Column(name = "invite_by")
    private UUID invitedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private CaseAccessRole role = CaseAccessRole.VIEWER;

    @Column(name = "granted_at", nullable = false)
    @Builder.Default
    private Instant grantedAt = Instant.now();
}
