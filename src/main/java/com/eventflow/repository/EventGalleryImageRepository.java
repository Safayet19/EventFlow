package com.eventflow.repository;

import com.eventflow.model.EventGalleryImage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface EventGalleryImageRepository extends MongoRepository<EventGalleryImage, String> {
    @Query(fields = "{'data': 0, 'contentType': 0}")
    List<EventGalleryImage> findByEventIdOrderBySortOrderAscUploadedAtAsc(String eventId);
    long countByEventId(String eventId);
    void deleteAllByEventId(String eventId);
}
