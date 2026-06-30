package com.legalaid.search.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class SearchResultResponse {

    // Unified result — type tells the frontend how to render
    // type = "LAWYER" or "SERVICE"
    private String  type;
    private UUID    id;

    // ── Common fields ────────────────────────────────────────
    private String  title;           // lawyer name or service title
    private String  avatarUrl;       // lawyer avatar or service thumbnail
    private String  category;
    private String  location;        // primary city

    // ── Lawyer-specific ──────────────────────────────────────
    private BigDecimal  lawyerRating;
    private Integer     lawyerReviewCount;
    private Integer     experienceYears;
    private Boolean     isVerified;
    private List<String> practiceAreas;

    // ── Service-specific ─────────────────────────────────────
    private BigDecimal  price;
    private Integer     deliveryDays;
    private Integer     revisions;
    private UUID        lawyerId;
    private String      lawyerName;
    private BigDecimal  serviceRating;  // from lawyer rating
}