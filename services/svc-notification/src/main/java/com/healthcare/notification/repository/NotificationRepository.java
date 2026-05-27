package com.healthcare.notification.repository;

import com.healthcare.notification.domain.NotificationJob;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    NotificationJob save(NotificationJob aggregate);
    Optional<NotificationJob> findById(String id);
    List<NotificationJob> findAll();
    void deleteById(String id);
}
