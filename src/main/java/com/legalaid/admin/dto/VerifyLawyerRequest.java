package com.legalaid.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyLawyerRequest {
    @NotNull(message = "Decision is required")
    private Decision decision;

    // Required when decision = REJECT
    private String rejectionReason;

    // Required when decision = REJECT
    public enum Decision {
        APPROVE,
        REJECT
    }
}
