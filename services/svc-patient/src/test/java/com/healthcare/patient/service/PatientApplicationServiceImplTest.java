package com.healthcare.patient.service;

import com.healthcare.patient.dto.CreatePatientRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PatientApplicationServiceImplTest {
    @Autowired
    private PatientApplicationService service;

    @Test
    void shouldRegisterAndFetchPatient() {
        var created = service.registerPatient(new CreatePatientRequest("EXT-100", "Ava", "Jones", "1985-04-12"), "corr-123");
        assertThat(created.id()).isNotBlank();
        assertThat(service.getPatient(created.id()).externalReference()).isEqualTo("EXT-100");
    }
}
