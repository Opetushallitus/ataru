-- name: yesql-get-all-applications
-- Used by migration version 1.24, should be removed when it is run on production database
select id,key from applications;

-- name: yesql-get-application-events-by-application-id
-- Used by migration version 1.24, should be removed when it is run on production database
select id,application_key from application_events where application_id = :application_id;

-- name: yesql-set-application-key-to-application-events!
-- Used by migration version 1.24, should be removed when it is run on production database
update application_events set application_key = :application_key where id = :id;

-- name: yesql-get-application-confirmation-emails
-- Used by migration version 1.24, should be removed when it is run on production database
select id,application_key from application_confirmation_emails where application_id = :application_id;

-- name: yesql-set-application-key-to-application-confirmation-emails!
-- Used by migration version 1.24, should be removed when it is run on production database
update application_confirmation_emails set application_key = :application_key where id = :id;

-- name: yesql-set-application-key-to-application-review!
-- Used by migration version 1.24, should be removed when it is run on production database
update application_reviews set application_key = :application_key where id = :id;

-- name: yesql-get-application-review-by-application-id
-- Used by migration version 1.24, should be removed when it is run on production database
select id,application_key from application_reviews where application_id = :application_id;
