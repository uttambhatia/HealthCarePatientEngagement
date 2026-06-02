package com.healthcare.careplan.service;

import com.healthcare.careplan.dto.CreateCarePlanRequest;
import com.healthcare.careplan.dto.CarePlanResponse;
import com.healthcare.careplan.dto.CarePlanResponsibilityResponse;
import com.healthcare.careplan.dto.UpdateCarePlanRequest;

import java.util.List;

public interface CarePlanApplicationService {
    CarePlanResponse createCarePlan(CreateCarePlanRequest request, String correlationId);
    CarePlanResponse updateCarePlan(String id, UpdateCarePlanRequest request, String correlationId);
    CarePlanResponse getCarePlan(String id);
    List<CarePlanResponse> listCarePlans();
    CarePlanResponsibilityResponse getCarePlanResponsibility(String patientId);
}
