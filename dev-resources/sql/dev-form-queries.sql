-- name: yesql-set-form-id!
-- Add form
UPDATE forms
SET id = :new_id
WHERE id = :old_id;

-- name: yesql-delete-fixture-application-review!
DELETE FROM application_reviews
WHERE application_key in (SELECT key
                         FROM applications
                         WHERE form_id = :form_id);

-- name: yesql-delete-fixture-application-events!
DELETE FROM application_events
WHERE application_key in (SELECT key
                         FROM applications
                         WHERE form_id = :form_id);

-- name: yesql-delete-fixture-application-secrets!
DELETE FROM application_secrets
WHERE application_key IN (SELECT key FROM applications
                          WHERE form_id = :form_id);

-- name: yesql-delete-fixture-application-answers!
DELETE FROM answers
WHERE application_id IN (SELECT id
                         FROM applications
                         WHERE form_id = :form_id);

-- name: yesql-delete-fixture-application-multi-answers!
DELETE FROM multi_answers
WHERE application_id IN (SELECT id
                         FROM applications
                         WHERE form_id = :form_id);

-- name: yesql-delete-fixture-application-group-answers!
DELETE FROM group_answers
WHERE application_id IN (SELECT id
                         FROM applications
                         WHERE form_id = :form_id);

-- name: yesql-delete-fixture-application!
DELETE FROM applications
WHERE form_id = :form_id;

-- name: yesql-delete-fixture-form!
DELETE FROM forms
WHERE id = :id;

-- name: yesql-delete-fixture-forms-with-key!
DELETE FROM forms
WHERE key = :key;

-- name: yesql-delete-kk-payment-events!
DELETE FROM kk_application_payment_events;

-- name: yesql-delete-kk-payment-states!
DELETE FROM kk_application_payment_states;
