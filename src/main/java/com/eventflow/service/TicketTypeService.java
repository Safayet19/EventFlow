package com.eventflow.service;

import com.eventflow.dto.TicketTypeForm;
import com.eventflow.model.Event;
import com.eventflow.model.EventTicketType;
import com.eventflow.repository.EventTicketTypeRepository;
import com.eventflow.repository.RegistrationRepository;
import org.springframework.dao.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketTypeService {

    public record TicketSelection(String ticketTypeId, String ticketTypeName, BigDecimal price, int capacity) { }

    private final EventTicketTypeRepository ticketTypeRepository;
    private final RegistrationRepository registrationRepository;



    /** Visible starting price: use actual active ticket tiers where configured. */
    public String startingPriceLabel(Event event, List<EventTicketType> tickets) {
        if (!EventRules.isPaid(event)) return "Free";
        if (tickets == null || tickets.isEmpty()) return (EventRules.basePrice(event).signum() <= 0 ? "Free" : "৳" + EventRules.basePrice(event)
                .setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString());
        return tickets.stream()
                .filter(EventTicketType::isActive)
                .map(EventTicketType::getPrice)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder())
                .map(price -> price.signum() == 0 ? "Free" :
                        "৳" + price.stripTrailingZeros().toPlainString())
                .orElse("Unavailable");
    }

    public List<EventTicketType> findByEvent(String eventId) {
        return ticketTypeRepository.findByEventIdOrderBySortOrderAscNameAsc(eventId);
    }

    public List<EventTicketType> findActiveByEvent(String eventId) {
        return ticketTypeRepository.findByEventIdAndActiveTrueOrderBySortOrderAscNameAsc(eventId);
    }

    public Map<String, List<EventTicketType>> byEvents(List<Event> events) {
        if (events == null || events.isEmpty()) return Map.of();
        Set<String> ids = events.stream().map(Event::getId).collect(Collectors.toSet());
        Map<String, List<EventTicketType>> grouped = new HashMap<>();
        for (String id : ids) grouped.put(id, new java.util.ArrayList<>());
        // One indexed MongoDB query instead of one query per event.
        for (EventTicketType ticket : ticketTypeRepository.findByEventIdIn(ids)) {
            List<EventTicketType> group = grouped.get(ticket.getEventId());
            if (group != null) group.add(ticket);
        }
        Comparator<EventTicketType> order = Comparator.comparingInt(EventTicketType::getSortOrder)
                .thenComparing(EventTicketType::getName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER));
        grouped.values().forEach(group -> group.sort(order));
        return grouped;
    }

    public Map<String, List<EventTicketType>> activeByEvents(List<Event> events) {
        Map<String, List<EventTicketType>> grouped = byEvents(events);
        Map<String, List<EventTicketType>> active = new HashMap<>();
        grouped.forEach((id, tickets) -> active.put(id,
                tickets.stream().filter(EventTicketType::isActive).toList()));
        return active;
    }

    public EventTicketType add(Event event, TicketTypeForm form) {
        EventTicketType ticket = EventTicketType.builder().eventId(event.getId()).build();
        apply(ticket, event, form, null);
        try {
            return ticketTypeRepository.save(ticket);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("A ticket type with this name already exists for the event.");
        }
    }

    public EventTicketType update(Event event, String ticketId, TicketTypeForm form) {
        EventTicketType ticket = requireOwned(event.getId(), ticketId);
        apply(ticket, event, form, ticket.getId());
        try {
            return ticketTypeRepository.save(ticket);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("A ticket type with this name already exists for the event.");
        }
    }

    public void delete(String eventId, String ticketId) {
        EventTicketType ticket = requireOwned(eventId, ticketId);
        if (registrationRepository.countByTicketTypeId(ticketId) > 0) {
            throw new IllegalArgumentException("This ticket type already has bookings and cannot be deleted. Disable it instead.");
        }
        ticketTypeRepository.delete(ticket);
    }

    public TicketSelection resolveForRegistration(Event event, String ticketTypeId) {
        List<EventTicketType> all = findByEvent(event.getId());
        if (all.isEmpty()) {
            return new TicketSelection(null, "General Admission", EventRules.basePrice(event), event.getCapacity());
        }
        List<EventTicketType> active = all.stream().filter(EventTicketType::isActive).toList();
        if (active.isEmpty()) {
            throw new IllegalArgumentException("Ticket registration is currently unavailable for this event.");
        }

        EventTicketType ticket;
        if (ticketTypeId == null || ticketTypeId.isBlank()) {
            if (active.size() != 1) {
                throw new IllegalArgumentException("Choose a ticket type before registering.");
            }
            ticket = active.get(0);
        } else {
            ticket = active.stream()
                    .filter(item -> item.getId().equals(ticketTypeId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Choose a valid available ticket type."));
        }

        long booked = registrationRepository.countByTicketTypeId(ticket.getId());
        if (booked >= ticket.getCapacity()) {
            throw new IllegalArgumentException(ticket.getName() + " tickets are sold out.");
        }
        return new TicketSelection(ticket.getId(), ticket.getName(), ticket.getPrice(), ticket.getCapacity());
    }

    public Map<String, Long> registrationCounts(String eventId) {
        Map<String, Long> counts = new HashMap<>();
        for (EventTicketType ticket : findByEvent(eventId)) {
            counts.put(ticket.getId(), registrationRepository.countByTicketTypeId(ticket.getId()));
        }
        return counts;
    }

    public Map<String, Long> ticketCounts(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }
        Set<String> eventIds = events.stream().map(Event::getId).collect(Collectors.toSet());
        Map<String, Long> counts = new HashMap<>();
        for (String eventId : eventIds) counts.put(eventId, 0L);
        for (EventTicketType type : ticketTypeRepository.findByEventIdIn(eventIds)) {
            counts.merge(type.getEventId(), 1L, Long::sum);
        }
        return counts;
    }

    public int totalAllocatedCapacity(String eventId) {
        return findByEvent(eventId).stream().mapToInt(EventTicketType::getCapacity).sum();
    }

    public void syncPricing(Event event) {
        List<EventTicketType> types = findByEvent(event.getId());
        if (types.isEmpty()) return;
        boolean free = !EventRules.isPaid(event);
        boolean changed = false;
        for (EventTicketType type : types) {
            if (free && type.getPrice() != null && type.getPrice().compareTo(BigDecimal.ZERO) != 0) {
                type.setPrice(BigDecimal.ZERO);
                changed = true;
            } else if (!free && (type.getPrice() == null || type.getPrice().compareTo(BigDecimal.ZERO) <= 0)) {
                type.setPrice(EventRules.basePrice(event));
                changed = true;
            }
        }
        if (changed) ticketTypeRepository.saveAll(types);
    }

    public void deleteAllByEvent(String eventId) {
        ticketTypeRepository.deleteAllByEventId(eventId);
    }

    private EventTicketType requireOwned(String eventId, String ticketId) {
        EventTicketType ticket = ticketTypeRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket type not found."));
        if (!eventId.equals(ticket.getEventId())) {
            throw new IllegalArgumentException("This ticket type does not belong to the event.");
        }
        return ticket;
    }

    private void apply(EventTicketType ticket, Event event, TicketTypeForm form, String editingId) {
        if (form == null) throw new IllegalArgumentException("Enter ticket information.");
        String name = clean(form.name());
        String description = clean(form.description());
        if (name.length() < 2 || name.length() > 60) {
            throw new IllegalArgumentException("Ticket name must be between 2 and 60 characters.");
        }
        if (description.length() > 250) {
            throw new IllegalArgumentException("Ticket description must be 250 characters or fewer.");
        }
        if (form.capacity() < 1 || form.capacity() > event.getCapacity()) {
            throw new IllegalArgumentException("Ticket capacity must be between 1 and the event capacity.");
        }

        long currentBookings = editingId == null ? 0 : registrationRepository.countByTicketTypeId(editingId);
        if (form.capacity() < currentBookings) {
            throw new IllegalArgumentException("Ticket capacity cannot be lower than its current bookings.");
        }

        int otherCapacity = findByEvent(event.getId()).stream()
                .filter(existing -> editingId == null || !existing.getId().equals(editingId))
                .mapToInt(EventTicketType::getCapacity)
                .sum();
        if (otherCapacity + form.capacity() > event.getCapacity()) {
            throw new IllegalArgumentException("Combined ticket capacities cannot exceed the event capacity.");
        }

        BigDecimal price = BigDecimal.ZERO;
        if (EventRules.isPaid(event)) {
            try {
                price = new BigDecimal(clean(form.price())).setScale(2, RoundingMode.HALF_UP);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Enter a valid ticket price.");
            }
            if (price.compareTo(BigDecimal.ZERO) <= 0 || price.compareTo(new BigDecimal("10000000")) > 0) {
                throw new IllegalArgumentException("Paid ticket price must be greater than 0.");
            }
        }

        ticket.setName(name);
        ticket.setDescription(description);
        ticket.setPrice(price);
        ticket.setCapacity(form.capacity());
        ticket.setActive(form.active());
        ticket.setSortOrder(Math.max(0, Math.min(form.sortOrder(), 999)));
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
