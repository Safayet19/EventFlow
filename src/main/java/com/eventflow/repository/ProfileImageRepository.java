package com.eventflow.repository;

import com.eventflow.model.ProfileImage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ProfileImageRepository extends MongoRepository<ProfileImage, String> {
    Optional<ProfileImage> findByUserId(String userId);
    void deleteByUserId(String userId);
}
