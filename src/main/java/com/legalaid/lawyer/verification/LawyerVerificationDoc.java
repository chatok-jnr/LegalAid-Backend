package com.legalaid.lawyer.verification;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lawyer_verification_docs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LawyerVerificationDoc {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ── Sub-package → parent package → plain UUID ────────────
    // Using @ManyToOne here would create a circular dependency
    // between verification/ and the parent lawyer/ package
    @Column(name = "lawyer_id", nullable = false)
    private UUID lawyerId;

    @Column(name = "doc_url", nullable = false, columnDefinition = "TEXT")
    private String docUrl;

    @Column(name = "cloudinary_id", length = 255)
    private String cloudinaryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 20)
    private DocType docType;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    public enum DocType {
        BAR_CERT,
        NID,
        OTHER
    }
}
