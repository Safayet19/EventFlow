package com.eventflow.controller;

import com.eventflow.model.Event;
import com.eventflow.dto.HomeEventCard;
import com.eventflow.view.ViewFormatter;
import com.eventflow.model.Registration;
import com.eventflow.model.EventSpeaker;
import com.eventflow.model.User;
import com.eventflow.service.EventService;
import com.eventflow.service.EventProgramService;
import com.eventflow.service.FeedbackService;
import com.eventflow.service.RegistrationService;
import com.eventflow.service.UserService;
import com.eventflow.service.TicketTypeService;
import com.eventflow.service.EventGalleryService;
import com.eventflow.service.AnalyticsService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final UserService userService;
    private final EventService eventService;
    private final RegistrationService registrationService;
    private final FeedbackService feedbackService;
    private final EventProgramService eventProgramService;
    private final TicketTypeService ticketTypeService;
    private final EventGalleryService eventGalleryService;
    private final AnalyticsService analyticsService;
    private final ViewFormatter display;



    @GetMapping({"/", "/home"})
    public String home(Authentication authentication, Model model) {
        User user = currentUserOrNull(authentication);
        List<Event> events = eventService.findAllSorted();
        List<Event> upcomingEvents = events.stream()
                .filter(eventService::isUpcoming)
                .limit(6)
                .toList();

        model.addAttribute("user", user);
        model.addAttribute("isGuest", user == null);
        model.addAttribute("featuredEvents", upcomingEvents.stream()
                .map(event -> HomeEventCard.from(event, display)).toList());
        model.addAttribute("registrationCounts", eventService.registrationCounts(upcomingEvents));
        model.addAttribute("startingPrices", startingPriceLabels(upcomingEvents));
        return "home";
    }

    @GetMapping("/events")
    public String events(Authentication authentication, Model model) {
        User user = currentUserOrNull(authentication);
        List<Event> events = eventService.findAllSorted();
        // Event discovery is shared by every role; organizer-owned events stay visible.
        model.addAttribute("user", user);
        model.addAttribute("isGuest", user == null);
        model.addAttribute("events", events);
        model.addAttribute("initialEventCount", user == null
                ? events.stream().filter(eventService::isUpcoming).count()
                : events.size());
        model.addAttribute("categories", events.stream().map(Event::getCategory).distinct().sorted().toList());
        model.addAttribute("registrationCounts", eventService.registrationCounts(events));
        model.addAttribute("feedbackSummaries", feedbackService.summariesForEvents(events));
        Map<String, List<com.eventflow.model.EventTicketType>> ticketTypes = ticketTypeService.byEvents(events);
        model.addAttribute("ticketTypesByEvent", ticketTypes);
        model.addAttribute("startingPrices", startingPriceLabels(events, ticketTypes));

        if (user != null && "PARTICIPANT".equals(user.getRole())) {
            Set<String> registeredEventIds = new HashSet<>();
            registrationService.findByParticipant(user.getId())
                    .forEach(registration -> registeredEventIds.add(registration.getEventId()));
            model.addAttribute("registeredEventIds", registeredEventIds);
        }
        return "events";
    }


    @GetMapping("/events/{id}")
    public String eventDetails(@PathVariable String id, Authentication authentication, Model model) {
        User user = currentUserOrNull(authentication);
        Event event = eventService.findOptional(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Event not found"));
        var schedule = eventProgramService.findSchedule(id);
        List<EventSpeaker> speakers = eventProgramService.findSpeakers(id);
        var sponsors = eventProgramService.findSponsors(id);
        var allTicketTypes = ticketTypeService.findByEvent(id);
        var ticketTypes = allTicketTypes.stream().filter(com.eventflow.model.EventTicketType::isActive).toList();
        var feedback = feedbackService.findForEvents(List.of(event));

        model.addAttribute("user", user);
        model.addAttribute("isGuest", user == null);
        model.addAttribute("event", event);
        model.addAttribute("schedule", schedule);
        model.addAttribute("speakers", speakers);
        model.addAttribute("sponsors", sponsors);
        model.addAttribute("ticketTypes", ticketTypes);
        model.addAttribute("hasCustomTicketTypes", !allTicketTypes.isEmpty());
        model.addAttribute("ticketRegistrationCounts", ticketTypeService.registrationCounts(id));
        model.addAttribute("startingPrice", ticketTypeService.startingPriceLabel(event, allTicketTypes));
        model.addAttribute("speakerMap", eventProgramService.speakerMap(speakers));
        model.addAttribute("registrationCount", registrationService.countByEvent(id));
        model.addAttribute("feedbackSummary", feedbackService.summaryOf(feedback));
        model.addAttribute("feedback", feedback.stream().limit(6).toList());
        model.addAttribute("gallery", eventGalleryService.findByEvent(id));
        boolean isRegistered = user != null && "PARTICIPANT".equals(user.getRole())
                && registrationService.isRegistered(id, user.getId());
        model.addAttribute("isRegistered", isRegistered);
        model.addAttribute("myFeedback", isRegistered
                ? feedbackService.findByEventAndParticipant(id, user.getId()).orElse(null)
                : null);
        return "event-details";
    }

    private Map<String, String> startingPriceLabels(List<Event> events) {
        return startingPriceLabels(events, ticketTypeService.byEvents(events));
    }

    private Map<String, String> startingPriceLabels(List<Event> events,
            Map<String, List<com.eventflow.model.EventTicketType>> tiers) {
        Map<String, String> labels = new HashMap<>();
        for (Event event : events) {
            labels.put(event.getId(), ticketTypeService.startingPriceLabel(event, tiers.get(event.getId())));
        }
        return labels;
    }

    private User currentUserOrNull(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.currentUser(authentication);
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    @GetMapping("/dashboard")
    public String dashboardRedirect(Authentication authentication) {
        User user = userService.currentUser(authentication);
        return switch (user.getRole()) {
            case "ADMIN" -> "redirect:/admin/dashboard";
            case "ORGANIZER" -> "redirect:/organizer/dashboard";
            default -> "redirect:/participant/dashboard";
        };
    }

    @GetMapping("/participant/dashboard")
    public String participantDashboard(Authentication authentication, Model model) {
        User user = userService.currentUser(authentication);
        List<Event> events = eventService.findAllSorted();
        List<Registration> registrations = registrationService.findByParticipant(user.getId());
        Set<String> ids = registrations.stream()
                .map(Registration::getEventId)
                .collect(java.util.stream.Collectors.toSet());
        List<Event> registeredEvents = events.stream().filter(event -> ids.contains(event.getId())).toList();

        model.addAttribute("user", user);
        model.addAttribute("metricOneLabel", "Available Events");
        model.addAttribute("metricOneValue", events.stream().filter(eventService::isUpcoming).count());
        model.addAttribute("metricTwoLabel", "My Registrations");
        model.addAttribute("metricTwoValue", registrations.size());
        model.addAttribute("metricThreeLabel", "Upcoming Registered");
        model.addAttribute("metricThreeValue", registeredEvents.stream().filter(eventService::isUpcoming).count());
        model.addAttribute("metricFourLabel", "Completed");
        model.addAttribute("metricFourValue", registeredEvents.stream().filter(eventService::isCompleted).count());
        List<Event> dashboardEvents = registeredEvents.stream().filter(eventService::isUpcoming).limit(5).toList();
        model.addAttribute("dashboardEvents", dashboardEvents);
        model.addAttribute("registrationCounts", eventService.registrationCounts(dashboardEvents));
        return "dashboard";
    }

    @GetMapping("/organizer/dashboard")
    public String organizerDashboard(Authentication authentication, Model model) {
        User user = userService.currentUser(authentication);
        List<Event> events = eventService.findByOrganizer(user.getId());
        long totalRegistrations = registrationService.countsForEvents(
                events.stream().map(Event::getId).toList()).values().stream()
                .mapToLong(Long::longValue).sum();

        model.addAttribute("user", user);
        model.addAttribute("metricOneLabel", "My Events");
        model.addAttribute("metricOneValue", events.size());
        model.addAttribute("metricTwoLabel", "Upcoming Events");
        model.addAttribute("metricTwoValue", events.stream().filter(eventService::isUpcoming).count());
        model.addAttribute("metricThreeLabel", "Total Registrations");
        model.addAttribute("metricThreeValue", totalRegistrations);
        model.addAttribute("metricFourLabel", "Completed Events");
        model.addAttribute("metricFourValue", events.stream().filter(eventService::isCompleted).count());
        List<Event> dashboardEvents = events.stream().limit(5).toList();
        model.addAttribute("dashboardEvents", dashboardEvents);
        model.addAttribute("registrationCounts", eventService.registrationCounts(dashboardEvents));
        model.addAttribute("analytics", analyticsService.forEvents(events));
        return "dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Authentication authentication, Model model) {
        User user = userService.currentUser(authentication);
        List<Event> events = eventService.findAllSorted();

        model.addAttribute("user", user);
        model.addAttribute("metricOneLabel", "Total Users");
        model.addAttribute("metricOneValue", userService.count());
        model.addAttribute("metricTwoLabel", "Organizers");
        model.addAttribute("metricTwoValue", userService.countByRole("ORGANIZER"));
        model.addAttribute("metricThreeLabel", "Total Events");
        model.addAttribute("metricThreeValue", eventService.count());
        model.addAttribute("metricFourLabel", "Registrations");
        model.addAttribute("metricFourValue", registrationService.count());
        model.addAttribute("participantCount", userService.countByRole("PARTICIPANT"));
        List<Event> dashboardEvents = events.stream().limit(5).toList();
        model.addAttribute("dashboardEvents", dashboardEvents);
        model.addAttribute("registrationCounts", eventService.registrationCounts(dashboardEvents));
        model.addAttribute("recentUsers", userService.findDashboardPreview());
        model.addAttribute("analytics", analyticsService.forEvents(events));
        return "dashboard";
    }
}
