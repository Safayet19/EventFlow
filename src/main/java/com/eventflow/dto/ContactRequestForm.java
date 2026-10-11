package com.eventflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Immutable validated form data; Spring MVC uses constructor binding. */
public record ContactRequestForm(
        @NotBlank(message = "Enter your full name.")
        String fullName,
        @NotBlank(message = "Enter your email address.")
        @Email(message = "Enter a valid email address.")
        String email,
        String phone,
        String requestType,
        @NotBlank(message = "Enter a subject.")
        String subject,
        @NotBlank(message = "Enter a message.")
        String message
) {}
