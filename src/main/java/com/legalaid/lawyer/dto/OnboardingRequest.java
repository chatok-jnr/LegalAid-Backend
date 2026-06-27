package com.legalaid.lawyer.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class OnboardingRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @NotBlank(message = "Bar ID is required")
    @Size(max = 100, message = "Bar ID must not exceed 100 characters")
    private String barId;

    @NotNull(message = "Experience years is required")
    @Min(value = 0, message = "Experience cannot be negative")
    @Max(value = 60, message = "Experience years seems too high")
    private Integer experienceYears;

    @NotBlank(message = "Bio is required")
    @Size(min = 50, max = 2000, message = "Bio must be between 50 and 2000 characters")
    private String bio;

    @Size(max = 500, message = "Office address too long")
    private String officeAddress;

    @DecimalMin(value = "0.0", message = "Fee cannot be negative")
    private BigDecimal feeMin;

    @DecimalMin(value = "0.0", message = "Fee cannot be negative")
    private BigDecimal feeMax;

    @NotEmpty(message = "At least one practice area is required")
    private List<String> practiceAreas;

    private List<String> courts;
    private List<String> languages;

    // Valid values: WEEKDAYS, WEEKENDS, EVENINGS, EMERGENCY
    private List<String> availability;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String division;
}