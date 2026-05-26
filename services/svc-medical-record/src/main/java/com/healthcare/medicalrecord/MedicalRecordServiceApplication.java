package com.healthcare.medicalrecord;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.healthcare")
public class MedicalRecordServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MedicalRecordServiceApplication.class, args);
    }
}
