package com.selfcare.loyalty.domain;

/**
 * Mirrors the legacy StarPointsController's transfer 'type' switch (mobile / flysmiles / amex).
 * MOBILE goes straight through the loyalty core adapter; the two partner channels are gated
 * behind a feature flag per operator (not every operator has a FlySmiles/Amex partnership) and
 * routed through an async partner-redemption request instead of a direct core-system call.
 */
public enum TransferChannel {
    MOBILE,
    FLYSMILES,
    AMEX
}
