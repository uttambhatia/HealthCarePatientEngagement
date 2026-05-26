package com.healthcare.medicalrecord.service;

import com.healthcare.medicalrecord.dto.CreateMedicalRecordRequest;
import com.healthcare.medicalrecord.dto.MedicalRecordResponse;

import java.util.List;

public interface MedicalRecordApplicationService {
    MedicalRecordResponse syncMedicalRecord(CreateMedicalRecordRequest request, String correlationId);
    MedicalRecordResponse getMedicalRecord(String id);
    List<MedicalRecordResponse> listMedicalRecords();
}
