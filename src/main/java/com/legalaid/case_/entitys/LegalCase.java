package com.legalaid.case_.entitys;

import com.legalaid.case_.enums.CasePriority;
import com.legalaid.case_.enums.CaseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cases")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class LegalCase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    // ── Cross-package → plain UUID ───────────────────────────
    // The user who owns/created the case
    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    // Assigned lawyer — optional, set when a contract is accepted
    @Column(name = "lawyer_id")
    private UUID lawyerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 100)
    private String category;

    @Column(length = 150)
    private String court;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CaseStatus status = CaseStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private CasePriority priority = CasePriority.MEDIUM;

    @Column(name = "next_hearing_date")
    private LocalDate nextHearingDate;

    @Column(name = "filed_date")
    private LocalDate filedDate;

    // ── Soft delete ──────────────────────────────────────────
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
