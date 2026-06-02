INSERT INTO device_patient_mapping (device_id, patient_id)
SELECT 'dev-seed-1001', 'pat-seed-1001'
WHERE NOT EXISTS (
    SELECT 1 FROM device_patient_mapping WHERE device_id = 'dev-seed-1001'
);

INSERT INTO device_patient_mapping (device_id, patient_id)
SELECT 'dev-seed-1002', 'pat-seed-1001'
WHERE NOT EXISTS (
    SELECT 1 FROM device_patient_mapping WHERE device_id = 'dev-seed-1002'
);

INSERT INTO device_patient_mapping (device_id, patient_id)
SELECT 'dev-seed-2001', 'pat-seed-2001'
WHERE NOT EXISTS (
    SELECT 1 FROM device_patient_mapping WHERE device_id = 'dev-seed-2001'
);

INSERT INTO telemetry_readings (id, status, device_id, metric_type, metric_value, recorded_at)
SELECT 'tel-seed-1001', 'RECORDED', 'dev-seed-1001', 'HEART_RATE', '74', '2026-01-12T08:00:00Z'
WHERE NOT EXISTS (
    SELECT 1 FROM telemetry_readings WHERE id = 'tel-seed-1001'
);

INSERT INTO telemetry_readings (id, status, device_id, metric_type, metric_value, recorded_at)
SELECT 'tel-seed-1002', 'RECORDED', 'dev-seed-1001', 'SPO2', '98', '2026-01-12T08:05:00Z'
WHERE NOT EXISTS (
    SELECT 1 FROM telemetry_readings WHERE id = 'tel-seed-1002'
);

INSERT INTO telemetry_readings (id, status, device_id, metric_type, metric_value, recorded_at)
SELECT 'tel-seed-1003', 'RECORDED', 'dev-seed-1002', 'GLUCOSE', '112', '2026-01-12T09:15:00Z'
WHERE NOT EXISTS (
    SELECT 1 FROM telemetry_readings WHERE id = 'tel-seed-1003'
);

INSERT INTO telemetry_readings (id, status, device_id, metric_type, metric_value, recorded_at)
SELECT 'tel-seed-1004', 'RECORDED', 'dev-seed-1002', 'HEART_RATE', '79', '2026-01-12T10:00:00Z'
WHERE NOT EXISTS (
    SELECT 1 FROM telemetry_readings WHERE id = 'tel-seed-1004'
);

INSERT INTO telemetry_readings (id, status, device_id, metric_type, metric_value, recorded_at)
SELECT 'tel-seed-2001', 'RECORDED', 'dev-seed-2001', 'HEART_RATE', '71', '2026-01-12T07:45:00Z'
WHERE NOT EXISTS (
    SELECT 1 FROM telemetry_readings WHERE id = 'tel-seed-2001'
);
