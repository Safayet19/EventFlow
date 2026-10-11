package com.eventflow.controller;

import com.eventflow.dto.FeedbackSummary;
import com.eventflow.model.Event;
import com.eventflow.model.Feedback;
import com.eventflow.model.User;
import com.eventflow.service.EventService;
import com.eventflow.service.FeedbackService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class FeedbackController {

    private final UserService userService;
    private final EventService eventService;
    private final FeedbackService feedbackService;



    @PostMapping("/participant/events/{eventId}/feedback")
    public String saveParticipantFeedback(@PathVariable String eventId,
                                          @RequestParam int rating,
                                          @RequestParam(required = false, defaultValue = "") String comment,
                                          @RequestParam(required = false) String returnTo,
                                          Authentication authentication,
                                          RedirectAttributes redirectAttributes) {
        User participant = userService.currentUser(authentication);
        boolean editing = feedbackService.findByEventAndParticipant(eventId, participant.getId()).isPresent();
        try {
            feedbackService.saveOrUpdate(eventId, participant, rating, comment);
            redirectAttributes.addFlashAttribute(
                    "toastSuccess",
                    editing ? "Feedback updated successfully." : "Thank you. Your feedback was submitted successfully."
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        String redirect = returnTo != null && returnTo.startsWith("/") && !returnTo.startsWith("//")
                ? returnTo
                : "/participant/registrations";
        return "redirect:" + redirect;
    }

    @GetMapping("/organizer/feedback")
    public String organizerFeedback(Authentication authentication,
                                    @RequestParam(required = false) String eventId,
                                    @RequestParam(required = false) String feedbackId,
                                    Model model) {
        User organizer = userService.currentUser(authentication);
        List<Event> events = eventService.findCompletedByOrganizer(organizer.getId());
        List<Feedback> feedback = feedbackService.findForEvents(events);
        FeedbackSummary overall = feedbackService.summaryOf(feedback);

        Map<String, List<Feedback>> feedbackByEvent = groupFeedbackByEvent(events, feedback);
        List<Event> ratedEvents = events.stream()
                .filter(event -> feedbackByEvent.containsKey(event.getId()))
                .toList();

        String selectedEventId = events.stream()
                .anyMatch(event -> eventId != null && eventId.equals(event.getId())) ? eventId : "";
        String selectedFeedbackId = "";
        if (feedbackId != null && !feedbackId.isBlank()) {
            Feedback selectedFeedback = feedback.stream()
                    .filter(item -> feedbackId.equals(item.getId()))
                    .findFirst()
                    .orElse(null);
            if (selectedFeedback != null
                    && (selectedEventId.isBlank() || selectedFeedback.getEventId().equals(selectedEventId))) {
                selectedFeedbackId = selectedFeedback.getId();
                if (selectedEventId.isBlank()) {
                    selectedEventId = selectedFeedback.getEventId();
                }
            }
        }

        model.addAttribute("events", events);
        model.addAttribute("ratedEvents", ratedEvents);
        model.addAttribute("feedback", feedback);
        model.addAttribute("feedbackByEvent", feedbackByEvent);
        model.addAttribute("eventMap", toEventMap(events));
        model.addAttribute("feedbackSummaries", feedbackService.summariesForEvents(events));
        model.addAttribute("overallFeedback", overall);
        model.addAttribute("reviewedEventCount", feedbackService.countReviewedEvents(feedback));
        model.addAttribute("selectedEventId", selectedEventId);
        model.addAttribute("selectedFeedbackId", selectedFeedbackId);
        return "organizer-feedback";
    }

    @GetMapping("/admin/feedback")
    public String adminFeedback(Model model) {
        List<Event> events = eventService.findAllSorted();
        List<Feedback> feedback = feedbackService.findAllNewestFirst();
        FeedbackSummary overall = feedbackService.summaryOf(feedback);

        Map<String, List<Feedback>> feedbackByEvent = groupFeedbackByEvent(events, feedback);
        List<Event> ratedEvents = events.stream()
                .filter(event -> feedbackByEvent.containsKey(event.getId()))
                .toList();

        model.addAttribute("events", events);
        model.addAttribute("ratedEvents", ratedEvents);
        model.addAttribute("feedback", feedback);
        model.addAttribute("feedbackByEvent", feedbackByEvent);
        model.addAttribute("eventMap", toEventMap(events));
        model.addAttribute("feedbackSummaries", feedbackService.summariesForEvents(events));
        model.addAttribute("overallFeedback", overall);
        model.addAttribute("reviewedEventCount", feedbackService.countReviewedEvents(feedback));
        return "admin-feedback";
    }

    @PostMapping("/admin/feedback/{id}/delete")
    public String deleteFeedback(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            feedbackService.deleteByAdmin(id);
            redirectAttributes.addFlashAttribute("toastSuccess", "Feedback removed successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return "redirect:/admin/feedback";
    }

    private Map<String, List<Feedback>> groupFeedbackByEvent(List<Event> events, List<Feedback> feedback) {
        Map<String, List<Feedback>> grouped = new LinkedHashMap<>();
        for (Event event : events) {
            List<Feedback> eventFeedback = feedback.stream()
                    .filter(item -> event.getId().equals(item.getEventId()))
                    .toList();
            if (!eventFeedback.isEmpty()) {
                grouped.put(event.getId(), eventFeedback);
            }
        }
        return grouped;
    }

    private Map<String, Event> toEventMap(List<Event> events) {
        Map<String, Event> eventMap = new HashMap<>();
        for (Event event : events) {
            eventMap.put(event.getId(), event);
        }
        return eventMap;
    }
}
