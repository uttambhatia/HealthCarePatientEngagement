package com.healthcare.careplan.service;

import com.healthcare.careplan.dto.CreateCarePlanRequest;
import com.healthcare.careplan.dto.UpdateCarePlanRequest;
import com.healthcare.careplan.exception.ProtocolValidationException;
import com.healthcare.careplan.exception.ResourceNotFoundException;
import com.healthcare.careplan.exception.VersionConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CarePlanApplicationServiceImplTest {

    @Autowired
    private CarePlanApplicationService service;

    @Test
    void shouldCreateCarePlanWithTasksAndVersion() {
        var created = service.createCarePlan(
                new CreateCarePlanRequest(
                        "pat-cp-1001",
                        "Improve medication adherence",
                        "ACTIVE",
                        "coord-77",
                        List.of("Schedule weekly follow-up", "Review medication checklist")
                ),
                "corr-cp-100"
        );

        assertThat(created.id()).isNotBlank();
        assertThat(created.status()).isEqualTo("MANAGED");
        assertThat(created.planStatus()).isEqualTo("ACTIVE");
        assertThat(created.tasks()).containsExactly("Schedule weekly follow-up", "Review medication checklist");
        assertThat(created.version()).isEqualTo(1);
    }

    @Test
    void shouldUpdateCarePlanAndIncreaseVersion() {
        var created = service.createCarePlan(
                new CreateCarePlanRequest(
                        "pat-cp-2001",
                        "Improve glucose control",
                        "DRAFT",
                        "coord-88",
                        List.of("Track blood glucose daily")
                ),
                "corr-cp-200"
        );

        var updated = service.updateCarePlan(
                created.id(),
                new UpdateCarePlanRequest(
                        "Improve glucose control and exercise",
                        "ACTIVE",
                        "coord-88",
                        List.of("Track blood glucose daily", "Walk 30 minutes"),
                        created.version()
                ),
                "corr-cp-201"
        );

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.goal()).isEqualTo("Improve glucose control and exercise");
        assertThat(updated.planStatus()).isEqualTo("ACTIVE");
        assertThat(updated.tasks()).containsExactly("Track blood glucose daily", "Walk 30 minutes");
        assertThat(updated.version()).isEqualTo(2);
    }

    @Test
    void shouldRejectInvalidPlanStatusByProtocol() {
        assertThatThrownBy(() -> service.createCarePlan(
                new CreateCarePlanRequest(
                        "pat-cp-3001",
                        "General wellness",
                        "UNKNOWN_STATUS",
                        "coord-91",
                        List.of("Perform routine check-ins")
                ),
                "corr-cp-300"
        )).isInstanceOf(ProtocolValidationException.class);
    }

    @Test
    void shouldRejectUpdateWhenVersionMismatches() {
        var created = service.createCarePlan(
                new CreateCarePlanRequest(
                        "pat-cp-4001",
                        "Hypertension monitoring",
                        "ACTIVE",
                        "coord-99",
                        List.of("Monitor blood pressure twice daily")
                ),
                "corr-cp-400"
        );

        assertThatThrownBy(() -> service.updateCarePlan(
                created.id(),
                new UpdateCarePlanRequest(
                        "Hypertension monitoring",
                        "ON_HOLD",
                        "coord-99",
                        List.of("Monitor blood pressure twice daily"),
                        created.version() + 2
                ),
                "corr-cp-401"
        )).isInstanceOf(VersionConflictException.class);
    }

    @Test
    void shouldReturnLatestResponsibilityForPatient() {
        var created = service.createCarePlan(
                new CreateCarePlanRequest(
                        "pat-cp-5001",
                        "Cardiac recovery support",
                        "DRAFT",
                        "coord-11",
                        List.of("Track resting heart rate")
                ),
                "corr-cp-500"
        );

        service.updateCarePlan(
                created.id(),
                new UpdateCarePlanRequest(
                        "Cardiac recovery support",
                        "ACTIVE",
                        "coord-22",
                        List.of("Track resting heart rate", "Weekly tele-follow-up"),
                        created.version()
                ),
                "corr-cp-501"
        );

        var responsibility = service.getCarePlanResponsibility("pat-cp-5001");

        assertThat(responsibility.patientId()).isEqualTo("pat-cp-5001");
        assertThat(responsibility.ownerId()).isEqualTo("coord-22");
        assertThat(responsibility.planStatus()).isEqualTo("ACTIVE");
        assertThat(responsibility.version()).isEqualTo(2);
    }

    @Test
    void shouldFailResponsibilityLookupForUnknownPatient() {
        assertThatThrownBy(() -> service.getCarePlanResponsibility("pat-cp-missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
