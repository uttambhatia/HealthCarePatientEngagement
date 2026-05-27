package com.healthcare.careplan.service;

        import com.healthcare.careplan.domain.CarePlanAggregate;
        import com.healthcare.careplan.dto.CreateCarePlanRequest;
        import com.healthcare.careplan.dto.CarePlanResponse;
        import com.healthcare.careplan.event.CarePlanCreatedEvent;
        import com.healthcare.careplan.exception.ResourceNotFoundException;
        import com.healthcare.careplan.integration.CarePlanFhirAdapter;
        import com.healthcare.careplan.repository.CarePlanRepository;
        import com.healthcare.platform.common.messaging.MessagingPort;
        import org.springframework.stereotype.Service;

        import java.util.List;
        import java.util.UUID;

        @Service
        public class CarePlanApplicationServiceImpl implements CarePlanApplicationService {
            private final CarePlanRepository repository;
            private final MessagingPort messagingPort;
            private final CarePlanFhirAdapter integration;

            public CarePlanApplicationServiceImpl(CarePlanRepository repository, MessagingPort messagingPort, CarePlanFhirAdapter integration) {
                this.repository = repository;
                this.messagingPort = messagingPort;
                this.integration = integration;
            }

            @Override
            public CarePlanResponse createCarePlan(CreateCarePlanRequest request, String correlationId) {
                CarePlanAggregate aggregate = repository.save(new CarePlanAggregate(
                        UUID.randomUUID().toString(),
                        "DRAFT",
                        request.patientId(),
                request.goal(),
                request.planStatus(),
                request.ownerId()
                ));
                integration.synchronizeCarePlan(aggregate, correlationId);
                messagingPort.publish("careplan-service", correlationId, new CarePlanCreatedEvent(
                        aggregate.id(),
                        aggregate.patientId(),
                        aggregate.goal(),
                        aggregate.planStatus(),
                        aggregate.ownerId()
                ));
                return map(aggregate);
            }

            @Override
            public CarePlanResponse getCarePlan(String id) {
                return repository.findById(id).map(this::map)
                        .orElseThrow(() -> new ResourceNotFoundException("CarePlan record not found: " + id));
            }

            @Override
            public List<CarePlanResponse> listCarePlans() {
                return repository.findAll().stream().map(this::map).toList();
            }


public CarePlanResponse createManagedCarePlan(CreateCarePlanRequest request, String correlationId) {
    return createCarePlan(request, correlationId);
}
            private CarePlanResponse map(CarePlanAggregate aggregate) {
                return new CarePlanResponse(
                        aggregate.id(),
                        aggregate.status(),
                        aggregate.patientId(),
                aggregate.goal(),
                aggregate.planStatus(),
                aggregate.ownerId()
                );
            }
        }
