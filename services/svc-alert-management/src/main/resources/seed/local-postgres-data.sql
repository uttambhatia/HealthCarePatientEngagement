INSERT INTO device_patient_mapping (device_id, patient_id)
SELECT 'dev-seed-1001', 'pat-seed-1001'
WHERE NOT EXISTS (
    SELECT 1 FROM device_patient_mapping WHERE device_id = 'dev-seed-1001'
);

INSERT INTO device_patient_mapping (device_id, patient_id)
SELECT 'dev-seed-1002', 'pat-seed-1002'
WHERE NOT EXISTS (
    SELECT 1 FROM device_patient_mapping WHERE device_id = 'dev-seed-1002'
);
