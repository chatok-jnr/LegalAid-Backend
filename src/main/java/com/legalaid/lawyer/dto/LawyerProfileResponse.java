package com.legalaid.lawyer.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class LawyerProfileResponse {

    private UUID        id;
    private UUID        userId;
    private String      title;
    private String      barId;
    private Integer     experienceYears;
    private String      bio;
    private String      officeAddress;
    private BigDecimal  feeMin;
    private BigDecimal  feeMax;
    private String      verificationStatus;
    private Boolean     onboardingCompleted;
    private BigDecimal  rating;
    private Integer     reviewCount;
    private Instant     verifiedAt;
    private List<String> practiceAreas;
    private List<String> courts;
    private List<String> languages;
    private List<String> availability;
    private List<LocationDto> locations;

    @Getter
    @Builder
    public static class LocationDto {
        private String  city;
        private String  division;
        private Boolean isPrimary;
    }
}