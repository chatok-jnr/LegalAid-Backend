package com.legalaid.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveDisputeRequest {

    // REFUND_CLIENT or RELEASE_TO_LAWYER
    @NotNull(message = "Decision is required")
    private DisputeDecision decision;

    @NotBlank(message = "Resolution note is required")
    @Size(max = 3000, message = "Resolution note too long")
    private String resolutionNote;

    public enum DisputeDecision {
        REFUND_CLIENT,       // cancel contract + refund payment to client
        RELEASE_TO_LAWYER    // complete contract + release payment to lawyer
    }
}