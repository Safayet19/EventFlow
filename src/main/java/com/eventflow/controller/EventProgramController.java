package com.eventflow.controller;

import com.eventflow.dto.ScheduleItemForm;
import com.eventflow.dto.SpeakerForm;
import com.eventflow.dto.SponsorForm;
import com.eventflow.dto.TicketTypeForm;
import com.eventflow.model.Event;
import com.eventflow.model.EventScheduleItem;
import com.eventflow.model.EventSpeaker;
import com.eventflow.model.User;
import com.eventflow.service.EventProgramService;
import com.eventflow.service.EventGalleryService;
import com.eventflow.service.EventService;
import com.eventflow.service.TicketTypeService;
import com.eventflow.service.UserService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class EventProgramController {

    private final UserService userService;
    private final EventService eventService;
    private final EventProgramService eventProgramService;
    private final TicketTypeService ticketTypeService;
    private final EventGalleryService eventGalleryService;



    @GetMapping("/organizer/events/{eventId}/program")
    public String organizerProgram(@PathVariable String eventId,
                                   Authentication authentication,
                                   Model model) {
        User organizer = userService.currentUser(authentication);
        Event event = eventService.findOwned(eventId, organizer);
        populateProgramModel(event, "organizer", model);
        return "event-program";
    }

    @GetMapping("/admin/events/{eventId}/program")
    public String adminProgram(@PathVariable String eventId, Model model) {
        Event event = eventService.findById(eventId);
        populateProgramModel(event, "admin", model);
        return "event-program";
    }

    @PostMapping("/organizer/events/{eventId}/schedule")
    public String addOrganizerSchedule(@PathVariable String eventId, Authentication authentication,
                                       @ModelAttribute ScheduleItemForm form, RedirectAttributes redirectAttributes) {
        return addSchedule(eventId, authentication, false, form, redirectAttributes);
    }

    @PostMapping("/admin/events/{eventId}/schedule")
    public String addAdminSchedule(@PathVariable String eventId, @ModelAttribute ScheduleItemForm form,
                                   RedirectAttributes redirectAttributes) {
        return addSchedule(eventId, null, true, form, redirectAttributes);
    }

    @PostMapping("/organizer/events/{eventId}/schedule/{itemId}/edit")
    public String editOrganizerSchedule(@PathVariable String eventId, @PathVariable String itemId,
                                        Authentication authentication, @ModelAttribute ScheduleItemForm form,
                                        RedirectAttributes redirectAttributes) {
        return editSchedule(eventId, itemId, authentication, false, form, redirectAttributes);
    }

    @PostMapping("/admin/events/{eventId}/schedule/{itemId}/edit")
    public String editAdminSchedule(@PathVariable String eventId, @PathVariable String itemId,
                                    @ModelAttribute ScheduleItemForm form, RedirectAttributes redirectAttributes) {
        return editSchedule(eventId, itemId, null, true, form, redirectAttributes);
    }

    @PostMapping("/organizer/events/{eventId}/schedule/{itemId}/delete")
    public String deleteOrganizerSchedule(@PathVariable String eventId, @PathVariable String itemId,
                                          Authentication authentication, RedirectAttributes redirectAttributes) {
        return deleteSchedule(eventId, itemId, authentication, false, redirectAttributes);
    }

    @PostMapping("/admin/events/{eventId}/schedule/{itemId}/delete")
    public String deleteAdminSchedule(@PathVariable String eventId, @PathVariable String itemId,
                                      RedirectAttributes redirectAttributes) {
        return deleteSchedule(eventId, itemId, null, true, redirectAttributes);
    }

    @PostMapping("/organizer/events/{eventId}/speakers")
    public String addOrganizerSpeaker(@PathVariable String eventId, Authentication authentication,
                                      @ModelAttribute SpeakerForm form, RedirectAttributes redirectAttributes) {
        return addSpeaker(eventId, authentication, false, form, redirectAttributes);
    }

    @PostMapping("/admin/events/{eventId}/speakers")
    public String addAdminSpeaker(@PathVariable String eventId, @ModelAttribute SpeakerForm form,
                                  RedirectAttributes redirectAttributes) {
        return addSpeaker(eventId, null, true, form, redirectAttributes);
    }

    @PostMapping("/organizer/events/{eventId}/speakers/{speakerId}/edit")
    public String editOrganizerSpeaker(@PathVariable String eventId, @PathVariable String speakerId,
                                       Authentication authentication, @ModelAttribute SpeakerForm form,
                                       RedirectAttributes redirectAttributes) {
        return editSpeaker(eventId, speakerId, authentication, false, form, redirectAttributes);
    }

    @PostMapping("/admin/events/{eventId}/speakers/{speakerId}/edit")
    public String editAdminSpeaker(@PathVariable String eventId, @PathVariable String speakerId,
                                   @ModelAttribute SpeakerForm form, RedirectAttributes redirectAttributes) {
        return editSpeaker(eventId, speakerId, null, true, form, redirectAttributes);
    }

    @PostMapping("/organizer/events/{eventId}/speakers/{speakerId}/delete")
    public String deleteOrganizerSpeaker(@PathVariable String eventId, @PathVariable String speakerId,
                                         Authentication authentication, RedirectAttributes redirectAttributes) {
        return deleteSpeaker(eventId, speakerId, authentication, false, redirectAttributes);
    }

    @PostMapping("/admin/events/{eventId}/speakers/{speakerId}/delete")
    public String deleteAdminSpeaker(@PathVariable String eventId, @PathVariable String speakerId,
                                     RedirectAttributes redirectAttributes) {
        return deleteSpeaker(eventId, speakerId, null, true, redirectAttributes);
    }

    @PostMapping("/organizer/events/{eventId}/sponsors")
    public String addOrganizerSponsor(@PathVariable String eventId, Authentication authentication,
                                      @ModelAttribute SponsorForm form, RedirectAttributes redirectAttributes) {
        return addSponsor(eventId, authentication, false, form, redirectAttributes);
    }

    @PostMapping("/admin/events/{eventId}/sponsors")
    public String addAdminSponsor(@PathVariable String eventId, @ModelAttribute SponsorForm form,
                                  RedirectAttributes redirectAttributes) {
        return addSponsor(eventId, null, true, form, redirectAttributes);
    }

    @PostMapping("/organizer/events/{eventId}/sponsors/{sponsorId}/edit")
    public String editOrganizerSponsor(@PathVariable String eventId, @PathVariable String sponsorId,
                                       Authentication authentication, @ModelAttribute SponsorForm form,
                                       RedirectAttributes redirectAttributes) {
        return editSponsor(eventId, sponsorId, authentication, false, form, redirectAttributes);
    }

    @PostMapping("/admin/events/{eventId}/sponsors/{sponsorId}/edit")
    public String editAdminSponsor(@PathVariable String eventId, @PathVariable String sponsorId,
                                   @ModelAttribute SponsorForm form, RedirectAttributes redirectAttributes) {
        return editSponsor(eventId, sponsorId, null, true, form, redirectAttributes);
    }

    @PostMapping("/organizer/events/{eventId}/sponsors/{sponsorId}/delete")
    public String deleteOrganizerSponsor(@PathVariable String eventId, @PathVariable String sponsorId,
                                         Authentication authentication, RedirectAttributes redirectAttributes) {
        return deleteSponsor(eventId, sponsorId, authentication, false, redirectAttributes);
    }

    @PostMapping("/admin/events/{eventId}/sponsors/{sponsorId}/delete")
    public String deleteAdminSponsor(@PathVariable String eventId, @PathVariable String sponsorId,
                                     RedirectAttributes redirectAttributes) {
        return deleteSponsor(eventId, sponsorId, null, true, redirectAttributes);
    }


    @PostMapping("/organizer/events/{eventId}/gallery")
    public String addOrganizerGallery(@PathVariable String eventId, Authentication authentication,
                                      @RequestParam("image") MultipartFile image,
                                      @RequestParam(value = "caption", defaultValue = "") String caption,
                                      @RequestParam(value = "sortOrder", defaultValue = "0") int sortOrder,
                                      RedirectAttributes redirectAttributes) {
        return addGallery(eventId, authentication, false, image, caption, sortOrder, redirectAttributes);
    }

    @PostMapping("/admin/events/{eventId}/gallery")
    public String addAdminGallery(@PathVariable String eventId,
                                  @RequestParam("image") MultipartFile image,
                                  @RequestParam(value = "caption", defaultValue = "") String caption,
                                  @RequestParam(value = "sortOrder", defaultValue = "0") int sortOrder,
                                  RedirectAttributes redirectAttributes) {
        return addGallery(eventId, null, true, image, caption, sortOrder, redirectAttributes);
    }

    @PostMapping("/organizer/events/{eventId}/gallery/{imageId}/edit")
    public String editOrganizerGallery(@PathVariable String eventId, @PathVariable String imageId,
                                       Authentication authentication,
                                       @RequestParam(value = "caption", defaultValue = "") String caption,
                                       @RequestParam(value = "sortOrder", defaultValue = "0") int sortOrder,
                                       RedirectAttributes redirectAttributes) {
        return editGallery(eventId, imageId, authentication, false, caption, sortOrder, redirectAttributes);
    }

    @PostMapping("/admin/events/{eventId}/gallery/{imageId}/edit")
    public String editAdminGallery(@PathVariable String eventId, @PathVariable String imageId,
                                   @RequestParam(value = "caption", defaultValue = "") String caption,
                                   @RequestParam(value = "sortOrder", defaultValue = "0") int sortOrder,
                                   RedirectAttributes redirectAttributes) {
        return editGallery(eventId, imageId, null, true, caption, sortOrder, redirectAttributes);
    }

    @PostMapping("/organizer/events/{eventId}/gallery/{imageId}/delete")
    public String deleteOrganizerGallery(@PathVariable String eventId, @PathVariable String imageId,
                                         Authentication authentication, RedirectAttributes redirectAttributes) {
        return deleteGallery(eventId, imageId, authentication, false, redirectAttributes);
    }

    @PostMapping("/admin/events/{eventId}/gallery/{imageId}/delete")
    public String deleteAdminGallery(@PathVariable String eventId, @PathVariable String imageId,
                                     RedirectAttributes redirectAttributes) {
        return deleteGallery(eventId, imageId, null, true, redirectAttributes);
    }

    @PostMapping("/organizer/events/{eventId}/tickets")
    public String addOrganizerTicket(@PathVariable String eventId, Authentication authentication,
                                     @ModelAttribute TicketTypeForm form, RedirectAttributes redirectAttributes) {
        return addTicket(eventId, authentication, false, form, redirectAttributes);
    }

    @PostMapping("/admin/events/{eventId}/tickets")
    public String addAdminTicket(@PathVariable String eventId, @ModelAttribute TicketTypeForm form,
                                 RedirectAttributes redirectAttributes) {
        return addTicket(eventId, null, true, form, redirectAttributes);
    }

    @PostMapping("/organizer/events/{eventId}/tickets/{ticketId}/edit")
    public String editOrganizerTicket(@PathVariable String eventId, @PathVariable String ticketId,
                                      Authentication authentication, @ModelAttribute TicketTypeForm form,
                                      RedirectAttributes redirectAttributes) {
        return editTicket(eventId, ticketId, authentication, false, form, redirectAttributes);
    }

    @PostMapping("/admin/events/{eventId}/tickets/{ticketId}/edit")
    public String editAdminTicket(@PathVariable String eventId, @PathVariable String ticketId,
                                  @ModelAttribute TicketTypeForm form, RedirectAttributes redirectAttributes) {
        return editTicket(eventId, ticketId, null, true, form, redirectAttributes);
    }

    @PostMapping("/organizer/events/{eventId}/tickets/{ticketId}/delete")
    public String deleteOrganizerTicket(@PathVariable String eventId, @PathVariable String ticketId,
                                        Authentication authentication, RedirectAttributes redirectAttributes) {
        return deleteTicket(eventId, ticketId, authentication, false, redirectAttributes);
    }

    @PostMapping("/admin/events/{eventId}/tickets/{ticketId}/delete")
    public String deleteAdminTicket(@PathVariable String eventId, @PathVariable String ticketId,
                                    RedirectAttributes redirectAttributes) {
        return deleteTicket(eventId, ticketId, null, true, redirectAttributes);
    }

    private String addSchedule(String eventId, Authentication authentication, boolean admin,
                               ScheduleItemForm form, RedirectAttributes redirectAttributes) {
        try {
            Event event = managedEvent(eventId, authentication, admin);
            eventProgramService.addScheduleItem(event, form);
            redirectAttributes.addFlashAttribute("toastSuccess", "Schedule session added successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return redirectProgram(eventId, admin);
    }

    private String editSchedule(String eventId, String itemId, Authentication authentication, boolean admin,
                                ScheduleItemForm form, RedirectAttributes redirectAttributes) {
        try {
            Event event = managedEvent(eventId, authentication, admin);
            eventProgramService.updateScheduleItem(event, itemId, form);
            redirectAttributes.addFlashAttribute("toastSuccess", "Schedule session updated successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return redirectProgram(eventId, admin);
    }

    private String deleteSchedule(String eventId, String itemId, Authentication authentication, boolean admin,
                                  RedirectAttributes redirectAttributes) {
        try {
            managedEvent(eventId, authentication, admin);
            eventProgramService.deleteScheduleItem(eventId, itemId);
            redirectAttributes.addFlashAttribute("toastSuccess", "Schedule session removed.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return redirectProgram(eventId, admin);
    }

    private String addSpeaker(String eventId, Authentication authentication, boolean admin,
                              SpeakerForm form, RedirectAttributes redirectAttributes) {
        try {
            Event event = managedEvent(eventId, authentication, admin);
            eventProgramService.addSpeaker(event, form);
            redirectAttributes.addFlashAttribute("toastSuccess", "Speaker added successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return redirectProgram(eventId, admin);
    }

    private String editSpeaker(String eventId, String speakerId, Authentication authentication, boolean admin,
                               SpeakerForm form, RedirectAttributes redirectAttributes) {
        try {
            Event event = managedEvent(eventId, authentication, admin);
            eventProgramService.updateSpeaker(event, speakerId, form);
            redirectAttributes.addFlashAttribute("toastSuccess", "Speaker updated successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return redirectProgram(eventId, admin);
    }

    private String deleteSpeaker(String eventId, String speakerId, Authentication authentication, boolean admin,
                                 RedirectAttributes redirectAttributes) {
        try {
            managedEvent(eventId, authentication, admin);
            eventProgramService.deleteSpeaker(eventId, speakerId);
            redirectAttributes.addFlashAttribute("toastSuccess", "Speaker removed successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return redirectProgram(eventId, admin);
    }

    private String addSponsor(String eventId, Authentication authentication, boolean admin,
                              SponsorForm form, RedirectAttributes redirectAttributes) {
        try {
            Event event = managedEvent(eventId, authentication, admin);
            eventProgramService.addSponsor(event, form);
            redirectAttributes.addFlashAttribute("toastSuccess", "Sponsor added successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return redirectProgram(eventId, admin);
    }

    private String editSponsor(String eventId, String sponsorId, Authentication authentication, boolean admin,
                               SponsorForm form, RedirectAttributes redirectAttributes) {
        try {
            Event event = managedEvent(eventId, authentication, admin);
            eventProgramService.updateSponsor(event, sponsorId, form);
            redirectAttributes.addFlashAttribute("toastSuccess", "Sponsor updated successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return redirectProgram(eventId, admin);
    }

    private String deleteSponsor(String eventId, String sponsorId, Authentication authentication, boolean admin,
                                 RedirectAttributes redirectAttributes) {
        try {
            managedEvent(eventId, authentication, admin);
            eventProgramService.deleteSponsor(eventId, sponsorId);
            redirectAttributes.addFlashAttribute("toastSuccess", "Sponsor removed successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return redirectProgram(eventId, admin);
    }


    private String addGallery(String eventId, Authentication authentication, boolean admin, MultipartFile image,
                              String caption, int sortOrder, RedirectAttributes redirectAttributes) {
        try {
            managedEvent(eventId, authentication, admin);
            eventGalleryService.add(eventId, image, caption, sortOrder);
            redirectAttributes.addFlashAttribute("toastSuccess", "Gallery image added successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return redirectProgram(eventId, admin) + "#gallery";
    }

    private String editGallery(String eventId, String imageId, Authentication authentication, boolean admin,
                               String caption, int sortOrder, RedirectAttributes redirectAttributes) {
        try {
            managedEvent(eventId, authentication, admin);
            eventGalleryService.update(eventId, imageId, caption, sortOrder);
            redirectAttributes.addFlashAttribute("toastSuccess", "Gallery details updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return redirectProgram(eventId, admin) + "#gallery";
    }

    private String deleteGallery(String eventId, String imageId, Authentication authentication, boolean admin,
                                 RedirectAttributes redirectAttributes) {
        try {
            managedEvent(eventId, authentication, admin);
            eventGalleryService.delete(eventId, imageId);
            redirectAttributes.addFlashAttribute("toastSuccess", "Gallery image removed.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return redirectProgram(eventId, admin) + "#gallery";
    }

    private String addTicket(String eventId, Authentication authentication, boolean admin,
                             TicketTypeForm form, RedirectAttributes redirectAttributes) {
        try {
            Event event = managedEvent(eventId, authentication, admin);
            ticketTypeService.add(event, form);
            redirectAttributes.addFlashAttribute("toastSuccess", "Ticket type added successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return redirectProgram(eventId, admin);
    }

    private String editTicket(String eventId, String ticketId, Authentication authentication, boolean admin,
                              TicketTypeForm form, RedirectAttributes redirectAttributes) {
        try {
            Event event = managedEvent(eventId, authentication, admin);
            ticketTypeService.update(event, ticketId, form);
            redirectAttributes.addFlashAttribute("toastSuccess", "Ticket type updated successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return redirectProgram(eventId, admin);
    }

    private String deleteTicket(String eventId, String ticketId, Authentication authentication, boolean admin,
                                RedirectAttributes redirectAttributes) {
        try {
            managedEvent(eventId, authentication, admin);
            ticketTypeService.delete(eventId, ticketId);
            redirectAttributes.addFlashAttribute("toastSuccess", "Ticket type removed successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return redirectProgram(eventId, admin);
    }

    private Event managedEvent(String eventId, Authentication authentication, boolean admin) {
        Event event;
        if (admin) {
            event = eventService.findById(eventId);
        } else {
            User organizer = userService.currentUser(authentication);
            event = eventService.findOwned(eventId, organizer);
        }
        eventService.requireEditable(event);
        return event;
    }

    private void populateProgramModel(Event event, String mode, Model model) {
        List<EventScheduleItem> schedule = eventProgramService.findSchedule(event.getId());
        List<EventSpeaker> speakers = eventProgramService.findSpeakers(event.getId());
        var sponsors = eventProgramService.findSponsors(event.getId());
        var ticketTypes = ticketTypeService.findByEvent(event.getId());
        var gallery = eventGalleryService.findByEvent(event.getId());

        model.addAttribute("event", event);
        model.addAttribute("schedule", schedule);
        model.addAttribute("speakers", speakers);
        model.addAttribute("sponsors", sponsors);
        model.addAttribute("ticketTypes", ticketTypes);
        model.addAttribute("gallery", gallery);
        model.addAttribute("ticketRegistrationCounts", ticketTypeService.registrationCounts(event.getId()));
        model.addAttribute("allocatedTicketCapacity", ticketTypeService.totalAllocatedCapacity(event.getId()));
        model.addAttribute("speakerMap", eventProgramService.speakerMap(speakers));
        model.addAttribute("programMode", mode);
        model.addAttribute("programReadOnly", eventService.isCompleted(event));
        model.addAttribute("programBase", "/" + mode + "/events/" + event.getId());
        model.addAttribute("backUrl", "admin".equals(mode) ? "/admin/events" : "/organizer/events");
    }

    private String redirectProgram(String eventId, boolean admin) {
        return "redirect:/" + (admin ? "admin" : "organizer") + "/events/" + eventId + "/program";
    }
}
