package com.eventflow.service;

import com.eventflow.dto.EventForm;
import com.eventflow.model.Event;
import com.eventflow.model.EventImage;
import com.eventflow.model.Registration;
import com.eventflow.model.User;
import com.eventflow.repository.EventImageRepository;
import com.eventflow.repository.EventRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EventService {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> EVENT_CATEGORIES = Set.of(
            "Technology", "Business", "Education", "Music", "Career", "Sports", "Community", "Other", "Health", "Design"
    );
    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024;
    private static final String DEFAULT_IMAGE = "/images/event-visual.png";

    private final EventRepository eventRepository;
    private final EventImageRepository eventImageRepository;
    private final RegistrationService registrationService;
    private final NotificationService notificationService;
    private final EventProgramService eventProgramService;
    private final TicketTypeService ticketTypeService;
    private final CouponService couponService;
    private final EventGalleryService eventGalleryService;



    @Caching(evict = {
            @CacheEvict(value = "eventsAll", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "featuredEvents", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "eventsByOrganizer", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "eventCount", allEntries = true, beforeInvocation = true)
    })
    public Event create(EventForm form, User organizer) {
        validate(form, 0L, null);
        Event event = Event.builder()
                .organizerId(organizer.getId())
                .organizerName(organizer.getFullName())
                .imageUrl(DEFAULT_IMAGE)
                .build();
        applyFields(event, form);
        event = eventRepository.save(event);
        saveImage(event, form.image());
        return event;
    }

    @Caching(evict = {
            @CacheEvict(value = "eventsAll", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "featuredEvents", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "eventsByOrganizer", allEntries = true, beforeInvocation = true)
    })
    public Event updateOwned(String eventId, EventForm form, User organizer) {
        Event event = findOwned(eventId, organizer);
        requireEditable(event);
        update(event, form);
        notifyParticipants(event, event.getTitle() + " has been updated. Please review the latest event details.");
        return event;
    }

    @Caching(evict = {
            @CacheEvict(value = "eventsAll", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "featuredEvents", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "eventsByOrganizer", allEntries = true, beforeInvocation = true)
    })
    public Event updateAsAdmin(String eventId, EventForm form) {
        Event event = findById(eventId);
        requireEditable(event);
        update(event, form);
        notifyParticipants(event, event.getTitle() + " was updated by an administrator.");
        return event;
    }

    @Caching(evict = {
            @CacheEvict(value = "eventsAll", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "featuredEvents", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "eventsByOrganizer", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "eventCount", allEntries = true, beforeInvocation = true)
    })
    public void deleteOwned(String eventId, User organizer) {
        Event event = findOwned(eventId, organizer);
        requireEditable(event);
        notifyParticipants(event, event.getTitle() + " has been cancelled by the organizer.");
        registrationService.deleteAllByEvent(eventId);
        eventProgramService.deleteAllByEvent(eventId);
        ticketTypeService.deleteAllByEvent(eventId);
        couponService.deleteAllByEvent(eventId);
        eventGalleryService.deleteAllByEvent(eventId);
        eventImageRepository.deleteByEventId(eventId);
        eventRepository.delete(event);
    }

    @Caching(evict = {
            @CacheEvict(value = "eventsAll", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "featuredEvents", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "eventsByOrganizer", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "eventCount", allEntries = true, beforeInvocation = true)
    })
    public void deleteAsAdmin(String eventId) {
        Event event = findById(eventId);
        requireEditable(event);
        notifyParticipants(event, event.getTitle() + " was cancelled by an administrator.");
        registrationService.deleteAllByEvent(eventId);
        eventProgramService.deleteAllByEvent(eventId);
        ticketTypeService.deleteAllByEvent(eventId);
        couponService.deleteAllByEvent(eventId);
        eventGalleryService.deleteAllByEvent(eventId);
        eventImageRepository.deleteByEventId(eventId);
        eventRepository.delete(event);
    }

    public Event findById(String id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));
    }

    public Optional<Event> findOptional(String id) {
        return eventRepository.findById(id);
    }

    public Optional<EventImage> findImage(String eventId) {
        return eventImageRepository.findByEventId(eventId);
    }

    public Event findOwned(String id, User organizer) {
        Event event = findById(id);
        if (event.getOrganizerId() == null || !event.getOrganizerId().equals(organizer.getId())) {
            throw new IllegalArgumentException("You cannot manage this event.");
        }
        return event;
    }

    @Cacheable("eventsAll")
    public List<Event> findAllSorted() {
        return eventRepository.findAllByOrderByDateAsc();
    }

    @Cacheable("featuredEvents")
    public List<Event> findFeatured() {
        return eventRepository.findTop3ByFeaturedTrueOrderByDateAsc();
    }

    @Cacheable(value = "eventsByOrganizer", key = "#organizerId")
    public List<Event> findByOrganizer(String organizerId) {
        return eventRepository.findByOrganizerIdOrderByDateAsc(organizerId);
    }

    public List<Event> findCompletedAll() {
        return completedNewestFirst(findAllSorted());
    }

    public List<Event> findCompletedByOrganizer(String organizerId) {
        return completedNewestFirst(findByOrganizer(organizerId));
    }

    public boolean isCompleted(Event event) {
        return event != null && "COMPLETED".equals(EventRules.status(event));
    }

    public void requireEditable(Event event) {
        if (isCompleted(event)) {
            throw new IllegalArgumentException("Completed events are read-only and cannot be changed.");
        }
    }

    private List<Event> completedNewestFirst(List<Event> events) {
        return events.stream()
                .filter(this::isCompleted)
                .sorted(Comparator
                        .comparing(Event::getDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Event::getTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Cacheable("eventCount")
    public long count() {
        return eventRepository.count();
    }

    public Map<String, Long> registrationCounts(List<Event> events) {
        if (events == null || events.isEmpty()) return Map.of();
        return registrationService.countsForEvents(events.stream().map(Event::getId).toList());
    }

    public boolean isUpcoming(Event event) {
        if (event.getDate() == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (event.getDate().isAfter(today)) {
            return true;
        }
        if (event.getDate().isBefore(today)) {
            return false;
        }
        return event.getTime() == null
                || !event.getTime().isBefore(LocalTime.now().withSecond(0).withNano(0));
    }

    private void update(Event event, EventForm form) {
        long currentRegistrations = registrationService.countByEvent(event.getId());
        validate(form, currentRegistrations, event.getId());
        applyFields(event, form);
        eventRepository.save(event);
        ticketTypeService.syncPricing(event);
        if (!EventRules.isPaid(event)) couponService.pauseAllByEvent(event.getId());
        saveImage(event, form.image());
    }

    private void validate(EventForm form, long currentRegistrations, String eventId) {
        String title = form.title();
        String category = form.category();
        String location = form.location();
        String description = form.description();

        if (title == null || title.trim().length() < 3 || title.trim().length() > 80) {
            throw new IllegalArgumentException("Event title must be between 3 and 80 characters.");
        }
        if (category == null || !EVENT_CATEGORIES.contains(category.trim())) {
            throw new IllegalArgumentException("Choose a valid event category.");
        }
        if (location == null || location.trim().length() < 3 || location.trim().length() > 100) {
            throw new IllegalArgumentException("Location must be between 3 and 100 characters.");
        }
        if (description == null || description.trim().length() < 20 || description.trim().length() > 1000) {
            throw new IllegalArgumentException("Description must be between 20 and 1000 characters.");
        }
        if (form.capacity() < 1 || form.capacity() > 100000) {
            throw new IllegalArgumentException("Capacity must be greater than 0.");
        }
        if (form.capacity() < currentRegistrations) {
            throw new IllegalArgumentException("Capacity cannot be lower than the current number of registrations.");
        }
        if (eventId != null && form.capacity() < ticketTypeService.totalAllocatedCapacity(eventId)) {
            throw new IllegalArgumentException("Capacity cannot be lower than the total capacity allocated to ticket types.");
        }

        String pricingType = form.pricingType() == null ? "FREE" : form.pricingType().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("FREE", "PAID").contains(pricingType)) {
            throw new IllegalArgumentException("Choose whether the event is free or paid.");
        }
        if ("PAID".equals(pricingType)) {
            BigDecimal basePrice;
            try {
                basePrice = new BigDecimal(form.basePrice() == null ? "" : form.basePrice().trim()).setScale(2, RoundingMode.HALF_UP);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Enter a valid base ticket price.");
            }
            if (basePrice.compareTo(BigDecimal.ZERO) <= 0 || basePrice.compareTo(new BigDecimal("10000000")) > 0) {
                throw new IllegalArgumentException("Paid event price must be greater than 0.");
            }
        }

        LocalDate parsedDate;
        LocalTime parsedTime;
        try {
            parsedDate = LocalDate.parse(form.date());
            parsedTime = LocalTime.parse(form.time());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Enter a valid event date and time.");
        }

        LocalDate today = LocalDate.now();
        if (parsedDate.isBefore(today)
                || (parsedDate.isEqual(today)
                && parsedTime.isBefore(LocalTime.now().withSecond(0).withNano(0)))) {
            throw new IllegalArgumentException("Event date and time cannot be in the past.");
        }

        MultipartFile image = form.image();
        if (image != null && !image.isEmpty()) {
            if (image.getSize() > MAX_IMAGE_SIZE) {
                throw new IllegalArgumentException("Event image must be 2 MB or smaller.");
            }
            String type = image.getContentType() == null ? "" : image.getContentType().toLowerCase(Locale.ROOT);
            if (!IMAGE_TYPES.contains(type)) {
                throw new IllegalArgumentException("Event image must be JPG, PNG or WEBP.");
            }
        }
    }

    private void applyFields(Event event, EventForm form) {
        event.setTitle(form.title().trim());
        event.setCategory(form.category().trim());
        event.setLocation(form.location().trim());
        event.setDate(LocalDate.parse(form.date()));
        event.setTime(LocalTime.parse(form.time()));
        event.setDescription(form.description().trim());
        event.setCapacity(form.capacity());
        String pricingType = form.pricingType() == null ? "FREE" : form.pricingType().trim().toUpperCase(Locale.ROOT);
        event.setPricingType("PAID".equals(pricingType) ? "PAID" : "FREE");
        if (EventRules.isPaid(event)) {
            event.setBasePrice(new BigDecimal(form.basePrice().trim()).setScale(2, RoundingMode.HALF_UP));
        } else {
            event.setBasePrice(BigDecimal.ZERO);
        }
        event.setStatus("UPCOMING");
    }

    private void saveImage(Event event, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return;
        }

        try {
            String type = image.getContentType() == null
                    ? "image/jpeg"
                    : image.getContentType().toLowerCase(Locale.ROOT);
            eventImageRepository.deleteByEventId(event.getId());
            EventImage storedImage = eventImageRepository.save(EventImage.builder().eventId(event.getId()).contentType(type).data(image.getBytes()).build());
            event.setImageUrl("/event-images/" + event.getId() + "?v=" + storedImage.getId());
            eventRepository.save(event);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not save the event image. Try again.");
        }
    }

    private void notifyParticipants(Event event, String message) {
        boolean cancelled = message != null && message.toLowerCase(Locale.ROOT).contains("cancelled");
        String link = cancelled
                ? "/participant/registrations"
                : "/events/" + event.getId() + ("COMPLETED".equals(EventRules.status(event)) ? "#participant-feedback" : "");
        for (Registration registration : registrationService.findByEvent(event.getId())) {
            notificationService.notifyUser(
                    registration.getParticipantId(),
                    message,
                    link
            );
        }
    }
}
