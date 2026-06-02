INSERT INTO notification_jobs (id, status, recipient_id, channel, template_id, message, delivery_attempts, last_error)
VALUES
  ('not-seed-1001', 'SENT', 'pat-1001', 'SMS', 'appointment-booked-v1', 'Your appointment is confirmed for 2026-06-15 09:30 UTC.', 1, NULL),
  ('not-seed-1002', 'PENDING', 'pat-1002', 'EMAIL', 'followup-reminder-v1', 'Please schedule your follow-up appointment.', 0, NULL)
ON CONFLICT (id) DO NOTHING;
