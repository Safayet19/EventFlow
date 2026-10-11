package com.eventflow.repository;

import com.eventflow.model.Feedback;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends MongoRepository<Feedback, String> {
    Optional<Feedback> findByEventIdAndParticipantId(String eventId, String participantId);
    List<Feedback> findByParticipantIdOrderByUpdatedAtDesc(String participantId);
    List<Feedback> findByEventIdIn(Collection<String> eventIds);
    List<Feedback> findAllByOrderByUpdatedAtDesc();
    void deleteByEventIdAndParticipantId(String eventId, String participantId);
    void deleteAllByEventId(String eventId);
    void deleteAllByParticipantId(String participantId);
}
