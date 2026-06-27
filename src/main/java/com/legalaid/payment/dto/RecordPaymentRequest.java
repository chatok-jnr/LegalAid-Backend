package com.legalaid.payment.dto;

import com.legalaid.payment.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RecordPaymentRequest {

    @NotNull(message = "Contract ID is required")
    private UUID contractId;

    @NotBlank(message = "Transaction ID is required")
    private String txnId;

    @NotBlank(message = "Sender number is required")
    private String senderNumber;

    // Defaults to BKASH if not provided
    private PaymentMethod method;
}