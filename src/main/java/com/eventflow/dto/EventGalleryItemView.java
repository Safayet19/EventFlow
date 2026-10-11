package com.eventflow.dto;

import java.time.LocalDateTime;

/** Immutable gallery metadata; large image bytes are deliberately not fetched. */
public record EventGalleryItemView(
        String id,
        String eventId,
        String caption,
        int sortOrder,
        LocalDateTime uploadedAt,
        String imageUrl
) {}
