package com.eventflow.dto;

/** Immutable form data; Spring MVC uses constructor binding. */
public record ForgotPasswordRequest(
        String email,
        String newPassword,
        String confirmPassword
) {}
