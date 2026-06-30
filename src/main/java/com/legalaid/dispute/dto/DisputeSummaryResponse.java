package com.legalaid.dispute.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class DisputeSummaryResponse {

    private UUID        id;
    private UUID        contractId;
    private UUID        raisedBy;
    private String      reason;
    private BigDecimal  amount;
    private String      status;
    private String      priority;
    private Instant     resolutionDeadline;
    private Instant     createdAt;
    private Instant     updatedAt;
}