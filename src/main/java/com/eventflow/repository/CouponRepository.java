package com.eventflow.repository;

import com.eventflow.model.Coupon;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends MongoRepository<Coupon, String> {
    List<Coupon> findAllByOrderByCreatedAtDesc();
    List<Coupon> findByOrganizerIdOrderByCreatedAtDesc(String organizerId);
    List<Coupon> findByEventIdOrderByCreatedAtDesc(String eventId);
    Optional<Coupon> findByEventIdAndCodeIgnoreCase(String eventId, String code);
    void deleteAllByEventId(String eventId);
}
