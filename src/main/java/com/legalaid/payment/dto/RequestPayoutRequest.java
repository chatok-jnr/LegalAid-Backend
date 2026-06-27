package com.legalaid.payment.dto;

import com.legalaid.payment.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RequestPayoutRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum payout is 1 BDT")
    private BigDecimal amount;

    // One of these must be provided
    private String mobileNumber;
    private String bankAccount;

    @NotNull(message = "Payout method is required")
    private PaymentMethod method;
}