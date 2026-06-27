package com.legalaid.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class InvoiceResponse {

    // Invoice metadata
    private UUID        invoiceNumber;   // payment ID used as invoice number
    private Instant     issuedAt;

    // Client info
    private UUID        clientId;
    private String      clientName;
    private String      clientEmail;

    // Lawyer info
    private UUID        lawyerId;
    private String      lawyerName;

    // Service info
    private UUID        contractId;
    private String      serviceTitle;

    // Amounts
    private BigDecimal  subtotal;
    private BigDecimal  platformFee;
    private BigDecimal  total;

    // Payment info
    private String      paymentMethod;
    private String      txnId;
    private String      paymentStatus;
}