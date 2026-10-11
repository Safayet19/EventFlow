package com.eventflow.repository;

import com.eventflow.model.EventSponsor;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface EventSponsorRepository extends MongoRepository<EventSponsor, String> {
    List<EventSponsor> findByEventId(String eventId);
    List<EventSponsor> findByEventIdIn(Collection<String> eventIds);
    void deleteAllByEventId(String eventId);
}
