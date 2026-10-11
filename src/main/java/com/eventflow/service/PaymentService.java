package com.eventflow.service;

import com.eventflow.dto.PaymentQuote;
import com.eventflow.model.Event;
import com.eventflow.model.PaymentAttempt;
import com.eventflow.model.User;
import com.eventflow.repository.PaymentAttemptRepository;
import com.eventflow.repository.RegistrationRepository;
import com.eventflow.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final TicketTypeService ticketTypeService;
    private final CouponService couponService;
    private final PaymentFinalizationService finalization;

    public PaymentQuote quote(String eventId, User participant, String ticketTypeId, String couponCode) {
        if (participant == null || !"PARTICIPANT".equals(participant.getRole()))
            throw new IllegalArgumentException("Sign in as a participant to book tickets.");
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));
        if ("COMPLETED".equals(EventRules.status(event)))
            throw new IllegalArgumentException("Registration is closed for this event.");
        if (registrationRepository.existsByEventIdAndParticipantId(eventId, participant.getId()))
            throw new IllegalArgumentException("You are already registered for this event.");
        var ticket = ticketTypeService.resolveForRegistration(event, ticketTypeId);
        if (ticket.price() == null || ticket.price().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("This ticket is free. Use Register instead.");
        var coupon = couponService.apply(event, ticket.price(), couponCode);
        return new PaymentQuote(eventId, ticket.ticketTypeId(), ticket.ticketTypeName(),
                ticket.price(), coupon.couponId(), coupon.couponCode(),
                coupon.discountAmount(), coupon.finalPrice());
    }

    /** Records the selected demo payment method and confirms the registration. */
    public PaymentAttempt start(User participant, String eventId, String ticketTypeId,
                                String couponCode, String selectedMethod) {
        String method = selectedMethod == null ? "" : selectedMethod.toUpperCase(Locale.ROOT);
        if (!(method.equals("BKASH") || method.equals("NAGAD") || method.equals("VISA")))
            throw new IllegalArgumentException("Choose a payment method to continue.");
        PaymentQuote quote = quote(eventId, participant, ticketTypeId, couponCode);
        return finalization.completeDemo(participant, quote, method);
    }

    public PaymentAttempt require(String reference) {
        if (reference == null || !reference.matches("EF[A-F0-9]{25}"))
            throw new IllegalArgumentException("Invalid payment reference.");
        return paymentAttemptRepository.findByTransactionId(reference)
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found."));
    }

    public PaymentAttempt forParticipant(String reference, User participant) {
        PaymentAttempt attempt = require(reference);
        if (participant == null || !participant.getId().equals(attempt.getParticipantId()))
            throw new IllegalArgumentException("You cannot view this payment.");
        return attempt;
    }
}
