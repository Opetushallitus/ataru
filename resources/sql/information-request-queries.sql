-- name: yesql-add-information-request<!
-- Add new information request in an unprocessed state
INSERT INTO information_requests (
  application_key,
  subject,
  message,
  virkailija_oid,
  message_type,
  recipient_target,
  send_reminder_time
) VALUES (
  :application_key,
  :subject,
  :message,
  :virkailija_oid,
  :message_type,
  :recipient_target,
  :send_reminder_time
);

-- name: yesql-get-information-requests
-- Get all information requests belonging to an application
SELECT
  ir.application_key,
  ir.subject,
  ir.message,
  ir.created_time,
  ir.message_type,
  v.first_name,
  v.last_name
FROM information_requests ir
LEFT JOIN virkailija v ON ir.virkailija_oid = v.oid
WHERE application_key = :application_key;

-- name: yesql-get-information-requests-to-remind
-- Get all information requests with unsent reminders
SELECT
    ir.id,
    ir.application_key,
    ir.subject,
    ir.message,
    ir.created_time,
    ir.message_type,
    ir.recipient_target,
    a.created_time AS application_updated_time
FROM information_requests ir
LEFT JOIN latest_applications a ON ir.application_key = a.key
WHERE ir.reminder_processed_time IS NULL
AND ir.send_reminder_time <= now();

-- name: yesql-set-information-request-reminder-processed-time-by-id!
-- Set reminder-processed-time to now
UPDATE information_requests
SET reminder_processed_time = now()
WHERE id = :id;
