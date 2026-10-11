package com.eventflow.repository;

import com.eventflow.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findTop8ByUserIdOrderByCreatedAtDesc(String userId);
    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(String userId);
    List<Notification> findByUserIdAndReadFalse(String userId);
    long countByUserIdAndReadFalse(String userId);
    void deleteAllByUserId(String userId);
}
