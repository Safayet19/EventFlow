package com.eventflow.dto;

/** Immutable form data; Spring MVC uses constructor binding. */
public record ProfileUpdateRequest(
        String fullName
) {}
