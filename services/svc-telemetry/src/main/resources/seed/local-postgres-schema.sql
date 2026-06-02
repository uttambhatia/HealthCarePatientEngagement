CREATE TABLE IF NOT EXISTS telemetry_readings (
    id VARCHAR(255) PRIMARY KEY,
    status VARCHAR(255) NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    metric_type VARCHAR(255) NOT NULL,
    metric_value VARCHAR(255) NOT NULL,
    recorded_at VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_telemetry_readings_device_id ON telemetry_readings (device_id);
CREATE INDEX IF NOT EXISTS idx_telemetry_readings_metric_type ON telemetry_readings (metric_type);
CREATE INDEX IF NOT EXISTS idx_telemetry_readings_recorded_at ON telemetry_readings (recorded_at);

CREATE TABLE IF NOT EXISTS device_patient_mapping (
    device_id VARCHAR(255) PRIMARY KEY,
    patient_id VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_device_patient_mapping_patient_id ON device_patient_mapping (patient_id);
