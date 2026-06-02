package com.healthcare.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaNotificationEntityRepository extends JpaRepository<NotificationJobEntity, String> {
}