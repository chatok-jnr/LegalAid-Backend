package com.legalaid.contract;

public enum EscrowStatus {
    NONE,               // no payment yet
    HELD,               // payment received and held in escrow
    RELEASED,           // payment released to lawyer (on completion)
    FROZEN,             // payment frozen (dispute raised)
    REFUNDED            // payment refunded to client (cancellation)
}