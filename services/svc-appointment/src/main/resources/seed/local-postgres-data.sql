INSERT INTO appointment_records (id, status, patient_id, provider_id, scheduled_at, channel)
VALUES
  ('apt-seed-1001', 'BOOKED', 'pat-1001', 'prov-44', '2026-06-15T09:30:00Z', 'VIDEO'),
  ('apt-seed-1002', 'BOOKED', 'pat-1002', 'prov-66', '2026-06-16T10:00:00Z', 'IN_PERSON')
ON CONFLICT (id) DO NOTHING;

INSERT INTO teleconsultation_sessions (
  id, appointment_id, patient_id, provider_id, status,
  doctor_join_url, patient_join_url, started_at, joined_at, completed_at,
  consultation_notes, follow_up_required, next_follow_up_date
)
VALUES (
  'tcs-seed-2001',
  'apt-seed-1001',
  'pat-1001',
  'prov-44',
  'COMPLETED',
  'https://teleconsult.healthcare.local/session/tcs-seed-2001?role=DOCTOR',
  'https://teleconsult.healthcare.local/session/tcs-seed-2001?role=PATIENT',
  '2026-06-01T10:00:00Z',
  '2026-06-01T10:05:00Z',
  '2026-06-01T10:25:00Z',
  'Seeded completed teleconsultation for local postgres verification.',
  TRUE,
  '2026-06-08T10:30:00Z'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO teleconsultation_interaction_logs (session_id, log_order, log_entry)
VALUES
  ('tcs-seed-2001', 0, 'Teleconsultation initiated'),
  ('tcs-seed-2001', 1, 'Patient joined'),
  ('tcs-seed-2001', 2, 'Consultation completed')
ON CONFLICT DO NOTHING;
