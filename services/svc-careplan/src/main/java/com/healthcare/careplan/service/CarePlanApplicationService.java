package com.healthcare.careplan.service;

import com.healthcare.careplan.dto.CreateCarePlanRequest;
import com.healthcare.careplan.dto.CarePlanResponse;

import java.util.List;

public interface CarePlanApplicationService {
    CarePlanResponse createCarePlan(CreateCarePlanRequest request, String correlationId);
    CarePlanResponse getCarePlan(String id);
    List<CarePlanResponse> listCarePlans();
}
