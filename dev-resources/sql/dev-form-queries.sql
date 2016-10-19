-- name: yesql-set-form-id!
-- Add form
update forms set id = :new_id where id = :old_id;

-- name: yesql-delete-fixture-application-review!
delete from application_reviews where application_id = (select id from applications where form_id = :form_id);

-- name: yesql-delete-fixture-application-events!
delete from application_events where application_id = (select id from applications where form_id = :form_id);


-- name: yesql-delete-fixture-application!
delete from applications where form_id = :form_id;

-- name: yesql-delete-fixture-form!
delete from forms where id = :id;
