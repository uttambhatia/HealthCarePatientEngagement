package com.healthcare.appointment.service;

        import com.healthcare.appointment.domain.AppointmentRecord;
        import com.healthcare.appointment.dto.CreateAppointmentRequest;
        import com.healthcare.appointment.dto.AppointmentResponse;
        import com.healthcare.appointment.event.AppointmentBookedEvent;
        import com.healthcare.appointment.exception.ResourceNotFoundException;
        import com.healthcare.appointment.integration.AppointmentNotificationAdapter;
        import com.healthcare.appointment.repository.AppointmentRepository;
        import com.healthcare.platform.common.messaging.MessagingPort;
        import org.springframework.stereotype.Service;

        import java.util.List;
        import java.util.UUID;

        @Service
        public class AppointmentApplicationServiceImpl implements AppointmentApplicationService {
            private final AppointmentRepository repository;
            private final MessagingPort messagingPort;
            private final AppointmentNotificationAdapter integration;

            public AppointmentApplicationServiceImpl(AppointmentRepository repository, MessagingPort messagingPort, AppointmentNotificationAdapter integration) {
                this.repository = repository;
                this.messagingPort = messagingPort;
                this.integration = integration;
            }

            @Override
            public AppointmentResponse bookAppointment(CreateAppointmentRequest request, String correlationId) {
                AppointmentRecord aggregate = repository.save(new AppointmentRecord(
                        UUID.randomUUID().toString(),
                        "BOOKED",
                        request.patientId(),
                request.providerId(),
                request.scheduledAt(),
                request.channel()
                ));
                integration.sendBookingNotification(aggregate, correlationId);
                messagingPort.publish("appointment-service", correlationId, new AppointmentBookedEvent(
                        aggregate.id(),
                        aggregate.patientId(),
                        aggregate.providerId(),
                        aggregate.scheduledAt(),
                        aggregate.channel()
                ));
                return map(aggregate);
            }

            @Override
            public AppointmentResponse getAppointment(String id) {
                return repository.findById(id).map(this::map)
                        .orElseThrow(() -> new ResourceNotFoundException("Appointment record not found: " + id));
            }

            @Override
            public List<AppointmentResponse> listAppointments() {
                return repository.findAll().stream().map(this::map).toList();
            }


public AppointmentResponse bookWithNotification(CreateAppointmentRequest request, String correlationId) {
    AppointmentResponse response = bookAppointment(request, correlationId);
    return response;
}
            private AppointmentResponse map(AppointmentRecord aggregate) {
                return new AppointmentResponse(
                        aggregate.id(),
                        aggregate.status(),
                        aggregate.patientId(),
                aggregate.providerId(),
                aggregate.scheduledAt(),
                aggregate.channel()
                );
            }
        }
