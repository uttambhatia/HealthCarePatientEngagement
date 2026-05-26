package com.healthcare.telemetry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.healthcare")
public class TelemetryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TelemetryServiceApplication.class, args);
    }
}
