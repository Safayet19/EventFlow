package com.eventflow.service;

import com.eventflow.dto.FeedbackSummary;
import com.eventflow.model.Event;
import com.eventflow.model.Feedback;
import com.eventflow.model.User;
import com.eventflow.repository.EventRepository;
import com.eventflow.repository.FeedbackRepository;
import com.eventflow.repository.RegistrationRepository;
import org.springframework.dao.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private static final int MAX_COMMENT_LENGTH = 500;

    private final FeedbackRepository feedbackRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationService notificationService;



    public Feedback saveOrUpdate(String eventId, User participant, int rating, String comment) {
        if (participant == null || !"PARTICIPANT".equals(participant.getRole())) {
            throw new IllegalArgumentException("Only participants can submit event feedback.");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));

        if (!registrationRepository.existsByEventIdAndParticipantId(eventId, participant.getId())) {
            throw new IllegalArgumentException("You must be registered for this event to leave feedback.");
        }
        if (!isCompleted(event)) {
            throw new IllegalArgumentException("Feedback will be available after the event is completed.");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Choose a rating from 1 to 5 stars.");
        }

        String cleanComment = comment == null ? "" : comment.trim();
        if (cleanComment.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Feedback comment must be 500 characters or fewer.");
        }

        Optional<Feedback> existing = feedbackRepository.findByEventIdAndParticipantId(eventId, participant.getId());
        boolean created = existing.isEmpty();
        Feedback feedback;

        if (existing.isPresent()) {
            feedback = existing.get();
            feedback.setParticipantName(participant.getFullName());
            feedback.setParticipantEmail(participant.getEmail());
            feedback.setRating(rating);
            feedback.setComment(cleanComment);
            feedback.setUpdatedAt(LocalDateTime.now());
        } else {
            LocalDateTime createdAt = LocalDateTime.now();
            feedback = Feedback.builder().eventId(eventId)
                    .participantId(participant.getId())
                    .participantName(participant.getFullName())
                    .participantEmail(participant.getEmail())
                    .rating(rating).comment(cleanComment)
                    .createdAt(createdAt).updatedAt(createdAt)
                    .build();
        }

        try {
            feedback = feedbackRepository.save(feedback);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("You already submitted feedback for this event. Refresh and edit your existing feedback.");
        }

        if (created && event.getOrganizerId() != null && !event.getOrganizerId().isBlank()) {
            notificationService.notifyUser(
                    event.getOrganizerId(),
                    participant.getFullName() + " rated " + event.getTitle() + " " + rating + "/5.",
                    "/organizer/feedback?eventId=" + eventId
                            + "&feedbackId=" + feedback.getId()
                            + "#feedback-" + feedback.getId()
            );
        }

        return feedback;
    }

    public Optional<Feedback> findByEventAndParticipant(String eventId, String participantId) {
        return feedbackRepository.findByEventIdAndParticipantId(eventId, participantId);
    }

    public List<Feedback> findByParticipant(String participantId) {
        return feedbackRepository.findByParticipantIdOrderByUpdatedAtDesc(participantId);
    }

    public List<Feedback> findAllNewestFirst() {
        return feedbackRepository.findAllByOrderByUpdatedAtDesc();
    }

    public List<Feedback> findForEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }
        Set<String> eventIds = events.stream().map(Event::getId).collect(Collectors.toSet());
        List<Feedback> feedback = new ArrayList<>(feedbackRepository.findByEventIdIn(eventIds));
        feedback.sort(Comparator.comparing(
                Feedback::getUpdatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return feedback;
    }

    public Map<String, FeedbackSummary> summariesForEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }

        Set<String> eventIds = events.stream().map(Event::getId).collect(Collectors.toSet());
        Map<String, List<Feedback>> grouped = feedbackRepository.findByEventIdIn(eventIds)
                .stream()
                .collect(Collectors.groupingBy(Feedback::getEventId));

        Map<String, FeedbackSummary> summaries = new HashMap<>();
        for (Event event : events) {
            summaries.put(event.getId(), summaryOf(grouped.getOrDefault(event.getId(), List.of())));
        }
        return summaries;
    }

    public FeedbackSummary summaryOf(List<Feedback> feedback) {
        Map<Integer, Long> counts = new LinkedHashMap<>();
        for (int rating = 5; rating >= 1; rating--) {
            counts.put(rating, 0L);
        }

        if (feedback == null || feedback.isEmpty()) {
            return new FeedbackSummary(0.0, 0L, 0L, counts);
        }

        long sum = 0L;
        for (Feedback item : feedback) {
            int rating = item.getRating();
            counts.put(rating, counts.getOrDefault(rating, 0L) + 1L);
            sum += rating;
        }

        long total = feedback.size();
        double average = Math.round((sum * 10.0 / total)) / 10.0;
        return new FeedbackSummary(average, total, counts.getOrDefault(5, 0L), counts);
    }

    public long countReviewedEvents(List<Feedback> feedback) {
        return feedback == null ? 0L : feedback.stream().map(Feedback::getEventId).distinct().count();
    }

    public void deleteByAdmin(String feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found."));
        feedbackRepository.delete(feedback);
    }

    private boolean isCompleted(Event event) {
        if (event.getDate() == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (event.getDate().isBefore(today)) {
            return true;
        }
        if (event.getDate().isAfter(today)) {
            return false;
        }
        LocalTime eventTime = event.getTime();
        return eventTime != null && eventTime.isBefore(LocalTime.now().withSecond(0).withNano(0));
    }
}
