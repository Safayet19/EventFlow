package com.eventflow.dto;

import java.util.Map;

/** Immutable public feedback aggregate, not a mutable MongoDB entity. */
public record FeedbackSummary(
        double averageRating,
        long totalReviews,
        long fiveStarReviews,
        Map<Integer, Long> ratingCounts
) {
    public long countFor(int rating) {
        return ratingCounts.getOrDefault(rating, 0L);
    }
}
