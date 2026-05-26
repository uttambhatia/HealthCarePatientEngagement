package com.healthcare.eventmessaging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.healthcare")
public class EventMessagingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventMessagingServiceApplication.class, args);
    }
}
