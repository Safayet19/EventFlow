package com.eventflow.repository;

import com.eventflow.model.EventScheduleItem;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface EventScheduleRepository extends MongoRepository<EventScheduleItem, String> {
    List<EventScheduleItem> findByEventIdOrderByStartTimeAsc(String eventId);
    List<EventScheduleItem> findBySpeakerId(String speakerId);
    long countByEventId(String eventId);
    List<EventScheduleItem> findByEventIdIn(Collection<String> eventIds);
    void deleteAllByEventId(String eventId);
}
