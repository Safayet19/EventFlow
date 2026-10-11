package com.eventflow.controller;

import com.eventflow.dto.EventForm;
import com.eventflow.model.Event;
import com.eventflow.model.Registration;
import com.eventflow.model.User;
import com.eventflow.service.EventService;
import com.eventflow.service.EventProgramService;
import com.eventflow.service.FeedbackService;
import com.eventflow.service.NotificationService;
import com.eventflow.service.RegistrationService;
import com.eventflow.service.UserService;
import com.eventflow.service.TicketTypeService;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class OrganizerController {

    private final UserService userService;
    private final EventService eventService;
    private final RegistrationService registrationService;
    private final NotificationService notificationService;
    private final FeedbackService feedbackService;
    private final EventProgramService eventProgramService;
    private final TicketTypeService ticketTypeService;



    @GetMapping("/organizer/events")
    public String myEvents(Authentication authentication, Model model) {
        User organizer = userService.currentUser(authentication);
        List<Event> events = eventService.findByOrganizer(organizer.getId());
        model.addAttribute("events", events);
        model.addAttribute("categories", events.stream().map(Event::getCategory)
                .filter(java.util.Objects::nonNull).distinct().sorted().toList());
        model.addAttribute("registrationCounts", eventService.registrationCounts(events));
        model.addAttribute("feedbackSummaries", feedbackService.summariesForEvents(events));
        model.addAttribute("scheduleCounts", eventProgramService.scheduleCounts(events));
        model.addAttribute("speakerCounts", eventProgramService.speakerCounts(events));
        model.addAttribute("sponsorCounts", eventProgramService.sponsorCounts(events));
        model.addAttribute("ticketCounts", ticketTypeService.ticketCounts(events));
        return "organizer-events";
    }

    @GetMapping("/organizer/events/new")
    public String newEvent(Model model) {
        model.addAttribute("event", Event.builder().build());
        model.addAttribute("currentRegistrationCount", 0L);
        model.addAttribute("pageTitle", "Create New Event");
        model.addAttribute("submitLabel", "Create Event");
        model.addAttribute("formAction", "/organizer/events");
        model.addAttribute("today", LocalDate.now());
        return "event-form";
    }

    @PostMapping("/organizer/events")
    public String createEvent(Authentication authentication,
                              @ModelAttribute EventForm form,
                              RedirectAttributes redirectAttributes) {
        User organizer = userService.currentUser(authentication);
        try {
            Event event = eventService.create(form, organizer);
            for (User admin : userService.findByRole("ADMIN")) {
                notificationService.notifyUser(
                        admin.getId(),
                        organizer.getFullName() + " created a new event: " + event.getTitle() + ".",
                        "/events/" + event.getId()
                );
            }
            redirectAttributes.addFlashAttribute("toastSuccess", "Event created successfully.");
            return "redirect:/organizer/events";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
            return "redirect:/organizer/events/new";
        }
    }

    @GetMapping("/organizer/events/{id}/edit")
    public String editEvent(@PathVariable String id, Authentication authentication, Model model,
                            RedirectAttributes redirectAttributes) {
        User organizer = userService.currentUser(authentication);
        try {
            Event event = eventService.findOwned(id, organizer);
            eventService.requireEditable(event);
            model.addAttribute("event", event);
            model.addAttribute("currentRegistrationCount", registrationService.countByEvent(id));
            model.addAttribute("pageTitle", "Edit Event");
            model.addAttribute("submitLabel", "Save Changes");
            model.addAttribute("formAction", "/organizer/events/" + id + "/edit");
            model.addAttribute("today", LocalDate.now());
            return "event-form";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
            return "redirect:/organizer/events";
        }
    }

    @PostMapping("/organizer/events/{id}/edit")
    public String updateEvent(@PathVariable String id,
                              Authentication authentication,
                              @ModelAttribute EventForm form,
                              RedirectAttributes redirectAttributes) {
        User organizer = userService.currentUser(authentication);
        try {
            eventService.updateOwned(id, form, organizer);
            redirectAttributes.addFlashAttribute("toastSuccess", "Event updated successfully.");
            return "redirect:/organizer/events";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
            if (ex.getMessage() != null && ex.getMessage().startsWith("Completed events are read-only")) {
                return "redirect:/organizer/events";
            }
            return "redirect:/organizer/events/" + id + "/edit";
        }
    }

    @PostMapping("/organizer/events/{id}/delete")
    public String deleteEvent(@PathVariable String id,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User organizer = userService.currentUser(authentication);
        try {
            eventService.deleteOwned(id, organizer);
            redirectAttributes.addFlashAttribute(
                    "toastSuccess",
                    "Event deleted and registered participants were notified."
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return "redirect:/organizer/events";
    }

    @GetMapping("/organizer/events/{id}/participants")
    public String participants(@PathVariable String id,
                               Authentication authentication,
                               Model model) {
        User organizer = userService.currentUser(authentication);
        Event event = eventService.findOwned(id, organizer);
        List<Registration> registrations = registrationService.findByEvent(id);
        Map<String, User> participantMap = new HashMap<>();
        for (Registration registration : registrations) {
            userService.findOptional(registration.getParticipantId())
                    .ifPresent(user -> participantMap.put(registration.getParticipantId(), user));
        }

        model.addAttribute("event", event);
        model.addAttribute("registrations", registrations);
        model.addAttribute("participantMap", participantMap);
        return "event-participants";
    }

    @GetMapping("/organizer/past-events")
    public String pastEvents(Authentication authentication, Model model) {
        User organizer = userService.currentUser(authentication);
        List<Event> events = eventService.findCompletedByOrganizer(organizer.getId());
        Map<String, Long> registrationCounts = eventService.registrationCounts(events);
        var feedback = feedbackService.findForEvents(events);

        model.addAttribute("events", events);
        model.addAttribute("categories", events.stream().map(Event::getCategory).filter(java.util.Objects::nonNull).distinct().sorted().toList());
        model.addAttribute("registrationCounts", registrationCounts);
        model.addAttribute("feedbackSummaries", feedbackService.summariesForEvents(events));
        model.addAttribute("overallFeedback", feedbackService.summaryOf(feedback));
        model.addAttribute("reviewedEventCount", feedbackService.countReviewedEvents(feedback));
        model.addAttribute("pastBookingCount", registrationCounts.values().stream().mapToLong(Long::longValue).sum());
        return "organizer-past-events";
    }

    @GetMapping("/organizer/bookings")
    public String bookings(Authentication authentication,
                           @RequestParam(required = false) String eventId,
                           Model model) {
        User organizer = userService.currentUser(authentication);
        List<Event> events = eventService.findByOrganizer(organizer.getId());
        List<Registration> registrations = registrationService.findForEvents(events);
        Map<String, Event> eventMap = new HashMap<>();
        Map<String, User> participantMap = new HashMap<>();

        for (Event event : events) {
            eventMap.put(event.getId(), event);
        }
        for (Registration registration : registrations) {
            userService.findOptional(registration.getParticipantId())
                    .ifPresent(user -> participantMap.put(registration.getParticipantId(), user));
        }

        long upcomingBookings = registrations.stream()
                .filter(registration -> eventService.isUpcoming(eventMap.get(registration.getEventId())))
                .count();
        long completedBookings = registrations.size() - upcomingBookings;
        long uniqueParticipants = registrations.stream().map(Registration::getParticipantId).distinct().count();
        boolean selectedEventOwned = eventId != null && eventMap.containsKey(eventId);

        model.addAttribute("registrations", registrations);
        model.addAttribute("events", events);
        model.addAttribute("eventMap", eventMap);
        model.addAttribute("participantMap", participantMap);
        model.addAttribute("selectedEventId", selectedEventOwned ? eventId : "");
        model.addAttribute("upcomingBookingCount", upcomingBookings);
        model.addAttribute("completedBookingCount", completedBookings);
        model.addAttribute("uniqueParticipantCount", uniqueParticipants);
        return "organizer-bookings";
    }

    @PostMapping("/organizer/bookings/{id}/delete")
    public String removeBooking(@PathVariable String id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            User organizer = userService.currentUser(authentication);
            registrationService.removeByOrganizer(id, organizer);
            redirectAttributes.addFlashAttribute("toastSuccess", "Booking removed successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return "redirect:/organizer/bookings";
    }
}
