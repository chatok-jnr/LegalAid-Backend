package com.legalaid.lawyer;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
    name = "lawyer_availability",
    uniqueConstraints = @UniqueConstraint(columnNames = {"lawyer_id", "slot"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LawyerAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ── Same-package → @ManyToOne ────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lawyer_id", nullable = false)
    private LawyerProfile lawyer;

    // WEEKDAYS, WEEKENDS, EVENINGS, EMERGENCY
    @Column(nullable = false, length = 50)
    private String slot;
}
