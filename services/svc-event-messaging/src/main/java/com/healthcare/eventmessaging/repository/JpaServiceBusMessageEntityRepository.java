package com.healthcare.eventmessaging.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaServiceBusMessageEntityRepository extends JpaRepository<ServiceBusMessageEntity, String> {
}
