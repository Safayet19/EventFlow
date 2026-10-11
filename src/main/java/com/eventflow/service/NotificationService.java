package com.eventflow.service;

import com.eventflow.model.Event;
import com.eventflow.model.Notification;
import com.eventflow.model.Registration;
import com.eventflow.repository.EventRepository;
import com.eventflow.repository.NotificationRepository;
import com.eventflow.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;



    @CacheEvict(value = "unreadNotificationCounts", key = "#userId")
    public void notifyUser(String userId, String message, String link) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        notificationRepository.save(Notification.builder()
                .userId(userId).message(message).link(link).build());
    }

    public List<Notification> findAllForUser(String userId) {
        return notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> findRecentForUser(String userId) {
        return notificationRepository.findTop8ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Cacheable(value = "unreadNotificationCounts", key = "#userId")
    public long countUnread(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @CacheEvict(value = "unreadNotificationCounts", key = "#userId")
    public String markReadAndGetLink(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null || !userId.equals(notification.getUserId())) {
            return null;
        }

        String resolvedLink = resolveSpecificLink(notification, userId);
        boolean changed = !notification.isRead() || !Objects.equals(notification.getLink(), resolvedLink);
        notification.setRead(true);
        if (!Objects.equals(notification.getLink(), resolvedLink)) {
            notification.setLink(resolvedLink);
        }
        if (changed) {
            notificationRepository.save(notification);
        }
        return resolvedLink;
    }

    private String resolveSpecificLink(Notification notification, String userId) {
        String link = notification.getLink();
        if (link == null || link.isBlank()) {
            return "/notifications";
        }

        String message = notification.getMessage() == null ? "" : notification.getMessage();
        Event mentionedEvent = ("/participant/registrations".equals(link)
                || link.startsWith("/organizer/feedback")
                || "/admin/events".equals(link)) ? findMentionedEvent(message) : null;

        if ("/participant/registrations".equals(link)) {
            if (mentionedEvent != null) {
                String lowerMessage = message.toLowerCase(Locale.ROOT);
                if (lowerMessage.contains("rate") || lowerMessage.contains("completed")) {
                    return "/events/" + mentionedEvent.getId() + "#participant-feedback";
                }
                if (lowerMessage.contains("updated")) {
                    return "/events/" + mentionedEvent.getId();
                }
                if (registrationRepository.existsByEventIdAndParticipantId(mentionedEvent.getId(), userId)) {
                    return "/participant/registrations#registration-" + mentionedEvent.getId();
                }
                return "/events/" + mentionedEvent.getId();
            }

            Registration couponRegistration = findRegistrationForCouponMessage(userId, message);
            if (couponRegistration != null) {
                return "/participant/registrations#registration-" + couponRegistration.getEventId();
            }
        }

        if (link.startsWith("/organizer/feedback") && mentionedEvent != null && !link.contains("eventId=")) {
            return "/organizer/feedback?eventId=" + mentionedEvent.getId()
                    + "#review-event-" + mentionedEvent.getId();
        }

        if ("/admin/events".equals(link) && mentionedEvent != null) {
            return "/events/" + mentionedEvent.getId();
        }

        return link;
    }

    private Event findMentionedEvent(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String lowerMessage = message.toLowerCase(Locale.ROOT);
        return eventRepository.findAllByOrderByDateAsc().stream()
                .filter(event -> event.getTitle() != null && !event.getTitle().isBlank())
                .sorted(Comparator.comparingInt((Event event) -> event.getTitle().length()).reversed())
                .filter(event -> lowerMessage.contains(event.getTitle().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }

    private Registration findRegistrationForCouponMessage(String userId, String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String lowerMessage = message.toLowerCase(Locale.ROOT);
        if (!lowerMessage.contains("coupon")) {
            return null;
        }
        return registrationRepository.findByParticipantIdOrderByRegisteredAtDesc(userId).stream()
                .filter(registration -> registration.getCouponCode() != null && !registration.getCouponCode().isBlank())
                .filter(registration -> lowerMessage.contains(registration.getCouponCode().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }

    @CacheEvict(value = "unreadNotificationCounts", key = "#userId")
    public void markAllRead(String userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(userId);
        if (unread.isEmpty()) {
            return;
        }
        unread.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @CacheEvict(value = "unreadNotificationCounts", key = "#userId")
    public void deleteAllForUser(String userId) {
        notificationRepository.deleteAllByUserId(userId);
    }
}
