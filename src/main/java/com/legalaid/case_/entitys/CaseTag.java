package com.legalaid.case_.entitys;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "case_tags", uniqueConstraints = @UniqueConstraint(columnNames = {"case_id", "tag"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseTag {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ── Same-package → @ManyToOne ────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private LegalCase legalCase;

    @Column(nullable = false, length = 80)
    private String tag;
}
