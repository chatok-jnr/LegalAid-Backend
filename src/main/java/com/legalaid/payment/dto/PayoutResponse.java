package com.legalaid.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class PayoutResponse {

    private UUID        id;
    private UUID        lawyerId;
    private BigDecimal  amount;
    private String      mobileNumber;
    private String      bankAccount;
    private String      method;
    private String      status;
    private UUID        processedBy;
    private Instant     requestedAt;
    private Instant     processedAt;
}