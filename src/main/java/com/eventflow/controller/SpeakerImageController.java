package com.eventflow.controller;

import com.eventflow.model.SpeakerImage;
import com.eventflow.service.EventProgramService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.concurrent.TimeUnit;

@Controller
@RequiredArgsConstructor
public class SpeakerImageController {

    private final EventProgramService eventProgramService;



    @GetMapping("/speaker-images/{speakerId}")
    public ResponseEntity<byte[]> speakerImage(@PathVariable String speakerId) {
        SpeakerImage image = eventProgramService.findSpeakerImage(speakerId).orElse(null);
        if (image == null || image.getData() == null) {
            return ResponseEntity.notFound().build();
        }
        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(image.getContentType());
        } catch (Exception ex) {
            contentType = MediaType.IMAGE_JPEG;
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .contentType(contentType)
                .body(image.getData());
    }
}
