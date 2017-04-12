-- name: yesql-set-form-id!
-- Add form
UPDATE forms
SET id = :new_id
WHERE id = :old_id;

-- name: yesql-delete-fixture-application-review!
DELETE FROM application_reviews
WHERE application_key = (SELECT key
                         FROM applications
                         WHERE form_id = :form_id);

-- name: yesql-delete-fixture-application-events!
DELETE FROM application_events
WHERE application_key = (SELECT key
                         FROM applications
                         WHERE form_id = :form_id);

-- name: yesql-delete-fixture-application!
DELETE FROM applications
WHERE form_id = :form_id;

-- name: yesql-delete-fixture-form!
DELETE FROM forms
WHERE id = :id;
