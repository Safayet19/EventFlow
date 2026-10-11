package com.eventflow.dto;

import java.math.BigDecimal;

/** Immutable, server-calculated checkout price. Never trust the browser's total. */
public record PaymentQuote(
        String eventId, String ticketTypeId, String ticketTypeName,
        BigDecimal originalPrice, String couponId, String couponCode,
        BigDecimal discountAmount, BigDecimal total
) { }
