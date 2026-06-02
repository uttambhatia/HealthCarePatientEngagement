INSERT INTO consent_records (id, status, patient_id, consent_type, granted, version, effective_from)
VALUES
  ('con-seed-1001', 'RECORDED', 'pat-1001', 'GENERAL_CARE', TRUE, 1, '2026-05-31T10:00:00Z'),
  ('con-seed-1002', 'RECORDED', 'pat-1002', 'GENERAL_CARE', TRUE, 1, '2026-05-31T11:00:00Z')
ON CONFLICT (id) DO NOTHING;
