package com.healthcare.identityadapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.healthcare")
public class IdentityAdapterServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdentityAdapterServiceApplication.class, args);
    }
}
