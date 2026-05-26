package com.healthcare.appointment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.healthcare")
public class AppointmentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppointmentServiceApplication.class, args);
    }
}
