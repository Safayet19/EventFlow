package com.eventflow.repository;

import com.eventflow.model.EventImage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface EventImageRepository extends MongoRepository<EventImage, String> {
    Optional<EventImage> findByEventId(String eventId);
    void deleteByEventId(String eventId);
}
