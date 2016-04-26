-- name: get-forms-query
-- Get all stored forms
select id, name, modified_time from forms;

-- name: add-form-query<!
-- Add form
insert into forms (name) values (:name);

-- name: form-exists-query
-- Get single form
select id from forms where id = CAST(:id AS uuid);

-- name: get-by-id
select * from forms where id = CAST(:id AS uuid);

-- name: update-form-query!
-- Update form
UPDATE forms SET name = :name WHERE id = CAST(:id AS uuid);
