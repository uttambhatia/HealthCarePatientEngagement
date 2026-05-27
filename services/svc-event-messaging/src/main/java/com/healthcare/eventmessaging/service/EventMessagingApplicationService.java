package com.healthcare.eventmessaging.service;

import com.healthcare.eventmessaging.dto.CreateServiceBusMessageRequest;
import com.healthcare.eventmessaging.dto.ServiceBusMessageResponse;

import java.util.List;

public interface EventMessagingApplicationService {
    ServiceBusMessageResponse queueMessage(CreateServiceBusMessageRequest request, String correlationId);
    ServiceBusMessageResponse getMessage(String id);
    List<ServiceBusMessageResponse> listMessages();
}
