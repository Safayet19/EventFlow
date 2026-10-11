package com.eventflow.controller;

import com.eventflow.model.Event;
import com.eventflow.model.Feedback;
import com.eventflow.model.Registration;
import com.eventflow.model.User;
import com.eventflow.service.EventService;
import com.eventflow.service.FeedbackService;
import com.eventflow.service.RegistrationService;
import com.eventflow.service.UserService;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ParticipantController {

    private final UserService userService;
    private final EventService eventService;
    private final RegistrationService registrationService;
    private final FeedbackService feedbackService;



    @PostMapping("/participant/events/{eventId}/register")
    public String register(@PathVariable String eventId,
                           @RequestParam(required = false) String ticketTypeId,
                           @RequestParam(required = false) String couponCode,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        User participant = userService.currentUser(authentication);
        try {
            Event event = eventService.findById(eventId);
            registrationService.register(eventId, participant, ticketTypeId, couponCode);
            redirectAttributes.addFlashAttribute(
                    "toastSuccess",
                    "Successfully registered for " + event.getTitle() + "."
            );
            return "redirect:/participant/registrations";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
            return "redirect:/events/" + eventId + "#tickets";
        }
    }

    @PostMapping("/participant/events/{eventId}/cancel")
    public String cancel(@PathVariable String eventId,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        User participant = userService.currentUser(authentication);
        try {
            registrationService.cancel(eventId, participant);
            redirectAttributes.addFlashAttribute("toastSuccess", "Registration cancelled successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return "redirect:/participant/registrations";
    }

    @GetMapping("/participant/registrations")
    public String myRegistrations(Authentication authentication, Model model) {
        User participant = userService.currentUser(authentication);
        List<Registration> registrations = registrationService.findByParticipant(participant.getId());
        Map<String, Event> eventMap = new HashMap<>();

        for (Registration registration : registrations) {
            eventService.findOptional(registration.getEventId())
                    .ifPresent(event -> eventMap.put(registration.getEventId(), event));
        }

        Map<String, Feedback> feedbackMap = new HashMap<>();
        for (Feedback feedback : feedbackService.findByParticipant(participant.getId())) {
            feedbackMap.put(feedback.getEventId(), feedback);
        }

        model.addAttribute("registrations", registrations);
        model.addAttribute("eventMap", eventMap);
        model.addAttribute("feedbackMap", feedbackMap);
        return "my-registrations";
    }
}
