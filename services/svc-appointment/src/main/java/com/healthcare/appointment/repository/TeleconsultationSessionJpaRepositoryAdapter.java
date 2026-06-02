package com.healthcare.appointment.repository;

import com.healthcare.appointment.domain.TeleconsultationSession;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Primary
public class TeleconsultationSessionJpaRepositoryAdapter implements TeleconsultationSessionRepository {
    private final JpaTeleconsultationSessionEntityRepository jpaRepository;

    public TeleconsultationSessionJpaRepositoryAdapter(JpaTeleconsultationSessionEntityRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TeleconsultationSession save(TeleconsultationSession session) {
        return toDomain(jpaRepository.save(toEntity(session)));
    }

    @Override
    public Optional<TeleconsultationSession> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<TeleconsultationSession> findByAppointmentId(String appointmentId) {
        return jpaRepository.findByAppointmentId(appointmentId).map(this::toDomain);
    }

    private TeleconsultationSessionEntity toEntity(TeleconsultationSession session) {
        return new TeleconsultationSessionEntity(
                session.id(),
                session.appointmentId(),
                session.patientId(),
                session.providerId(),
                session.status(),
                session.doctorJoinUrl(),
                session.patientJoinUrl(),
                session.startedAt(),
                session.joinedAt(),
                session.completedAt(),
                session.consultationNotes(),
                session.followUpRequired(),
                session.nextFollowUpDate(),
                session.interactionLogs()
        );
    }

    private TeleconsultationSession toDomain(TeleconsultationSessionEntity entity) {
        return new TeleconsultationSession(
                entity.getId(),
                entity.getAppointmentId(),
                entity.getPatientId(),
                entity.getProviderId(),
                entity.getStatus(),
                entity.getDoctorJoinUrl(),
                entity.getPatientJoinUrl(),
                entity.getStartedAt(),
                entity.getJoinedAt(),
                entity.getCompletedAt(),
                entity.getConsultationNotes(),
                entity.isFollowUpRequired(),
                entity.getNextFollowUpDate(),
                entity.getInteractionLogs()
        );
    }
}
