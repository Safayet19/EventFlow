package com.eventflow.dto;

import org.springframework.web.multipart.MultipartFile;

/** Immutable form data; Spring MVC uses constructor binding. */
public record SpeakerForm(
        String name,
        String designation,
        String organization,
        String bio,
        MultipartFile photo
) {}
