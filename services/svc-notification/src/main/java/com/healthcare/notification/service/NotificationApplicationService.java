package com.healthcare.notification.service;

import com.healthcare.notification.dto.CreateNotificationRequest;
import com.healthcare.notification.dto.NotificationResponse;

import java.util.List;

public interface NotificationApplicationService {
    NotificationResponse sendNotification(CreateNotificationRequest request, String correlationId);
    NotificationResponse getNotification(String id);
    List<NotificationResponse> listNotifications();
}
