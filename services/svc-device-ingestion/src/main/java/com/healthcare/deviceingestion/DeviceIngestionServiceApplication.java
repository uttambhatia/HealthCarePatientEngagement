package com.healthcare.deviceingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.healthcare")
public class DeviceIngestionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeviceIngestionServiceApplication.class, args);
    }
}
