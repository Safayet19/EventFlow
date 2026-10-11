package com.eventflow.dto;

import com.eventflow.model.Event;
import com.eventflow.service.EventRules;
import com.eventflow.view.ViewFormatter;

/** Homepage adapter keeps the approved homepage template and design unchanged. */
public record HomeEventCard(
        String id, String category, String imageUrl, String title,
        String formattedDate, String location, boolean paid
) {
    public static HomeEventCard from(Event event, ViewFormatter display) {
        return new HomeEventCard(event.getId(), event.getCategory(), event.getImageUrl(),
                event.getTitle(), display.eventDate(event), event.getLocation(), EventRules.isPaid(event));
    }
}
