package com.eventflow.service;

import com.eventflow.dto.EventGalleryItemView;
import com.eventflow.model.EventGalleryImage;
import com.eventflow.repository.EventGalleryImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EventGalleryService {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024;
    private static final int MAX_IMAGES_PER_EVENT = 10;

    private final EventGalleryImageRepository repository;



    public List<EventGalleryItemView> findByEvent(String eventId) {
        return repository.findByEventIdOrderBySortOrderAscUploadedAtAsc(eventId).stream()
                .map(image -> new EventGalleryItemView(image.getId(), image.getEventId(), image.getCaption(),
                        image.getSortOrder(), image.getUploadedAt(),
                        image.getId() == null ? "" : "/event-gallery-images/" + image.getId()))
                .toList();
    }

    public long countByEvent(String eventId) {
        return repository.countByEventId(eventId);
    }

    public EventGalleryImage findImage(String imageId) {
        return repository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Gallery image not found."));
    }

    public EventGalleryImage add(String eventId, MultipartFile image, String caption, int sortOrder) {
        if (repository.countByEventId(eventId) >= MAX_IMAGES_PER_EVENT) {
            throw new IllegalArgumentException("An event can contain up to 10 gallery images.");
        }
        validateImage(image);
        String cleanCaption = cleanCaption(caption);
        try {
            String contentType = image.getContentType() == null ? "image/jpeg" : image.getContentType().toLowerCase(Locale.ROOT);
            return repository.save(EventGalleryImage.builder().eventId(eventId).contentType(contentType).data(image.getBytes())
                    .caption(cleanCaption).sortOrder(Math.max(0, Math.min(sortOrder, 999))).build());
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not save the gallery image. Try again.");
        }
    }

    public EventGalleryImage update(String eventId, String imageId, String caption, int sortOrder) {
        EventGalleryImage image = requireForEvent(eventId, imageId);
        image.setCaption(cleanCaption(caption));
        image.setSortOrder(Math.max(0, Math.min(sortOrder, 999)));
        return repository.save(image);
    }

    public void delete(String eventId, String imageId) {
        repository.delete(requireForEvent(eventId, imageId));
    }

    public void deleteAllByEvent(String eventId) {
        repository.deleteAllByEventId(eventId);
    }

    private EventGalleryImage requireForEvent(String eventId, String imageId) {
        EventGalleryImage image = repository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Gallery image not found."));
        if (!eventId.equals(image.getEventId())) {
            throw new IllegalArgumentException("This gallery image does not belong to the selected event.");
        }
        return image;
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) throw new IllegalArgumentException("Choose an image to upload.");
        if (image.getSize() > MAX_IMAGE_SIZE) throw new IllegalArgumentException("Gallery image must be 2 MB or smaller.");
        String type = image.getContentType() == null ? "" : image.getContentType().toLowerCase(Locale.ROOT);
        if (!IMAGE_TYPES.contains(type)) throw new IllegalArgumentException("Gallery image must be JPG, PNG or WEBP.");
    }

    private String cleanCaption(String caption) {
        String value = caption == null ? "" : caption.trim();
        if (value.length() > 160) throw new IllegalArgumentException("Gallery caption must be 160 characters or fewer.");
        return value;
    }
}
