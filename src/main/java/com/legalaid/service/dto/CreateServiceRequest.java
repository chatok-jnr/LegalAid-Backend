package com.legalaid.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class CreateServiceRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Delivery days is required")
    @Min(value = 1, message = "Delivery days must be at least 1")
    @Max(value = 365, message = "Delivery days cannot exceed 365")
    private Integer deliveryDays;

    @Min(value = 0, message = "Revisions cannot be negative")
    @Builder.Default
    private Integer revisions = 1;

    @Size(max = 5000, message = "Description too long")
    private String description;

    // Sub-items — all optional
    private List<@Valid MediaItem>     media;
    private List<@Valid TextItem>      highlights;
    private List<@Valid TextItem>      features;
    private List<@Valid FaqItem>       faqs;

    // ── Nested DTOs ──────────────────────────────────────────
    @Getter
    @Setter
    public static class MediaItem {
        @NotBlank(message = "Media URL is required")
        private String url;
        private String cloudinaryId;
        @NotNull(message = "Media type is required")
        private String type;      // IMAGE or VIDEO
        private Integer sortOrder;
    }

    @Getter
    @Setter
    public static class TextItem {
        @NotBlank(message = "Text is required")
        @Size(max = 300)
        private String text;
        private Integer sortOrder;
    }

    @Getter
    @Setter
    public static class FaqItem {
        @NotBlank(message = "Question is required")
        @Size(max = 300)
        private String question;
        @NotBlank(message = "Answer is required")
        private String answer;
        private Integer sortOrder;
    }
}
