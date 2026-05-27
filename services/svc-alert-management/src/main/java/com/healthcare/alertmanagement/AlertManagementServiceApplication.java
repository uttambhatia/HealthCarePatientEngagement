package com.healthcare.alertmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.healthcare")
public class AlertManagementServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlertManagementServiceApplication.class, args);
    }
}
