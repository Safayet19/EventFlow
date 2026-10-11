package com.eventflow.controller;

import com.eventflow.model.EventGalleryImage;
import com.eventflow.service.EventGalleryService;
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
public class EventGalleryImageController {

    private final EventGalleryService galleryService;



    @GetMapping("/event-gallery-images/{imageId}")
    public ResponseEntity<byte[]> image(@PathVariable String imageId) {
        EventGalleryImage image = galleryService.findImage(imageId);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(image.getContentType());
        } catch (Exception ex) {
            mediaType = MediaType.IMAGE_JPEG;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(image.getData());
    }
}
