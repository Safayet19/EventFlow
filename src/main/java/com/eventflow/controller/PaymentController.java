package com.eventflow.controller;

import com.eventflow.model.PaymentAttempt;
import com.eventflow.repository.RegistrationRepository;
import com.eventflow.model.User;
import com.eventflow.service.EventService;
import com.eventflow.service.PaymentService;
import com.eventflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final EventService eventService;
    private final UserService userService;
    private final RegistrationRepository registrationRepository;

    @GetMapping("/participant/events/{eventId}/checkout")
    public String checkout(@PathVariable String eventId,
                           @RequestParam(required = false) String ticketTypeId,
                           @RequestParam(required = false, defaultValue = "") String couponCode,
                           Authentication authentication, Model model,
                           RedirectAttributes flash) {
        User user = userService.currentUser(authentication);
        try {
            var quote = paymentService.quote(eventId, user, ticketTypeId, couponCode);
            model.addAttribute("event", eventService.findById(eventId));
            model.addAttribute("quote", quote);
            model.addAttribute("couponInput", couponCode);
            return "payment-checkout";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("toastError", ex.getMessage());
            return "redirect:/events/" + eventId + "#tickets";
        }
    }

    @PostMapping("/participant/events/{eventId}/pay")
    public String start(@PathVariable String eventId, @RequestParam(required = false) String ticketTypeId,
                        @RequestParam(required = false) String couponCode,
                        @RequestParam(required = false) String paymentMethod,
                        Authentication authentication, RedirectAttributes flash) {
        try {
            User user = userService.currentUser(authentication);
            PaymentAttempt attempt = paymentService.start(
                    user, eventId, ticketTypeId, couponCode, paymentMethod);
            flash.addFlashAttribute("toastSuccess", "Registration confirmed.");
            return "redirect:/participant/payments/result?ref=" + attempt.getTransactionId();
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("toastError", ex.getMessage());
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("toastError", "Unable to complete registration. Please try again.");
        }
        return "redirect:/participant/events/" + eventId + "/checkout"
                + (ticketTypeId == null || ticketTypeId.isBlank() ? "" : "?ticketTypeId="
                + java.net.URLEncoder.encode(ticketTypeId, java.nio.charset.StandardCharsets.UTF_8));
    }

    @GetMapping("/participant/payments/result")
    public String result(@RequestParam(value = "ref", required = false) String transaction,
                         Authentication authentication, Model model, RedirectAttributes flash) {
        try {
            PaymentAttempt attempt = paymentService.forParticipant(transaction, userService.currentUser(authentication));
            model.addAttribute("attempt", attempt);
            registrationRepository.findByEventIdAndParticipantId(attempt.getEventId(), attempt.getParticipantId())
                    .filter(registration -> attempt.getTransactionId().equals(registration.getPaymentReference()))
                    .ifPresent(registration -> model.addAttribute("registrationId", registration.getId()));
            model.addAttribute("event", eventService.findOptional(attempt.getEventId()).orElse(null));
            return "payment-result";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("toastError", ex.getMessage());
            return "redirect:/participant/registrations";
        }
    }
}
