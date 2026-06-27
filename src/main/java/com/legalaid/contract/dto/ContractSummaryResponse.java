package com.legalaid.contract.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ContractSummaryResponse {

    private UUID        id;
    private UUID        serviceId;
    private UUID        clientId;
    private UUID        lawyerId;
    private UUID        caseId;

    // Snapshotted values
    private String      serviceTitle;     // fetched from service
    private BigDecimal  amount;
    private Integer     deliveryDays;
    private Integer     revisions;

    // Status
    private String      status;
    private String      escrowStatus;

    private Instant     createdAt;
    private Instant     updatedAt;
}