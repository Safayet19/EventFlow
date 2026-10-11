package com.eventflow.controller;

import com.eventflow.model.ProfileImage;
import com.eventflow.service.UserService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.concurrent.TimeUnit;

@Controller
@RequiredArgsConstructor
public class ProfileImageController {

    private final UserService userService;



    @GetMapping("/profile-images/{userId}")
    public ResponseEntity<byte[]> image(@PathVariable String userId) {
        ProfileImage image = userService.findProfileImage(userId).orElse(null);
        if (image == null || image.getData() == null) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(image.getContentType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic());
        headers.setContentLength(image.getData().length);
        return new ResponseEntity<>(image.getData(), headers, HttpStatus.OK);
    }
}
