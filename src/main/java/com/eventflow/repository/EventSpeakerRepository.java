package com.eventflow.repository;

import com.eventflow.model.EventSpeaker;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface EventSpeakerRepository extends MongoRepository<EventSpeaker, String> {
    List<EventSpeaker> findByEventIdOrderByNameAsc(String eventId);
    long countByEventId(String eventId);
    List<EventSpeaker> findByEventIdIn(Collection<String> eventIds);
    void deleteAllByEventId(String eventId);
}
