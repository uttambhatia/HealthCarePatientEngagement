CREATE TABLE IF NOT EXISTS appointment_records (
    id VARCHAR(255) PRIMARY KEY,
    status VARCHAR(255) NOT NULL,
    patient_id VARCHAR(255) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    scheduled_at VARCHAR(255) NOT NULL,
    channel VARCHAR(255) NOT NULL,
    CONSTRAINT uk_appointment_provider_schedule UNIQUE (provider_id, scheduled_at)
);

CREATE TABLE IF NOT EXISTS teleconsultation_sessions (
    id VARCHAR(255) PRIMARY KEY,
    appointment_id VARCHAR(255) NOT NULL,
    patient_id VARCHAR(255) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    doctor_join_url VARCHAR(255) NOT NULL,
    patient_join_url VARCHAR(255) NOT NULL,
    started_at VARCHAR(255) NOT NULL,
    joined_at VARCHAR(255),
    completed_at VARCHAR(255),
    consultation_notes VARCHAR(4000),
    follow_up_required BOOLEAN NOT NULL,
    next_follow_up_date VARCHAR(255),
    CONSTRAINT uk_teleconsult_appointment UNIQUE (appointment_id)
);

CREATE TABLE IF NOT EXISTS teleconsultation_interaction_logs (
    session_id VARCHAR(255) NOT NULL,
    log_order INTEGER NOT NULL,
    log_entry VARCHAR(500) NOT NULL,
    PRIMARY KEY (session_id, log_order),
    CONSTRAINT fk_teleconsult_logs_session FOREIGN KEY (session_id)
        REFERENCES teleconsultation_sessions (id) ON DELETE CASCADE
);
