package com.eventflow.repository;

import com.eventflow.model.ContactRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ContactRequestRepository extends MongoRepository<ContactRequest, String> {
    List<ContactRequest> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
}
