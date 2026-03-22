package com.wordfleet.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SessionServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SessionServerApplication.class, args);
    }
}
