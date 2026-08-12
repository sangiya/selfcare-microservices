package com.selfcare.platform.common.adapter;

/**
 * Marker interface for a pluggable integration into an operator-specific core or third-party
 * system (Doc 1 sec 6.3) — e.g. "which loyalty core system does this operator use", "which
 * payment gateway", "which SMS provider". Each capability gets one stable Java interface
 * extending this (see {@code loyalty-service}'s {@code LoyaltyCoreAdapter} for a full example);
 * each operator gets one concrete implementation of that interface, selected at runtime by
 * tenant ID through {@link ApiAdapterRegistry}. Adding a new operator's integration is then
 * "write one new adapter class", not "branch the calling service's code".
 */
public interface ApiAdapter {

    /**
     * The tenant/operator this adapter instance serves. Use the constant
     * {@link #ALL_TENANTS} for an adapter that is shared/default across every operator that
     * doesn't register a more specific override.
     */
    String tenantId();

    String ALL_TENANTS = "*";
}
