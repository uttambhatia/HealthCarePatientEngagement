CREATE TABLE IF NOT EXISTS clinical_alerts (
    id VARCHAR(255) PRIMARY KEY,
    status VARCHAR(255) NOT NULL,
    patient_id VARCHAR(255) NOT NULL,
    device_id VARCHAR(255),
    severity VARCHAR(255) NOT NULL,
    trigger_type VARCHAR(255) NOT NULL,
    summary VARCHAR(1024) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_clinical_alerts_patient_id ON clinical_alerts (patient_id);
CREATE INDEX IF NOT EXISTS idx_clinical_alerts_device_id ON clinical_alerts (device_id);

CREATE TABLE IF NOT EXISTS device_patient_mapping (
    device_id VARCHAR(255) PRIMARY KEY,
    patient_id VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_device_patient_mapping_patient_id ON device_patient_mapping (patient_id);
