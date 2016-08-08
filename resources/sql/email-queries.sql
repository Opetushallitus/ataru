-- name: yesql-add-application-confirmation-email<!
insert into application_confirmation_emails (application_id, recipient) values (:application_id, :recipient);

-- name: yesql-increment-delivery-attempt-count
update application_confirmation_emails set delivery_attempts = delivery_attempts + 1 where id = :confirmation_id;

-- name: yesql-increment-delivery-attempt-count-and-mark-delivered
update application_confirmation_emails set delivered_at = now(), delivery_attempts = delivery_attempts + 1 where id = :confirmation_id;


