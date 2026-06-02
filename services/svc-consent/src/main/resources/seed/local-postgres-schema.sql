CREATE TABLE IF NOT EXISTS consent_records (
    id VARCHAR(255) PRIMARY KEY,
    status VARCHAR(255) NOT NULL,
    patient_id VARCHAR(255) NOT NULL,
    consent_type VARCHAR(255) NOT NULL,
    granted BOOLEAN NOT NULL,
    version INTEGER NOT NULL,
    effective_from VARCHAR(255) NOT NULL,
    CONSTRAINT uk_consent_patient_type_version UNIQUE (patient_id, consent_type, version)
);
