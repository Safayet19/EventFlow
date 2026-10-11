package com.eventflow.service;

import com.eventflow.model.Event;
import com.eventflow.model.Registration;
import com.eventflow.model.User;
import com.eventflow.repository.EventRepository;
import com.eventflow.repository.FeedbackRepository;
import com.eventflow.repository.RegistrationRepository;
import com.eventflow.repository.PaymentAttemptRepository;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.bson.Document;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final MongoTemplate mongoTemplate;
    private final EventRepository eventRepository;
    private final FeedbackRepository feedbackRepository;
    private final NotificationService notificationService;
    private final TicketTypeService ticketTypeService;
    private final CouponService couponService;



    @CacheEvict(value = "registrationCount", allEntries = true, beforeInvocation = true)
    public Registration register(String eventId, User participant) {
        return register(eventId, participant, null);
    }

    @CacheEvict(value = "registrationCount", allEntries = true, beforeInvocation = true)
    public Registration register(String eventId, User participant, String ticketTypeId) {
        return register(eventId, participant, ticketTypeId, null);
    }

    @CacheEvict(value = "registrationCount", allEntries = true, beforeInvocation = true)
    // Serialize capacity, ticket-allocation and coupon-limit checks for one application
    // instance. Multi-instance deployments require database-level atomic reservations.
    @Transactional
    public synchronized Registration register(String eventId, User participant, String ticketTypeId, String couponCode) {
        // Lock this event document inside the MongoDB transaction to serialize
        // concurrent registrations across replicas.
        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(eventId)),
                new Update().inc("bookingVersion", 1L), Event.class);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));

        if (isClosed(event)) {
            throw new IllegalArgumentException("Registration is closed for this event.");
        }
        if (registrationRepository.existsByEventIdAndParticipantId(eventId, participant.getId())) {
            throw new IllegalArgumentException("You are already registered for this event.");
        }
        if (registrationRepository.countByEventId(eventId) >= event.getCapacity()) {
            throw new IllegalArgumentException("This event is already full.");
        }

        TicketTypeService.TicketSelection ticket = ticketTypeService.resolveForRegistration(event, ticketTypeId);
        if (ticket.price() != null && ticket.price().compareTo(java.math.BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Paid tickets must be booked through checkout.");
        }
        CouponService.AppliedCoupon coupon = couponService.apply(event, ticket.price(), couponCode);

        try {
            Registration registration = registrationRepository.save(Registration.builder()
                    .eventId(eventId)
                    .participantId(participant.getId())
                    .participantName(participant.getFullName())
                    .participantEmail(participant.getEmail())
                    .ticketTypeId(ticket.ticketTypeId())
                    .ticketTypeName(ticket.ticketTypeName())
                    .ticketPrice(ticket.price() == null ? java.math.BigDecimal.ZERO : ticket.price())
                    .couponId(coupon.couponId())
                    .couponCode(coupon.couponCode())
                    .discountAmount(coupon.discountAmount() == null ? java.math.BigDecimal.ZERO : coupon.discountAmount())
                    .finalPrice(coupon.finalPrice() == null ? ticket.price() : coupon.finalPrice())
                    .paymentStatus("NOT_REQUIRED")
                    .build());

            notificationService.notifyUser(
                    event.getOrganizerId(),
                    participant.getFullName() + " registered for " + event.getTitle()
                            + " (" + ticket.ticketTypeName() + ").",
                    "/organizer/events/" + eventId + "/participants"
            );
            notificationService.notifyUser(
                    participant.getId(),
                    "Registration confirmed for " + event.getTitle() + ".",
                    "/participant/registrations#registration-" + eventId
            );
            if (coupon.couponCode() != null && !coupon.couponCode().isBlank()) {
                notificationService.notifyUser(
                        participant.getId(),
                        "Your " + coupon.couponCode() + " coupon discount was applied successfully.",
                        "/participant/registrations#registration-" + eventId
                );
            }
            return registration;
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("You are already registered for this event.");
        }
    }

    @CacheEvict(value = "registrationCount", allEntries = true, beforeInvocation = true)
    @Transactional
    public Event cancel(String eventId, User participant) {
        Registration registration = registrationRepository.findByEventIdAndParticipantId(eventId, participant.getId())
                .orElseThrow(() -> new IllegalArgumentException("Registration was not found."));

        Event event = eventRepository.findById(eventId).orElse(null);
        if (event != null && isClosed(event)) {
            throw new IllegalArgumentException("Completed event registrations are read-only and cannot be cancelled.");
        }
        if (RegistrationRules.paidTicket(registration) || EventRules.isPaid(event)) {
            throw new IllegalArgumentException("Paid event registrations cannot be cancelled.");
        }
        cancelDemoAttempt(registration);
        registrationRepository.delete(registration);
        feedbackRepository.deleteByEventIdAndParticipantId(eventId, participant.getId());

        if (event != null) {
            notificationService.notifyUser(
                    event.getOrganizerId(),
                    participant.getFullName() + " cancelled registration for " + event.getTitle() + ".",
                    "/organizer/events/" + eventId + "/participants"
            );
        }
        return event;
    }

    @CacheEvict(value = "registrationCount", allEntries = true, beforeInvocation = true)
    @Transactional
    public void removeByAdmin(String registrationId) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found."));
        Event event = eventRepository.findById(registration.getEventId()).orElse(null);
        if (event != null && isClosed(event)) {
            throw new IllegalArgumentException("Bookings for completed events are read-only and cannot be removed.");
        }
        cancelDemoAttempt(registration);
        registrationRepository.delete(registration);
        feedbackRepository.deleteByEventIdAndParticipantId(registration.getEventId(), registration.getParticipantId());

        notificationService.notifyUser(
                registration.getParticipantId(),
                "An administrator removed your registration"
                        + (event == null ? "." : " for " + event.getTitle() + "."),
                event == null ? "/participant/registrations" : "/events/" + event.getId()
        );
    }

    public List<Registration> findByParticipant(String participantId) {
        return registrationRepository.findByParticipantIdOrderByRegisteredAtDesc(participantId);
    }

    public List<Registration> findByEvent(String eventId) {
        return registrationRepository.findByEventIdOrderByRegisteredAtAsc(eventId);
    }

    public List<Registration> findAllNewestFirst() {
        return registrationRepository.findAllByOrderByRegisteredAtDesc();
    }

    public List<Registration> findForEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }
        Set<String> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toSet());
        return registrationRepository.findByEventIdInOrderByRegisteredAtDesc(eventIds);
    }

    @CacheEvict(value = "registrationCount", allEntries = true, beforeInvocation = true)
    @Transactional
    public void removeByOrganizer(String registrationId, User organizer) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found."));
        Event event = eventRepository.findById(registration.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));

        if (event.getOrganizerId() == null || !event.getOrganizerId().equals(organizer.getId())) {
            throw new IllegalArgumentException("You cannot manage this registration.");
        }
        if (isClosed(event)) {
            throw new IllegalArgumentException("Bookings for completed events are read-only and cannot be removed.");
        }

        cancelDemoAttempt(registration);
        registrationRepository.delete(registration);
        feedbackRepository.deleteByEventIdAndParticipantId(registration.getEventId(), registration.getParticipantId());
        notificationService.notifyUser(
                registration.getParticipantId(),
                "The organizer removed your registration for " + event.getTitle() + ".",
                "/events/" + event.getId()
        );
    }

    @Cacheable(value = "registrationCount", key = "#eventId")
    public long countByEvent(String eventId) {
        return registrationRepository.countByEventId(eventId);
    }

    /** One MongoDB aggregation replaces one count query for every event in a listing. */
    public Map<String, Long> countsForEvents(Collection<String> eventIds) {
        Map<String, Long> counts = new HashMap<>();
        if (eventIds == null || eventIds.isEmpty()) return counts;
        eventIds.stream().filter(id -> id != null && !id.isBlank()).forEach(id -> counts.put(id, 0L));
        if (counts.isEmpty()) return counts;
        var aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("eventId").in(counts.keySet())),
                Aggregation.group("eventId").count().as("total")
        );
        for (Document result : mongoTemplate.aggregate(aggregation, "registrations", Document.class).getMappedResults()) {
            Object eventId = result.get("_id");
            Object total = result.get("total");
            if (eventId instanceof String id && total instanceof Number amount && counts.containsKey(id)) {
                counts.put(id, amount.longValue());
            }
        }
        return counts;
    }

    public long count() {
        return registrationRepository.count();
    }

    public boolean isRegistered(String eventId, String participantId) {
        return registrationRepository.existsByEventIdAndParticipantId(eventId, participantId);
    }

    @CacheEvict(value = "registrationCount", allEntries = true, beforeInvocation = true)
    public void deleteAllByEvent(String eventId) {
        registrationRepository.findByEventIdOrderByRegisteredAtAsc(eventId).forEach(this::cancelDemoAttempt);
        registrationRepository.deleteAllByEventId(eventId);
        feedbackRepository.deleteAllByEventId(eventId);
    }

    private void cancelDemoAttempt(Registration registration) {
        if (!"PAID_DEMO".equals(registration.getPaymentStatus())
                || registration.getPaymentReference() == null) return;
        paymentAttemptRepository.findByTransactionId(registration.getPaymentReference()).ifPresent(attempt -> {
            attempt.setStatus("CANCELLED");
            paymentAttemptRepository.save(attempt);
        });
    }

    private boolean isClosed(Event event) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        return event.getDate() == null
                || "COMPLETED".equals(EventRules.status(event))
                || event.getDate().isBefore(today)
                || (event.getDate().isEqual(today)
                && event.getTime() != null
                && event.getTime().isBefore(now));
    }
}
