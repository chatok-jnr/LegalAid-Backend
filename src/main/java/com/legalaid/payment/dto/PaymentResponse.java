package com.legalaid.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class PaymentResponse {

    private UUID        id;
    private UUID        contractId;
    private UUID        clientId;
    private BigDecimal  amount;
    private BigDecimal  platformFee;
    private String      method;
    private String      txnId;
    private String      senderNumber;
    private String      status;
    private UUID        verifiedBy;
    private Instant     verifiedAt;
    private Instant     releasedAt;
    private Instant     createdAt;
}