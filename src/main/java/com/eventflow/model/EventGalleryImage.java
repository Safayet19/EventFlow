package com.eventflow.model;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "event_gallery_images")
public class EventGalleryImage {

    @Id
    private String id;

    @Indexed
    private String eventId;

    private String contentType;
    private byte[] data;
    private String caption;
    private int sortOrder;
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();


}
