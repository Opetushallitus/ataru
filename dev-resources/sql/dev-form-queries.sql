-- name: yesql-delete-all-application_events!
-- Removes ALL application_events
delete from application_events;

-- name: yesql-delete-all-applications!
-- Removes ALL applications
delete from applications;

-- name: yesql-delete-all-forms!
-- Removes ALL forms
delete from forms;

-- name: yesql-set-form-id!
-- Add form
update forms set id = :new_id where id = :old_id
