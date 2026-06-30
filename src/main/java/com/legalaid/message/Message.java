package com.legalaid.message;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ── Cross-package → plain UUID ───────────────────────────
    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    // ── Message content ──────────────────────────────────────
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    // ── Optional file attachment ─────────────────────────────
    @Column(name = "attachment_url", columnDefinition = "TEXT")
    private String attachmentUrl;

    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    // ── Read status ──────────────────────────────────────────
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}