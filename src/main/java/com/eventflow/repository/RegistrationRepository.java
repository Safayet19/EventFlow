package com.eventflow.repository;

import com.eventflow.model.Registration;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends MongoRepository<Registration, String> {
    boolean existsByEventIdAndParticipantId(String eventId, String participantId);
    long countByEventId(String eventId);
    long countByParticipantId(String participantId);
    long countByTicketTypeId(String ticketTypeId);
    long countByCouponId(String couponId);
    Optional<Registration> findByEventIdAndParticipantId(String eventId, String participantId);
    List<Registration> findByParticipantIdOrderByRegisteredAtDesc(String participantId);
    List<Registration> findByEventIdOrderByRegisteredAtAsc(String eventId);
    List<Registration> findAllByOrderByRegisteredAtDesc();
    List<Registration> findByEventIdInOrderByRegisteredAtDesc(Collection<String> eventIds);
    void deleteAllByEventId(String eventId);
    void deleteAllByParticipantId(String participantId);
}
