package com.eventflow.dto;

/** Immutable form data; Spring MVC uses constructor binding. */
public record CouponForm(
        String eventId,
        String code,
        String discountType,
        String discountValue,
        String expiryDate,
        int usageLimit,
        boolean active
) {}
