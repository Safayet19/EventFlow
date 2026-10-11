package com.eventflow.model;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "event_schedule")
public class EventScheduleItem {

    @Id
    private String id;

    @Indexed
    private String eventId;

    private String title;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private String description;

    @Indexed
    private String speakerId;

}
