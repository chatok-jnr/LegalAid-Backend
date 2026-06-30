package com.legalaid.dispute;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dispute_timeline")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeTimeline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ── Same-package → @ManyToOne ────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", nullable = false)
    private Dispute dispute;

    // ── Cross-package → plain UUID ───────────────────────────
    @Column(name = "actor_id")
    private UUID actorId;   // who triggered this event (nullable for system events)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisputeEventType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;    // human-readable description of the event

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}