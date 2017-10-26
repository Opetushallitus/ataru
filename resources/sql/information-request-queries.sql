-- name: yesql-add-information-request<!
-- Add new information request in an unprocessed state
INSERT INTO information_request (
  application_key,
  subject,
  message,
  state
) VALUES (
  :application_key,
  :subject,
  :message,
  'unprocessed'
);

-- name: yesql-set-information-request-state!
-- Set new state to information request
UPDATE information_request SET state = :state WHERE id = :id;
