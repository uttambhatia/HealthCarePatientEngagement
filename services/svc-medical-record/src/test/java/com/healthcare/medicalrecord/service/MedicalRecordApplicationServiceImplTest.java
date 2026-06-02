package com.healthcare.medicalrecord.service;

import com.healthcare.medicalrecord.dto.CreateMedicalRecordRequest;
import com.healthcare.medicalrecord.dto.UpdateMedicalRecordRequest;
import com.healthcare.medicalrecord.exception.FhirValidationException;
import com.healthcare.medicalrecord.exception.VersionConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class MedicalRecordApplicationServiceImplTest {

    @Autowired
    private MedicalRecordApplicationService service;

    @Test
    void shouldCreateAndUpdateMedicalRecordWithVersioning() {
        var created = service.syncMedicalRecord(
                new CreateMedicalRecordRequest(
                        "pat-mr-1001",
                        "Observation",
                        "Observation/obs-1001",
                        "HbA1c measured at 7.2"
                ),
                "corr-mr-100"
        );

        assertThat(created.id()).isNotBlank();
        assertThat(created.version()).isEqualTo(1);
        assertThat(created.fhirResourceType()).isEqualTo("Observation");

        var updated = service.updateMedicalRecord(
                created.id(),
                new UpdateMedicalRecordRequest(
                        "Condition",
                        "Condition/cond-1001",
                        "Diagnosed with controlled hypertension",
                        created.version()
                ),
                "corr-mr-101"
        );

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.fhirResourceType()).isEqualTo("Condition");
        assertThat(updated.resourceReference()).isEqualTo("Condition/cond-1001");
        assertThat(updated.version()).isEqualTo(2);
    }

    @Test
    void shouldRejectUnsupportedFhirResourceType() {
        assertThatThrownBy(() -> service.syncMedicalRecord(
                new CreateMedicalRecordRequest(
                        "pat-mr-2001",
                        "CustomResource",
                        "Custom/custom-1",
                        "Custom data"
                ),
                "corr-mr-200"
        )).isInstanceOf(FhirValidationException.class);
    }

    @Test
    void shouldRejectUpdateWhenVersionMismatches() {
        var created = service.syncMedicalRecord(
                new CreateMedicalRecordRequest(
                        "pat-mr-3001",
                        "Observation",
                        "Observation/obs-3001",
                        "Vitals stable"
                ),
                "corr-mr-300"
        );

        assertThatThrownBy(() -> service.updateMedicalRecord(
                created.id(),
                new UpdateMedicalRecordRequest(
                        "Observation",
                        "Observation/obs-3002",
                        "Vitals updated",
                        created.version() + 1
                ),
                "corr-mr-301"
        )).isInstanceOf(VersionConflictException.class);
    }
}
