package com.selfcare.loyalty.domain;

/** One entry per legacy StarPointsController action being ported (Doc 1 sec 2.3 audit). */
public enum LoyaltyActionType {
    GET_BALANCE,
    REGISTER,
    TRANSFER,
    DONATE,
    GET_ACTIVITY,
    PARTNER_REDEMPTION_REQUEST
}
