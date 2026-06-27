package com.legalaid.contract;

public enum ContractStatus {
    PENDING_LAWYER,     // waiting for lawyer to accept or decline
    PENDING_PAYMENT,    // lawyer accepted — waiting for client to pay
    ACTIVE,             // payment received — work in progress
    COMPLETED,          // client confirmed work done
    DISPUTED,           // client raised a dispute
    CANCELLED,          // cancelled by either party (rules enforced in service)
    UNDER_REVIEW        // admin is reviewing (optional intermediate state)
}