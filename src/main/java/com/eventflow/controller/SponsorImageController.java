package com.eventflow.controller;

import com.eventflow.model.SponsorImage;
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
public class SponsorImageController {

    private final EventProgramService eventProgramService;



    @GetMapping("/sponsor-images/{sponsorId}")
    public ResponseEntity<byte[]> sponsorImage(@PathVariable String sponsorId) {
        SponsorImage image = eventProgramService.findSponsorImage(sponsorId).orElse(null);
        if (image == null || image.getData() == null) {
            return ResponseEntity.notFound().build();
        }
        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(image.getContentType());
        } catch (Exception ex) {
            contentType = MediaType.IMAGE_PNG;
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .contentType(contentType)
                .body(image.getData());
    }
}
