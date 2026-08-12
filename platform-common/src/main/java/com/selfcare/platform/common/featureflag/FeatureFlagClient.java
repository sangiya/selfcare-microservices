package com.selfcare.platform.common.featureflag;

/**
 * Thin wrapper around the feature-flag provider (Unleash — Doc 2 sec 3.9) so business code
 * depends on this interface, not the Unleash SDK directly. Flags are always evaluated for the
 * current tenant, so "on for Operator A, off for Operator B" (Doc 1 sec 6.2) is the default
 * behavior, not an opt-in.
 */
public interface FeatureFlagClient {

    /**
     * @param flagName   the Unleash toggle name, e.g. "loyalty-flysmiles-transfer"
     * @param defaultValue returned if the flag service is unreachable — fail closed (false) for
     *                     anything risk-sensitive, fail open (true) for cosmetic/UX toggles
     */
    boolean isEnabled(String flagName, boolean defaultValue);
}
