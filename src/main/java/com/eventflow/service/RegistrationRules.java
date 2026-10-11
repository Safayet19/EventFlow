package com.eventflow.service;

import com.eventflow.model.Registration;
import java.math.BigDecimal;

/** Booking discount state, shared by analytics and presentation. */
public final class RegistrationRules {
    private RegistrationRules() {}

    /** A discounted-to-zero paid ticket is still a paid booking. */
    public static boolean paidTicket(Registration registration) {
        return registration != null && (
                (registration.getTicketPrice() != null
                        && registration.getTicketPrice().compareTo(BigDecimal.ZERO) > 0)
                || (registration.getFinalPrice() != null
                        && registration.getFinalPrice().compareTo(BigDecimal.ZERO) > 0)
                || (registration.getPaymentStatus() != null
                        && registration.getPaymentStatus().startsWith("PAID")));
    }

    public static boolean discounted(Registration registration) {
        return registration != null
                && registration.getCouponCode() != null
                && !registration.getCouponCode().isBlank()
                && registration.getDiscountAmount() != null
                && registration.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0;
    }
}
