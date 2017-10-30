-- name: yesql-add-information-request<!
-- Add new information request in an unprocessed state
INSERT INTO information_requests (
  application_key,
  subject,
  message,
  virkailija_oid
) VALUES (
  :application_key,
  :subject,
  :message,
  :virkailija_oid
);

-- name: yesql-get-information-requests
-- Get all information requests belonging to an application
SELECT
  ir.application_key,
  ir.subject,
  ir.message,
  ir.created_time,
  v.first_name,
  v.last_name
FROM information_requests ir
LEFT JOIN virkailija v ON ir.virkailija_oid = v.oid
WHERE application_key = :application_key;
