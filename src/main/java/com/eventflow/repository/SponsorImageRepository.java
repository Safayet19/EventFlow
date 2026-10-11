package com.eventflow.repository;

import com.eventflow.model.SponsorImage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SponsorImageRepository extends MongoRepository<SponsorImage, String> {
    Optional<SponsorImage> findBySponsorId(String sponsorId);
    void deleteBySponsorId(String sponsorId);
}
