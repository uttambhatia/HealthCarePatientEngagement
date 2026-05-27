package com.healthcare.careplan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.healthcare")
public class CarePlanServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CarePlanServiceApplication.class, args);
    }
}
