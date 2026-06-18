package com.healthcare.patient.service;

import com.healthcare.patient.dto.CreatePatientRequest;
import com.healthcare.patient.dto.PatientResponse;

import java.util.List;

public interface PatientApplicationService {
    PatientResponse registerPatient(CreatePatientRequest request, String correlationId);
    PatientResponse approveRegistration(String id, String actor, String correlationId);
    PatientResponse rejectRegistration(String id, String actor, String correlationId);
    PatientResponse resendRegistrationNotification(String id, String correlationId);
    PatientResponse getPatient(String id);
    List<PatientResponse> listPatients();
}
