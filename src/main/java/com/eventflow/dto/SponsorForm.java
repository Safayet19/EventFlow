package com.eventflow.dto;

import org.springframework.web.multipart.MultipartFile;

/** Immutable form data; Spring MVC uses constructor binding. */
public record SponsorForm(
        String name,
        String level,
        String website,
        String description,
        int sortOrder,
        MultipartFile logo
) {}
