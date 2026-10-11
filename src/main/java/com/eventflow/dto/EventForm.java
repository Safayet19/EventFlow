package com.eventflow.dto;

import org.springframework.web.multipart.MultipartFile;

/** Immutable form data; Spring MVC uses constructor binding. */
public record EventForm(
        String title,
        String category,
        String location,
        String date,
        String time,
        String description,
        int capacity,
        String pricingType,
        String basePrice,
        MultipartFile image
) {}
