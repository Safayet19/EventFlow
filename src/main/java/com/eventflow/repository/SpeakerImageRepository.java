package com.eventflow.repository;

import com.eventflow.model.SpeakerImage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SpeakerImageRepository extends MongoRepository<SpeakerImage, String> {
    Optional<SpeakerImage> findBySpeakerId(String speakerId);
    void deleteBySpeakerId(String speakerId);
}
