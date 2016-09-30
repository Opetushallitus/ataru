-- name: yesql-add-application-query<!
-- Add application
insert into applications (form_id, key, content, lang, preferred_name, last_name) values (:form_id, :key, :content, :lang, :preferred_name, :last_name);

-- name: yesql-get-application-list
select a.id,
  a.key, a.lang,
  a.preferred_name || ' ' ||  a.last_name as applicant_name,
  a.modified_time, coalesce(ar.state, 'received') as state
from applications a
left outer join application_reviews ar on a.id = ar.application_id
join forms f on f.id = a.form_id and f.key = :form_key
order by a.modified_time desc;

-- name: yesql-get-application-events
select event_type, time from application_events where application_id = :application_id;

-- name: yesql-get-application-review
select id, modified_time, state, notes from application_reviews where application_id = :application_id;

-- name: yesql-application-query-by-modified
select a.id, a.key, a.lang, a.form_id as form, a.modified_time, a.content from applications a
join forms f on f.id = a.form_id and f.key = :form_key
order by a.modified_time desc;

-- name: yesql-get-application-by-id
select id, key, lang, form_id as form, modified_time, content from applications where id = :application_id;

-- name: yesql-add-application-event!
-- Add application event
insert into application_events (application_id, event_type) values (:application_id, :event_type);

-- name: yesql-add-application-review!
-- Add application review
insert into application_reviews (application_id, state) values (:application_id, :state);

-- name: yesql-save-application-review!
-- Save modifications for existing review record
update application_reviews set notes = :notes, modified_time = now() where id = :id;

-- name: yesql-add-person-oid!
-- Add person OID to an application
update applications set person_oid = :person_oid where id = :id;
