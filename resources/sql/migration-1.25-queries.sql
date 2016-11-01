-- name: yesql-get-all-applications
select id,key from applications;

-- name: yesql-get-application-events-by-application-id
select id,application_key from application_events where application_id = :application_id;

-- name: yesql-set-application-key-to-application-events!
update application_events set application_key = :application_key where id = :id;

-- name: yesql-get-application-confirmation-emails
select id,application_key from application_confirmation_emails where application_id = :application_id;

-- name: yesql-set-application-key-to-application-confirmation-emails!
update application_confirmation_emails set application_key = :application_key where id = :id;

-- name: yesql-set-application-key-to-application-review!
update application_reviews set application_key = :application_key where id = :id;

-- name: yesql-get-application-review-by-application-id
select id,application_key from application_reviews where application_id = :application_id;
