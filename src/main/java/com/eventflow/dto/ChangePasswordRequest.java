package com.eventflow.dto;

/** Immutable form data; Spring MVC uses constructor binding. */
public record ChangePasswordRequest(
        String currentPassword,
        String newPassword,
        String confirmPassword
) {}
