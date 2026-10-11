package com.eventflow.controller;

import com.eventflow.dto.EventForm;
import com.eventflow.model.Event;
import com.eventflow.model.Registration;
import com.eventflow.model.User;
import com.eventflow.service.EventService;
import com.eventflow.service.EventProgramService;
import com.eventflow.service.FeedbackService;
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
public class AdminController {

    private final UserService userService;
    private final EventService eventService;
    private final RegistrationService registrationService;
    private final FeedbackService feedbackService;
    private final EventProgramService eventProgramService;
    private final TicketTypeService ticketTypeService;



    @GetMapping("/admin/users")
    public String users(Model model) {
        model.addAttribute("users", userService.findAllOrdered());
        return "admin-users";
    }

    @PostMapping("/admin/users/{id}/role")
    public String changeRole(@PathVariable String id,
                             @RequestParam String role,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            User admin = userService.currentUser(authentication);
            User user = userService.changeRole(admin, id, role);
            redirectAttributes.addFlashAttribute(
                    "toastSuccess",
                    user.getFullName() + " is now " + user.getRole() + "."
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/toggle")
    public String toggleUser(@PathVariable String id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            User admin = userService.currentUser(authentication);
            User user = userService.toggleEnabled(admin, id);
            redirectAttributes.addFlashAttribute(
                    "toastSuccess",
                    user.getFullName() + (user.isEnabled() ? " has been enabled." : " has been disabled.")
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(@PathVariable String id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            User admin = userService.currentUser(authentication);
            userService.deleteUser(admin, id);
            redirectAttributes.addFlashAttribute("toastSuccess", "User deleted successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/events")
    public String events(Model model) {
        List<Event> events = eventService.findAllSorted();
        model.addAttribute("events", events);
        model.addAttribute("categories", events.stream().map(Event::getCategory).filter(java.util.Objects::nonNull).distinct().sorted().toList());
        model.addAttribute("registrationCounts", eventService.registrationCounts(events));
        model.addAttribute("feedbackSummaries", feedbackService.summariesForEvents(events));
        model.addAttribute("scheduleCounts", eventProgramService.scheduleCounts(events));
        model.addAttribute("speakerCounts", eventProgramService.speakerCounts(events));
        model.addAttribute("sponsorCounts", eventProgramService.sponsorCounts(events));
        model.addAttribute("ticketCounts", ticketTypeService.ticketCounts(events));
        return "admin-events";
    }

    @GetMapping("/admin/events/{id}/edit")
    public String editEvent(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Event event = eventService.findById(id);
            eventService.requireEditable(event);
            model.addAttribute("event", event);
            model.addAttribute("currentRegistrationCount", registrationService.countByEvent(id));
            model.addAttribute("pageTitle", "Edit Event as Admin");
            model.addAttribute("submitLabel", "Save Changes");
            model.addAttribute("formAction", "/admin/events/" + id + "/edit");
            model.addAttribute("today", LocalDate.now());
            return "event-form";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
            return "redirect:/admin/events";
        }
    }

    @PostMapping("/admin/events/{id}/edit")
    public String updateEvent(@PathVariable String id,
                              @ModelAttribute EventForm form,
                              RedirectAttributes redirectAttributes) {
        try {
            eventService.updateAsAdmin(id, form);
            redirectAttributes.addFlashAttribute("toastSuccess", "Event updated successfully.");
            return "redirect:/admin/events";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
            if (ex.getMessage() != null && ex.getMessage().startsWith("Completed events are read-only")) {
                return "redirect:/admin/events";
            }
            return "redirect:/admin/events/" + id + "/edit";
        }
    }

    @PostMapping("/admin/events/{id}/delete")
    public String deleteEvent(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            eventService.deleteAsAdmin(id);
            redirectAttributes.addFlashAttribute("toastSuccess", "Event deleted successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return "redirect:/admin/events";
    }

    @GetMapping("/admin/past-events")
    public String pastEvents(Model model) {
        List<Event> events = eventService.findCompletedAll();
        Map<String, Long> registrationCounts = eventService.registrationCounts(events);
        var feedback = feedbackService.findForEvents(events);

        model.addAttribute("events", events);
        model.addAttribute("categories", events.stream().map(Event::getCategory).filter(java.util.Objects::nonNull).distinct().sorted().toList());
        model.addAttribute("registrationCounts", registrationCounts);
        model.addAttribute("feedbackSummaries", feedbackService.summariesForEvents(events));
        model.addAttribute("overallFeedback", feedbackService.summaryOf(feedback));
        model.addAttribute("reviewedEventCount", feedbackService.countReviewedEvents(feedback));
        model.addAttribute("pastBookingCount", registrationCounts.values().stream().mapToLong(Long::longValue).sum());
        return "admin-past-events";
    }

    @GetMapping("/admin/registrations")
    public String registrations(@RequestParam(required = false) String eventId, Model model) {
        List<Registration> registrations = registrationService.findAllNewestFirst();
        List<Event> events = eventService.findAllSorted();
        Map<String, Event> eventMap = new HashMap<>();
        Map<String, User> participantMap = new HashMap<>();

        for (Event event : events) {
            eventMap.put(event.getId(), event);
        }
        for (Registration registration : registrations) {
            if (!eventMap.containsKey(registration.getEventId())) {
                eventService.findOptional(registration.getEventId())
                        .ifPresent(event -> eventMap.put(registration.getEventId(), event));
            }
            userService.findOptional(registration.getParticipantId())
                    .ifPresent(user -> participantMap.put(registration.getParticipantId(), user));
        }

        long upcomingBookings = registrations.stream()
                .filter(registration -> eventService.isUpcoming(eventMap.get(registration.getEventId())))
                .count();
        long completedBookings = registrations.size() - upcomingBookings;
        long uniqueParticipants = registrations.stream().map(Registration::getParticipantId).distinct().count();
        boolean selectedEventExists = eventId != null && eventMap.containsKey(eventId);

        model.addAttribute("registrations", registrations);
        model.addAttribute("events", events);
        model.addAttribute("eventMap", eventMap);
        model.addAttribute("participantMap", participantMap);
        model.addAttribute("selectedEventId", selectedEventExists ? eventId : "");
        model.addAttribute("upcomingBookingCount", upcomingBookings);
        model.addAttribute("completedBookingCount", completedBookings);
        model.addAttribute("uniqueParticipantCount", uniqueParticipants);
        return "admin-registrations";
    }

    @PostMapping("/admin/registrations/{id}/delete")
    public String deleteRegistration(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            registrationService.removeByAdmin(id);
            redirectAttributes.addFlashAttribute("toastSuccess", "Registration removed successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return "redirect:/admin/registrations";
    }
}
