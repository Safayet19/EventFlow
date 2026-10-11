package com.eventflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Immutable validated form data; Spring MVC uses constructor binding. */
public record RegisterRequest(
        @NotBlank(message = "Full name is required.")
        String fullName,
        @NotBlank(message = "Email is required.")
        @Email(message = "Enter a valid email address.")
        String email,
        @NotBlank(message = "Password is required.")
        @Size(min = 8, message = "Password must have at least 8 characters.")
        String password,
        @NotBlank(message = "Confirm your password.")
        String confirmPassword,
        @NotBlank(message = "Choose Participant or Organizer.")
        String role
) {}
