package com.eventflow.service;

import com.eventflow.model.Event;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Event lifecycle and pricing rules; no view formatting or database calls. */
public final class EventRules {
    private EventRules() {}

    public static String status(Event event) {
        if (event == null) return "UPCOMING";
        LocalDate date = event.getDate();
        if (date != null) {
            LocalDate today = LocalDate.now();
            if (date.isBefore(today)
                    || (date.isEqual(today) && event.getTime() != null
                    && event.getTime().isBefore(LocalTime.now().withSecond(0).withNano(0)))) {
                return "COMPLETED";
            }
        }
        String status = event.getStatus();
        return status == null || status.isBlank() ? "UPCOMING" : status;
    }

    public static String pricingType(Event event) {
        String raw = event == null ? null : event.getPricingType();
        return raw == null || raw.isBlank() ? "FREE" : raw;
    }

    public static boolean isPaid(Event event) {
        return "PAID".equalsIgnoreCase(pricingType(event));
    }

    public static BigDecimal basePrice(Event event) {
        return event == null || event.getBasePrice() == null
                ? BigDecimal.ZERO : event.getBasePrice();
    }
}
