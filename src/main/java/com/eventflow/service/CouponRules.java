package com.eventflow.service;

import com.eventflow.model.Coupon;
import java.time.LocalDate;

/** Coupon eligibility independent of persistence and Thymeleaf. */
public final class CouponRules {
    private CouponRules() {}

    public static boolean expired(Coupon coupon) {
        return coupon != null && coupon.getExpiryDate() != null
                && coupon.getExpiryDate().isBefore(LocalDate.now());
    }
}
