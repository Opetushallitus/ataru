-- name: yesql-add-information-request<!
-- Add new information request in an unprocessed state
INSERT INTO information_request (
  application_key,
  subject,
  message
) VALUES (
  :application_key,
  :subject,
  :message
);

-- name: yesql-get-information-requests
-- Get all information requests belonging to an application
SELECT application_key, subject, message, created_time FROM information_request WHERE application_key = :application_key;
