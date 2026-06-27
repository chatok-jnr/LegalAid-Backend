package com.legalaid.service.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class ServiceSummaryResponse {

    private UUID        id;
    private UUID        lawyerId;

    // Lawyer info — joined from lawyer_profiles + users
    private String      lawyerName;
    private String      lawyerAvatarUrl;
    private BigDecimal  lawyerRating;
    private Boolean     lawyerVerified;

    // Service info
    private String      title;
    private String      category;
    private BigDecimal  price;
    private Integer     deliveryDays;
    private Integer     revisions;

    // First image only — for cards
    private String      thumbnailUrl;
}