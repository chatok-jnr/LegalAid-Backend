package com.legalaid.notification;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ── Cross-package → plain UUID ───────────────────────────
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // ── Content ──────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    // ── Read state ───────────────────────────────────────────
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    // ── Navigation ───────────────────────────────────────────
    // Frontend uses action_url to redirect when notification clicked
    @Column(name = "action_url", columnDefinition = "TEXT")
    private String actionUrl;

    // Structured reference — easier to query than parsing action_url
    @Column(name = "reference_id")
    private UUID referenceId;

    // CONTRACT, CASE, DISPUTE, REVIEW, PAYMENT
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    // ── Soft delete ──────────────────────────────────────────
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}