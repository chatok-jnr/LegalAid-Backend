package com.legalaid.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReplyReviewRequest {

    @NotBlank(message = "Reply text is required")
    @Size(max = 1000, message = "Reply too long")
    private String reply;
}