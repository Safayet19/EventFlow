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
@Document(collection = "event_sponsors")
public class EventSponsor {

    @Id
    private String id;

    @Indexed
    private String eventId;

    private String name;
    private String level;
    private String website;
    private String description;
    private int sortOrder;
    private String logoUrl;

}
