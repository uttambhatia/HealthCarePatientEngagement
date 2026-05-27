package com.healthcare.patient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.healthcare")
public class PatientServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PatientServiceApplication.class, args);
    }
}
