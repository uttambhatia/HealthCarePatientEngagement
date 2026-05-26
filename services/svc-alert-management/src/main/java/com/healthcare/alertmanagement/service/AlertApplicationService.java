package com.healthcare.alertmanagement.service;

import com.healthcare.alertmanagement.dto.CreateAlertRequest;
import com.healthcare.alertmanagement.dto.AlertResponse;

import java.util.List;

public interface AlertApplicationService {
    AlertResponse triggerAlert(CreateAlertRequest request, String correlationId);
    AlertResponse getAlert(String id);
    List<AlertResponse> listAlerts();
}
