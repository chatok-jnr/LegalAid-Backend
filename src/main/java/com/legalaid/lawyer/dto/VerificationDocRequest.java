package com.legalaid.lawyer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerificationDocRequest {

    @NotBlank(message = "Document URL is required")
    private String docUrl;

    private String cloudinaryId;

    @NotNull(message = "Document type is required")
    private String docType;  // BAR_CERT, NID, OTHER
}