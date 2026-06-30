package com.legalaid.review.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class SubmitReviewRequest {

    @NotNull(message = "Contract ID is required")
    private UUID contractId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;

    @Size(max = 2000, message = "Comment too long")
    private String comment;

    // Optional per-aspect ratings
    // e.g. [{ aspect: "Communication", rating: 5 }]
    private List<AspectRating> aspects;

    // Optional tags e.g. ["Fast response", "Expert knowledge"]
    private List<String> tags;

    @Getter
    @Setter
    public static class AspectRating {
        @NotBlank(message = "Aspect name is required")
        @Size(max = 100)
        private String aspect;

        @NotNull
        @Min(1) @Max(5)
        private Integer rating;
    }
}