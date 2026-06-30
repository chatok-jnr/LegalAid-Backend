package com.legalaid.dispute.dto;

import com.legalaid.dispute.DisputePriority;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class RaiseDisputeRequest {

    @NotNull(message = "Contract ID is required")
    private UUID contractId;

    @NotBlank(message = "Reason is required")
    @Size(max = 200, message = "Reason too long")
    private String reason;

    @Size(max = 3000, message = "Description too long")
    private String description;

    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;          // amount in dispute — optional

    private DisputePriority priority;   // defaults to MEDIUM

    private Instant resolutionDeadline; // optional — admin can override
}