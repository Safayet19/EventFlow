package com.eventflow.repository;

import com.eventflow.model.Event;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EventRepository extends MongoRepository<Event, String> {
    List<Event> findTop3ByFeaturedTrueOrderByDateAsc();
    List<Event> findAllByOrderByDateAsc();
    List<Event> findByOrganizerIdOrderByDateAsc(String organizerId);
    long countByOrganizerId(String organizerId);
}
