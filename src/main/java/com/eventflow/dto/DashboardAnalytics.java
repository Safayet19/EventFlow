package com.eventflow.dto;

import java.math.BigDecimal;
import lombok.Builder;
import java.util.List;

/** Immutable statistics result. Spring EL reads record components in Thymeleaf. */
@Builder
public record DashboardAnalytics(
        long totalBookings,
        long completedEvents,
        long totalReviews,
        double averageRating,
        long couponUses,
        BigDecimal ticketValue,
        int capacityUsagePercent,
        List<String> trendLabels,
        List<Long> trendValues,
        List<String> categoryLabels,
        List<Long> categoryValues,
        List<String> topEventLabels,
        List<Long> topEventValues,
        List<Long> ratingValues
) {
    public DashboardAnalytics {
        ticketValue = ticketValue == null ? BigDecimal.ZERO : ticketValue;
    }

}
