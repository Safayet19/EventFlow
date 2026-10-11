package com.eventflow.controller;

import com.eventflow.model.EventImage;
import com.eventflow.service.EventService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.concurrent.TimeUnit;

@Controller
@RequiredArgsConstructor
public class EventImageController {

    private final EventService eventService;



    @GetMapping("/event-images/{eventId}")
    @ResponseBody
    public ResponseEntity<byte[]> image(@PathVariable String eventId) {
        EventImage image = eventService.findImage(eventId).orElse(null);
        if (image == null || image.getData() == null) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(image.getContentType());
        } catch (Exception ex) {
            mediaType = MediaType.IMAGE_JPEG;
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePrivate())
                .contentType(mediaType)
                .body(image.getData());
    }
}
