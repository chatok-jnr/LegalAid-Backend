package com.legalaid.notification;

public enum NotificationType {

    // ── Contract events ──────────────────────────────────────
    CONTRACT_REQUEST,       // lawyer receives new hire request
    CONTRACT_ACCEPTED,      // client notified lawyer accepted
    CONTRACT_DECLINED,      // client notified lawyer declined
    CONTRACT_COMPLETED,     // lawyer notified client marked complete
    CONTRACT_CANCELLED,     // both parties notified of cancellation

    // ── Payment events ───────────────────────────────────────
    PAYMENT_RECEIVED,       // lawyer notified payment verified + contract active
    PAYMENT_RELEASED,       // lawyer notified payment released to them
    PAYMENT_REFUNDED,       // client notified payment was refunded
    PAYMENT_REJECTED,       // client notified TxnID was rejected

    // ── Dispute events ───────────────────────────────────────
    DISPUTE_RAISED,         // both parties notified dispute was raised
    DISPUTE_RESOLVED,       // both parties notified of resolution

    // ── Review events ────────────────────────────────────────
    REVIEW_RECEIVED,        // lawyer notified of new review
    REVIEW_REPLIED,         // client notified lawyer replied

    // ── Verification events ──────────────────────────────────
    VERIFICATION_APPROVED,  // lawyer notified they are now verified
    VERIFICATION_REJECTED,  // lawyer notified docs were rejected

    // ── Message events ───────────────────────────────────────
    MESSAGE_RECEIVED,       // new message in contract thread

    // ── Case events ──────────────────────────────────────────
    CASE_INVITE,            // user invited to a case

    // ── Milestone events ─────────────────────────────────────
    MILESTONE_DONE,         // milestone marked complete

    // ── Generic ──────────────────────────────────────────────
    GENERAL
}