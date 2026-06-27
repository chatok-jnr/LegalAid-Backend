package com.legalaid.payment;

public enum PaymentStatus {
    PENDING_VERIFICATION,   // client submitted TxnID — awaiting admin check
    HELD,                   // verified — money in escrow
    RELEASED,               // released to lawyer on completion
    FROZEN,                 // dispute raised — money locked
    REFUNDED,               // cancelled — money returned to client
    FAILED                  // verification failed — invalid TxnID
}