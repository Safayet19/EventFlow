package com.eventflow.model;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "feedback")
@CompoundIndexes({
        @CompoundIndex(name = "event_participant_feedback_unique", def = "{'eventId': 1, 'participantId': 1}", unique = true)
})
public class Feedback {

    @Id
    private String id;

    @Indexed
    private String eventId;

    @Indexed
    private String participantId;

    private String participantName;
    private String participantEmail;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
