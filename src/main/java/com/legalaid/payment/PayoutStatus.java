package com.legalaid.payment;

public enum PayoutStatus {
    REQUESTED,      // lawyer requested withdrawal
    PROCESSING,     // admin is processing
    COMPLETED,      // money sent to lawyer
    REJECTED        // admin rejected the request
}