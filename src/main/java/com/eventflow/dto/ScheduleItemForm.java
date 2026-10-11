package com.eventflow.dto;

/** Immutable form data; Spring MVC uses constructor binding. */
public record ScheduleItemForm(
        String title,
        String startTime,
        String endTime,
        String location,
        String description,
        String speakerId
) {}
