-- name: yesql-delete-all-application_events!
-- Removes ALL application_events
delete from application_events;

-- name: yesql-delete-all-applications!
-- Removes ALL applications
delete from applications;

-- name: yesql-delete-all-forms!
-- Removes ALL forms
delete from forms;

-- name: yesql-add-form-with-id-query<!
-- Add form
insert into forms (id, name, content, modified_by) values (:id, :name, :content, :modified_by);
