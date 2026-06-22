package com.healthcare.careplan.event;

import com.healthcare.careplan.dto.CreateCarePlanRequest;
import com.healthcare.careplan.dto.UpdateCarePlanRequest;
import com.healthcare.careplan.exception.ResourceNotFoundException;
import com.healthcare.careplan.repository.CarePlanRepository;
import com.healthcare.careplan.service.CarePlanApplicationService;
import com.healthcare.platform.common.event.MessageEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TeleconsultationCompletedEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeleconsultationCompletedEventConsumer.class);

    private final CarePlanApplicationService carePlanService;
    private final CarePlanRepository carePlanRepository;

    public TeleconsultationCompletedEventConsumer(
            CarePlanApplicationService carePlanService,
            CarePlanRepository carePlanRepository) {
        this.carePlanService = carePlanService;
        this.carePlanRepository = carePlanRepository;
    }

    public void handle(MessageEnvelope<TeleconsultationCompletedEvent> envelope) {
        TeleconsultationCompletedEvent event = envelope.payload();
        LOGGER.info("Consumed TeleconsultationCompletedEvent correlationId={} appointmentId={} patientId={} followUpRequired={}",
                envelope.correlationId(), event.appointmentId(), event.patientId(), event.followUpRequired());

        try {
            carePlanRepository.findLatestByPatientId(event.patientId()).ifPresentOrElse(
                    existingPlan -> {
                        // Update existing care plan: add follow-up tasks
                        List<String> updatedTasks = new ArrayList<>(existingPlan.tasks());
                        updatedTasks.add("FOLLOW_UP | Review teleconsultation notes from appointment " + event.appointmentId());
                        if (event.followUpRequired() && event.nextFollowUpDate() != null && !event.nextFollowUpDate().isBlank()) {
                            updatedTasks.add("FOLLOW_UP_APPOINTMENT | Book follow-up appointment for " + event.nextFollowUpDate());
                        }

                        carePlanService.updateCarePlan(
                                existingPlan.id(),
                                new UpdateCarePlanRequest(
                                        existingPlan.goal(),
                                        existingPlan.planStatus(),
                                        existingPlan.ownerId(),
                                        updatedTasks,
                                        existingPlan.version()
                                ),
                                envelope.correlationId()
                        );
                        LOGGER.info("Updated care plan id={} for patient={} after teleconsultation appointmentId={}",
                                existingPlan.id(), event.patientId(), event.appointmentId());
                    },
                    () -> {
                        // No existing plan — create one for post-teleconsult follow-up
                        List<String> tasks = new ArrayList<>();
                        tasks.add("FOLLOW_UP | Review teleconsultation notes from appointment " + event.appointmentId());
                        if (event.followUpRequired() && event.nextFollowUpDate() != null && !event.nextFollowUpDate().isBlank()) {
                            tasks.add("FOLLOW_UP_APPOINTMENT | Book follow-up appointment for " + event.nextFollowUpDate());
                        }

                        carePlanService.createCarePlan(
                                new CreateCarePlanRequest(
                                        event.patientId(),
                                        "Post-teleconsultation follow-up care",
                                        "ACTIVE",
                                        event.providerId(),
                                        tasks
                                ),
                                envelope.correlationId()
                        );
                        LOGGER.info("Created new care plan for patient={} after teleconsultation appointmentId={}",
                                event.patientId(), event.appointmentId());
                    }
            );
        } catch (Exception ex) {
            LOGGER.error("Failed to update/create care plan after teleconsultation appointmentId={} patientId={} correlationId={} error={}",
                    event.appointmentId(), event.patientId(), envelope.correlationId(), ex.getMessage(), ex);
        }
    }
}
