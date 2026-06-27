package com.legalaid.service.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ServiceDetailResponse {

    private UUID        id;
    private UUID        lawyerId;

    // Lawyer info
    private String      lawyerName;
    private String      lawyerAvatarUrl;
    private String      lawyerTitle;
    private BigDecimal  lawyerRating;
    private Integer     lawyerReviewCount;
    private Boolean     lawyerVerified;
    private Integer     lawyerExperienceYears;

    // Service info
    private String      title;
    private String      category;
    private BigDecimal  price;
    private Integer     deliveryDays;
    private Integer     revisions;
    private String      description;
    private Boolean     isActive;
    private Instant     createdAt;

    // Sub-items
    private List<MediaDto>     media;
    private List<String>       highlights;
    private List<String>       features;
    private List<FaqDto>       faqs;

    @Getter
    @Builder
    public static class MediaDto {
        private String  url;
        private String  cloudinaryId;
        private String  type;        // IMAGE or VIDEO
        private Integer sortOrder;
    }

    @Getter
    @Builder
    public static class FaqDto {
        private String  question;
        private String  answer;
        private Integer sortOrder;
    }
}