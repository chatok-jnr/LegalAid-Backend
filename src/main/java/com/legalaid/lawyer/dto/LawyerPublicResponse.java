package com.legalaid.lawyer.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class LawyerPublicResponse {

    private UUID        id;
    private String      name;       // from users table
    private String      avatarUrl;  // from users table
    private String      title;
    private Integer     experienceYears;
    private String      bio;
    private BigDecimal  feeMin;
    private BigDecimal  feeMax;
    private BigDecimal  rating;
    private Integer     reviewCount;
    private Boolean     isVerified;
    private Instant     verifiedAt;
    private List<String> practiceAreas;
    private List<String> courts;
    private List<String> languages;
    private List<String> availability;
    private List<String> locations;   // city names only for public view
}
