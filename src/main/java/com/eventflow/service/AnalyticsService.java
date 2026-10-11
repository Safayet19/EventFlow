package com.eventflow.service;

import com.eventflow.dto.DashboardAnalytics;
import com.eventflow.model.Event;
import com.eventflow.model.Feedback;
import com.eventflow.model.Registration;
import com.eventflow.repository.FeedbackRepository;
import com.eventflow.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final RegistrationRepository registrationRepository;
    private final FeedbackRepository feedbackRepository;



    public DashboardAnalytics forEvents(List<Event> events) {
        List<Event> safeEvents = events == null ? List.of() : events;
        Set<String> eventIds = safeEvents.stream().map(Event::getId).collect(Collectors.toSet());
        List<Registration> registrations = eventIds.isEmpty()
                ? List.of()
                : registrationRepository.findByEventIdInOrderByRegisteredAtDesc(eventIds);
        List<Feedback> feedback = eventIds.isEmpty()
                ? List.of()
                : feedbackRepository.findByEventIdIn(eventIds);

        long completedEvents = safeEvents.stream().filter(event -> "COMPLETED".equals(EventRules.status(event))).count();
        long totalBookings = registrations.size();
        long totalReviews = feedback.size();
        double averageRating = totalReviews == 0 ? 0.0
                : Math.round(feedback.stream().mapToInt(Feedback::getRating).average().orElse(0.0) * 10.0) / 10.0;
        long couponUses = registrations.stream().filter(RegistrationRules::discounted).count();
        // Count only confirmed payment records; exclude old bookings without a payment status.
        BigDecimal ticketValue = registrations.stream()
                .filter(registration -> registration.getPaymentStatus() != null
                        && registration.getPaymentStatus().startsWith("PAID_"))
                .map(registration -> registration.getFinalPrice() == null ? BigDecimal.ZERO : registration.getFinalPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalCapacity = safeEvents.stream().mapToLong(Event::getCapacity).sum();
        int capacityUsage = totalCapacity == 0 ? 0 : (int) Math.min(100, Math.round(totalBookings * 100.0 / totalCapacity));

        List<String> trendLabels = new ArrayList<>();
        List<Long> trendValues = new ArrayList<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM yy", Locale.ENGLISH);
        YearMonth current = YearMonth.now();
        for (int offset = 5; offset >= 0; offset--) {
            YearMonth month = current.minusMonths(offset);
            trendLabels.add(month.format(monthFormatter));
            long count = registrations.stream()
                    .filter(registration -> registration.getRegisteredAt() != null
                            && YearMonth.from(registration.getRegisteredAt()).equals(month))
                    .count();
            trendValues.add(count);
        }

        Map<String, Long> categoryMap = safeEvents.stream()
                .collect(Collectors.groupingBy(event -> event.getCategory() == null ? "Other" : event.getCategory(),
                        LinkedHashMap::new, Collectors.counting()));
        List<Map.Entry<String, Long>> categories = categoryMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .toList();

        Map<String, Event> eventMap = safeEvents.stream().collect(Collectors.toMap(Event::getId, Function.identity(), (a, b) -> a));
        Map<String, Long> bookingsByEvent = registrations.stream()
                .collect(Collectors.groupingBy(Registration::getEventId, Collectors.counting()));
        List<Map.Entry<String, Long>> topEvents = bookingsByEvent.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .toList();

        List<Long> ratingValues = new ArrayList<>();
        for (int rating = 5; rating >= 1; rating--) {
            final int currentRating = rating;
            ratingValues.add(feedback.stream().filter(item -> item.getRating() == currentRating).count());
        }

        return DashboardAnalytics.builder()
                .totalBookings(totalBookings)
                .completedEvents(completedEvents)
                .totalReviews(totalReviews)
                .averageRating(averageRating)
                .couponUses(couponUses)
                .ticketValue(ticketValue)
                .capacityUsagePercent(capacityUsage)
                .trendLabels(trendLabels)
                .trendValues(trendValues)
                .categoryLabels(categories.stream().map(Map.Entry::getKey).toList())
                .categoryValues(categories.stream().map(Map.Entry::getValue).toList())
                .topEventLabels(topEvents.stream().map(entry -> {
                    Event event = eventMap.get(entry.getKey());
                    return event == null ? "Deleted event" : event.getTitle();
                }).toList())
                .topEventValues(topEvents.stream().map(Map.Entry::getValue).toList())
                .ratingValues(ratingValues)
                .build();
    }
}
