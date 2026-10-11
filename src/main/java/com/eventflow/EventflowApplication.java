package com.eventflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class EventflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventflowApplication.class, args);
    }

}
