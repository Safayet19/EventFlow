package com.eventflow.dto;

/** Immutable form data; Spring MVC uses constructor binding. */
public record TicketTypeForm(
        String name,
        String description,
        String price,
        int capacity,
        boolean active,
        int sortOrder
) {}
