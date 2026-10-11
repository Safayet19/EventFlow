package com.eventflow.service;

import com.eventflow.dto.ScheduleItemForm;
import com.eventflow.dto.SpeakerForm;
import com.eventflow.dto.SponsorForm;
import com.eventflow.model.Event;
import com.eventflow.model.EventScheduleItem;
import com.eventflow.model.EventSpeaker;
import com.eventflow.model.EventSponsor;
import com.eventflow.model.SpeakerImage;
import com.eventflow.model.SponsorImage;
import com.eventflow.repository.EventScheduleRepository;
import com.eventflow.repository.EventSpeakerRepository;
import com.eventflow.repository.EventSponsorRepository;
import com.eventflow.repository.SpeakerImageRepository;
import com.eventflow.repository.SponsorImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventProgramService {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> SPONSOR_LEVELS = Set.of("TITLE", "GOLD", "SILVER", "BRONZE", "PARTNER");
    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024;
    private static final String DEFAULT_SPEAKER_IMAGE = "/images/default-avatar.png";

    private final EventScheduleRepository scheduleRepository;
    private final EventSpeakerRepository speakerRepository;
    private final SpeakerImageRepository speakerImageRepository;
    private final EventSponsorRepository sponsorRepository;
    private final SponsorImageRepository sponsorImageRepository;



    public List<EventScheduleItem> findSchedule(String eventId) {
        return scheduleRepository.findByEventIdOrderByStartTimeAsc(eventId);
    }

    public List<EventSpeaker> findSpeakers(String eventId) {
        return speakerRepository.findByEventIdOrderByNameAsc(eventId);
    }

    public List<EventSponsor> findSponsors(String eventId) {
        List<EventSponsor> sponsors = new ArrayList<>(sponsorRepository.findByEventId(eventId));
        sponsors.sort(Comparator
                .comparingInt((EventSponsor sponsor) -> sponsorLevelRank(sponsor.getLevel()))
                .thenComparingInt(EventSponsor::getSortOrder)
                .thenComparing(EventSponsor::getName, String.CASE_INSENSITIVE_ORDER));
        return sponsors;
    }

    public Optional<SpeakerImage> findSpeakerImage(String speakerId) {
        return speakerImageRepository.findBySpeakerId(speakerId);
    }

    public Optional<SponsorImage> findSponsorImage(String sponsorId) {
        return sponsorImageRepository.findBySponsorId(sponsorId);
    }

    public EventScheduleItem addScheduleItem(Event event, ScheduleItemForm form) {
        EventScheduleItem item = EventScheduleItem.builder().eventId(event.getId()).build();
        applyScheduleFields(item, form, event);
        return scheduleRepository.save(item);
    }

    public EventScheduleItem updateScheduleItem(Event event, String itemId, ScheduleItemForm form) {
        EventScheduleItem item = requireScheduleItem(event.getId(), itemId);
        applyScheduleFields(item, form, event);
        return scheduleRepository.save(item);
    }

    public void deleteScheduleItem(String eventId, String itemId) {
        EventScheduleItem item = requireScheduleItem(eventId, itemId);
        scheduleRepository.delete(item);
    }

    public EventSpeaker addSpeaker(Event event, SpeakerForm form) {
        validateSpeakerForm(form);
        EventSpeaker speaker = EventSpeaker.builder().eventId(event.getId()).build();
        applySpeakerFields(speaker, form);
        speaker.setPhotoUrl(DEFAULT_SPEAKER_IMAGE);
        speaker = speakerRepository.save(speaker);
        saveSpeakerImage(speaker, form.photo());
        return speaker;
    }

    public EventSpeaker updateSpeaker(Event event, String speakerId, SpeakerForm form) {
        validateSpeakerForm(form);
        EventSpeaker speaker = requireSpeaker(event.getId(), speakerId);
        applySpeakerFields(speaker, form);
        speaker = speakerRepository.save(speaker);
        saveSpeakerImage(speaker, form.photo());
        return speaker;
    }

    public void deleteSpeaker(String eventId, String speakerId) {
        EventSpeaker speaker = requireSpeaker(eventId, speakerId);
        List<EventScheduleItem> linkedItems = scheduleRepository.findBySpeakerId(speakerId);
        for (EventScheduleItem item : linkedItems) {
            if (eventId.equals(item.getEventId())) {
                item.setSpeakerId(null);
            }
        }
        if (!linkedItems.isEmpty()) {
            scheduleRepository.saveAll(linkedItems);
        }
        speakerImageRepository.deleteBySpeakerId(speakerId);
        speakerRepository.delete(speaker);
    }

    public EventSponsor addSponsor(Event event, SponsorForm form) {
        validateSponsorForm(form);
        EventSponsor sponsor = EventSponsor.builder().eventId(event.getId()).build();
        applySponsorFields(sponsor, form);
        sponsor = sponsorRepository.save(sponsor);
        saveSponsorImage(sponsor, form.logo());
        return sponsor;
    }

    public EventSponsor updateSponsor(Event event, String sponsorId, SponsorForm form) {
        validateSponsorForm(form);
        EventSponsor sponsor = requireSponsor(event.getId(), sponsorId);
        applySponsorFields(sponsor, form);
        sponsor = sponsorRepository.save(sponsor);
        saveSponsorImage(sponsor, form.logo());
        return sponsor;
    }

    public void deleteSponsor(String eventId, String sponsorId) {
        EventSponsor sponsor = requireSponsor(eventId, sponsorId);
        sponsorImageRepository.deleteBySponsorId(sponsorId);
        sponsorRepository.delete(sponsor);
    }

    public Map<String, Long> scheduleCounts(List<Event> events) {
        if (events == null || events.isEmpty()) return Map.of();
        Set<String> eventIds = events.stream().map(Event::getId).collect(Collectors.toSet());
        Map<String, Long> counts = emptyCountMap(eventIds);
        for (EventScheduleItem item : scheduleRepository.findByEventIdIn(eventIds)) {
            counts.merge(item.getEventId(), 1L, Long::sum);
        }
        return counts;
    }

    public Map<String, Long> speakerCounts(List<Event> events) {
        if (events == null || events.isEmpty()) return Map.of();
        Set<String> eventIds = events.stream().map(Event::getId).collect(Collectors.toSet());
        Map<String, Long> counts = emptyCountMap(eventIds);
        for (EventSpeaker speaker : speakerRepository.findByEventIdIn(eventIds)) {
            counts.merge(speaker.getEventId(), 1L, Long::sum);
        }
        return counts;
    }

    public Map<String, Long> sponsorCounts(List<Event> events) {
        if (events == null || events.isEmpty()) return Map.of();
        Set<String> eventIds = events.stream().map(Event::getId).collect(Collectors.toSet());
        Map<String, Long> counts = emptyCountMap(eventIds);
        for (EventSponsor sponsor : sponsorRepository.findByEventIdIn(eventIds)) {
            counts.merge(sponsor.getEventId(), 1L, Long::sum);
        }
        return counts;
    }

    public Map<String, EventSpeaker> speakerMap(List<EventSpeaker> speakers) {
        Map<String, EventSpeaker> map = new HashMap<>();
        for (EventSpeaker speaker : speakers) map.put(speaker.getId(), speaker);
        return map;
    }

    public void deleteAllByEvent(String eventId) {
        for (EventSpeaker speaker : findSpeakers(eventId)) {
            speakerImageRepository.deleteBySpeakerId(speaker.getId());
        }
        for (EventSponsor sponsor : findSponsors(eventId)) {
            sponsorImageRepository.deleteBySponsorId(sponsor.getId());
        }
        scheduleRepository.deleteAllByEventId(eventId);
        speakerRepository.deleteAllByEventId(eventId);
        sponsorRepository.deleteAllByEventId(eventId);
    }

    private Map<String, Long> emptyCountMap(Set<String> ids) {
        Map<String, Long> counts = new HashMap<>();
        for (String id : ids) counts.put(id, 0L);
        return counts;
    }

    private void applyScheduleFields(EventScheduleItem item, ScheduleItemForm form, Event event) {
        if (form == null) throw new IllegalArgumentException("Enter schedule information.");
        String title = clean(form.title());
        String location = clean(form.location());
        String description = clean(form.description());

        if (title.length() < 3 || title.length() > 80) {
            throw new IllegalArgumentException("Session title must be between 3 and 80 characters.");
        }
        if (location.length() > 100) throw new IllegalArgumentException("Session location must be 100 characters or fewer.");
        if (description.length() > 400) throw new IllegalArgumentException("Session description must be 400 characters or fewer.");

        LocalTime startTime;
        LocalTime endTime;
        try {
            startTime = LocalTime.parse(clean(form.startTime()));
            endTime = LocalTime.parse(clean(form.endTime()));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Choose a valid session start and end time.");
        }
        if (!endTime.isAfter(startTime)) throw new IllegalArgumentException("Session end time must be after its start time.");

        String speakerId = clean(form.speakerId());
        if (!speakerId.isBlank()) requireSpeaker(event.getId(), speakerId);
        else speakerId = null;

        item.setTitle(title);
        item.setStartTime(startTime);
        item.setEndTime(endTime);
        item.setLocation(location);
        item.setDescription(description);
        item.setSpeakerId(speakerId);
    }

    private void validateSpeakerForm(SpeakerForm form) {
        if (form == null) throw new IllegalArgumentException("Enter speaker information.");
        String name = clean(form.name());
        String designation = clean(form.designation());
        String organization = clean(form.organization());
        String bio = clean(form.bio());

        if (name.length() < 2 || name.length() > 80) throw new IllegalArgumentException("Speaker name must be between 2 and 80 characters.");
        if (designation.length() < 2 || designation.length() > 100) throw new IllegalArgumentException("Speaker designation must be between 2 and 100 characters.");
        if (organization.length() > 120) throw new IllegalArgumentException("Speaker organization must be 120 characters or fewer.");
        if (bio.length() > 700) throw new IllegalArgumentException("Speaker bio must be 700 characters or fewer.");
        validateImage(form.photo(), "Speaker photo");
    }

    private void applySpeakerFields(EventSpeaker speaker, SpeakerForm form) {
        speaker.setName(clean(form.name()));
        speaker.setDesignation(clean(form.designation()));
        speaker.setOrganization(clean(form.organization()));
        speaker.setBio(clean(form.bio()));
    }

    private void validateSponsorForm(SponsorForm form) {
        if (form == null) throw new IllegalArgumentException("Enter sponsor information.");
        String name = clean(form.name());
        String level = clean(form.level()).toUpperCase(Locale.ROOT);
        String website = clean(form.website());
        String description = clean(form.description());

        if (name.length() < 2 || name.length() > 100) throw new IllegalArgumentException("Sponsor name must be between 2 and 100 characters.");
        if (!SPONSOR_LEVELS.contains(level)) throw new IllegalArgumentException("Choose a valid sponsor level.");
        if (description.length() > 400) throw new IllegalArgumentException("Sponsor description must be 400 characters or fewer.");
        if (!website.isBlank()) {
            try {
                URI uri = URI.create(website);
                if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null) {
                    throw new IllegalArgumentException();
                }
            } catch (Exception ex) {
                throw new IllegalArgumentException("Sponsor website must be a valid http or https URL.");
            }
        }
        validateImage(form.logo(), "Sponsor logo");
    }

    private void applySponsorFields(EventSponsor sponsor, SponsorForm form) {
        sponsor.setName(clean(form.name()));
        sponsor.setLevel(clean(form.level()).toUpperCase(Locale.ROOT));
        sponsor.setWebsite(clean(form.website()));
        sponsor.setDescription(clean(form.description()));
        sponsor.setSortOrder(Math.max(0, Math.min(form.sortOrder(), 999)));
    }

    private void validateImage(MultipartFile image, String label) {
        if (image == null || image.isEmpty()) return;
        if (image.getSize() > MAX_IMAGE_SIZE) throw new IllegalArgumentException(label + " must be 2 MB or smaller.");
        String type = image.getContentType() == null ? "" : image.getContentType().toLowerCase(Locale.ROOT);
        if (!IMAGE_TYPES.contains(type)) throw new IllegalArgumentException(label + " must be JPG, PNG or WEBP.");
    }

    private void saveSpeakerImage(EventSpeaker speaker, MultipartFile photo) {
        if (photo == null || photo.isEmpty()) return;
        try {
            String type = photo.getContentType() == null ? "image/jpeg" : photo.getContentType().toLowerCase(Locale.ROOT);
            speakerImageRepository.deleteBySpeakerId(speaker.getId());
            SpeakerImage stored = speakerImageRepository.save(SpeakerImage.builder().speakerId(speaker.getId()).contentType(type).data(photo.getBytes()).build());
            speaker.setPhotoUrl("/speaker-images/" + speaker.getId() + "?v=" + stored.getId());
            speakerRepository.save(speaker);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not save the speaker photo. Try again.");
        }
    }

    private void saveSponsorImage(EventSponsor sponsor, MultipartFile logo) {
        if (logo == null || logo.isEmpty()) return;
        try {
            String type = logo.getContentType() == null ? "image/png" : logo.getContentType().toLowerCase(Locale.ROOT);
            sponsorImageRepository.deleteBySponsorId(sponsor.getId());
            SponsorImage stored = sponsorImageRepository.save(SponsorImage.builder().sponsorId(sponsor.getId()).contentType(type).data(logo.getBytes()).build());
            sponsor.setLogoUrl("/sponsor-images/" + sponsor.getId() + "?v=" + stored.getId());
            sponsorRepository.save(sponsor);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not save the sponsor logo. Try again.");
        }
    }

    private EventScheduleItem requireScheduleItem(String eventId, String itemId) {
        EventScheduleItem item = scheduleRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule item not found."));
        if (!eventId.equals(item.getEventId())) throw new IllegalArgumentException("This schedule item does not belong to the selected event.");
        return item;
    }

    private EventSpeaker requireSpeaker(String eventId, String speakerId) {
        EventSpeaker speaker = speakerRepository.findById(speakerId)
                .orElseThrow(() -> new IllegalArgumentException("Speaker not found."));
        if (!eventId.equals(speaker.getEventId())) throw new IllegalArgumentException("This speaker does not belong to the selected event.");
        return speaker;
    }

    private EventSponsor requireSponsor(String eventId, String sponsorId) {
        EventSponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new IllegalArgumentException("Sponsor not found."));
        if (!eventId.equals(sponsor.getEventId())) throw new IllegalArgumentException("This sponsor does not belong to the selected event.");
        return sponsor;
    }

    private int sponsorLevelRank(String level) {
        if (level == null) return 5;
        return switch (level.toUpperCase(Locale.ROOT)) {
            case "TITLE" -> 0;
            case "GOLD" -> 1;
            case "SILVER" -> 2;
            case "BRONZE" -> 3;
            default -> 4;
        };
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
