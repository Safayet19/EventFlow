package com.eventflow.model;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "event_speakers")
public class EventSpeaker {

    @Id
    private String id;

    @Indexed
    private String eventId;

    private String name;
    private String designation;
    private String organization;
    private String bio;
    @Builder.Default
    private String photoUrl = "/images/default-avatar.png";
}
