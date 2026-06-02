package com.healthcare.appointment.repository;

import com.healthcare.appointment.domain.AppointmentRecord;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class AppointmentJpaRepositoryAdapter implements AppointmentRepository {
    private final JpaAppointmentEntityRepository jpaRepository;

    public AppointmentJpaRepositoryAdapter(JpaAppointmentEntityRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AppointmentRecord save(AppointmentRecord aggregate) {
        AppointmentRecordEntity saved = jpaRepository.save(toEntity(aggregate));
        return toDomain(saved);
    }

    @Override
    public Optional<AppointmentRecord> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<AppointmentRecord> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsBookedSlot(String providerId, String scheduledAt) {
        return jpaRepository.existsByProviderIdAndScheduledAtAndStatus(providerId, scheduledAt, "BOOKED");
    }

    @Override
    public List<AppointmentRecord> findBookedSlotsByProviderAndDate(String providerId, String datePrefix) {
        return jpaRepository
                .findByProviderIdAndScheduledAtStartingWithAndStatus(providerId, datePrefix, "BOOKED")
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    private AppointmentRecordEntity toEntity(AppointmentRecord aggregate) {
        return new AppointmentRecordEntity(
                aggregate.id(),
                aggregate.status(),
                aggregate.patientId(),
                aggregate.providerId(),
                aggregate.scheduledAt(),
                aggregate.channel()
        );
    }

    private AppointmentRecord toDomain(AppointmentRecordEntity entity) {
        return new AppointmentRecord(
                entity.getId(),
                entity.getStatus(),
                entity.getPatientId(),
                entity.getProviderId(),
                entity.getScheduledAt(),
                entity.getChannel()
        );
    }
}