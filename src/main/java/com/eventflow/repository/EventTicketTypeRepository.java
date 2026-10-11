package com.eventflow.repository;

import com.eventflow.model.EventTicketType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface EventTicketTypeRepository extends MongoRepository<EventTicketType, String> {
    List<EventTicketType> findByEventIdOrderBySortOrderAscNameAsc(String eventId);
    List<EventTicketType> findByEventIdAndActiveTrueOrderBySortOrderAscNameAsc(String eventId);
    List<EventTicketType> findByEventIdIn(Collection<String> eventIds);
    void deleteAllByEventId(String eventId);
}
