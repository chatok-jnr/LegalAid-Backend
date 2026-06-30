package com.legalaid.review;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "review_aspects",
        uniqueConstraints = @UniqueConstraint(columnNames = {"review_id", "aspect"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewAspect {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ── Same-package → @ManyToOne ────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    // e.g. "Communication", "Expertise", "Value", "Timeliness"
    @Column(nullable = false, length = 100)
    private String aspect;

    @Column(nullable = false)
    private Integer rating;   // 1-5
}