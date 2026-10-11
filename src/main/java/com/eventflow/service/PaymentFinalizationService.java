package com.eventflow.service;

import com.eventflow.dto.PaymentQuote;
import com.eventflow.model.Event;
import com.eventflow.model.PaymentAttempt;
import com.eventflow.model.Registration;
import com.eventflow.model.User;
import com.eventflow.repository.CouponRepository;
import com.eventflow.repository.EventRepository;
import com.eventflow.repository.PaymentAttemptRepository;
import com.eventflow.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Confirms simulated paid registrations and stores their payment records atomically. */
@Service
@RequiredArgsConstructor
public class PaymentFinalizationService {
    private final MongoTemplate mongoTemplate;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final CouponRepository couponRepository;
    private final TicketTypeService ticketTypeService;
    private final CouponService couponService;
    private final NotificationService notificationService;

    /** Writes to the event document force concurrent orders to conflict, protecting capacity in Atlas transactions. */
    private Event lockEvent(String eventId) {
        var result = mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(eventId)),
                new Update().inc("bookingVersion", 1L), Event.class);
        if (result.getMatchedCount() == 0) throw new IllegalArgumentException("Event not found.");
        return eventRepository.findById(eventId).orElseThrow(() -> new IllegalArgumentException("Event not found."));
    }

    /** Confirms a demo booking atomically without an external payment provider. */
    @Transactional
    @CacheEvict(value = "registrationCount", allEntries = true)
    public PaymentAttempt completeDemo(User participant, PaymentQuote quote, String method) {
        Event event = lockEvent(quote.eventId());
        if (participant == null || !"PARTICIPANT".equals(participant.getRole()))
            throw new IllegalArgumentException("Only participants can book tickets.");
        if ("COMPLETED".equals(EventRules.status(event)))
            throw new IllegalArgumentException("Registration for this event is closed.");
        if (registrationRepository.existsByEventIdAndParticipantId(event.getId(), participant.getId()))
            throw new IllegalArgumentException("You are already registered for this event.");
        if (registrationRepository.countByEventId(event.getId()) >= event.getCapacity())
            throw new IllegalArgumentException("This event is full.");

        var ticket = ticketTypeService.resolveForRegistration(event, quote.ticketTypeId());
        if (ticket.price() == null || ticket.price().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("This ticket is free. Use Register instead.");
        if (ticket.ticketTypeId() != null
                && registrationRepository.countByTicketTypeId(ticket.ticketTypeId()) >= ticket.capacity())
            throw new IllegalArgumentException("This ticket type is sold out.");
        var coupon = couponService.apply(event, ticket.price(), quote.couponCode());
        if (coupon.couponId() != null) {
            var currentCoupon = couponRepository.findById(coupon.couponId())
                    .orElseThrow(() -> new IllegalArgumentException("Coupon no longer exists."));
            if (currentCoupon.getUsageLimit() > 0
                    && registrationRepository.countByCouponId(coupon.couponId()) >= currentCoupon.getUsageLimit())
                throw new IllegalArgumentException("This coupon has reached its usage limit.");
        }

        LocalDateTime now = LocalDateTime.now();
        String reference = "EF" + UUID.randomUUID().toString().replace("-", "").substring(0, 25).toUpperCase();
        registrationRepository.save(Registration.builder()
                .eventId(event.getId()).participantId(participant.getId())
                .participantName(participant.getFullName()).participantEmail(participant.getEmail())
                .ticketTypeId(ticket.ticketTypeId()).ticketTypeName(ticket.ticketTypeName())
                .ticketPrice(ticket.price()).couponId(coupon.couponId()).couponCode(coupon.couponCode())
                .discountAmount(coupon.discountAmount()).finalPrice(coupon.finalPrice())
                .paymentStatus("PAID_DEMO").paymentMethod(method).paymentReference(reference)
                .build());
        PaymentAttempt attempt = paymentAttemptRepository.save(PaymentAttempt.builder()
                .transactionId(reference).eventId(event.getId()).participantId(participant.getId())
                .participantName(participant.getFullName()).participantEmail(participant.getEmail())
                .ticketTypeId(ticket.ticketTypeId()).ticketTypeName(ticket.ticketTypeName())
                .ticketPrice(ticket.price()).couponId(coupon.couponId()).couponCode(coupon.couponCode())
                .discountAmount(coupon.discountAmount()).payableAmount(coupon.finalPrice())
                .preferredMethod(method).status("PAID").createdAt(now).completedAt(now)
                .build());
        notificationService.notifyUser(participant.getId(),
                "Registration confirmed for " + event.getTitle() + ".",
                "/participant/registrations#registration-" + event.getId());
        notificationService.notifyUser(event.getOrganizerId(),
                participant.getFullName() + " registered for " + event.getTitle() + ".",
                "/organizer/events/" + event.getId() + "/participants");
        return attempt;
    }
}
