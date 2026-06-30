package com.legalaid.dispute;

public enum DisputeStatus {
    AWAITING_RESPONSE,  // raised — other party hasn't responded yet
    OPEN,               // both parties engaged — admin reviewing
    UNDER_REVIEW,       // admin is actively investigating
    RESOLVED,           // admin issued a decision
    CLOSED              // dispute closed (no further action)
}