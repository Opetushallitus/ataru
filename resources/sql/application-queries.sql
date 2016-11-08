-- name: yesql-add-application-query<!
-- Add application
insert into applications
(form_id, key, content, lang, preferred_name, last_name, hakukohde, hakukohde_name, secret)
values
(:form_id, :key, :content, :lang, :preferred_name, :last_name, :hakukohde, :hakukohde_name, :secret);

-- name: yesql-get-application-list
select a.id,
  a.key, a.lang,
  a.preferred_name || ' ' ||  a.last_name as applicant_name,
  a.created_time, coalesce(ar.state, 'received') as state
from applications a
left outer join application_reviews ar on a.id = ar.application_id
join forms f on f.id = a.form_id and f.key = :form_key
order by a.created_time desc;

-- name: yesql-get-application-list-by-hakukohde
select a.id,
  a.key, a.lang,
  a.preferred_name || ' ' ||  a.last_name as applicant_name,
  a.modified_time, coalesce(ar.state, 'received') as state
from applications a
  left outer join application_reviews ar on a.id = ar.application_id
  join forms f on f.id = a.form_id and f.key = :form_key
where a.hakukohde = :hakukohde_oid
order by a.modified_time desc;

-- name: yesql-get-application-events
select event_type, time, new_review_state, application_key, id from application_events
where application_key = :application_key order by time asc;

-- name: yesql-get-application-review
select id, application_id, modified_time, state, notes, application_key from application_reviews where application_key = :application_key;

-- name: yesql-application-query-by-modified
select a.id, a.key, a.lang, a.form_id as form, a.created_time, a.content from applications a
join forms f on f.id = a.form_id and f.key = :form_key;

-- name: yesql-get-application-by-id
select id, key, lang, form_id as form, created_time, content from applications where id = :application_id;

-- name: yesql-get-latest-application-by-key
with latest_version as (
    select max(created_time) as latest_time from applications a where a.key = :application_key
)
select id, key, lang, form_id as form, created_time, content from applications a join latest_version lv on a.created_time = lv.latest_time;

-- name: yesql-get-latest-application-by-secret
with latest_version as (
    select max(created_time) as latest_time from applications a where a.secret = :secret
)
select id, key, lang, form_id as form, created_time, content from applications a join latest_version lv on a.created_time = lv.latest_time;

-- name: yesql-get-latest-version-by-secret-lock-for-update
with latest_version as (
    select max(created_time) as latest_time from applications a where a.secret = :secret
)
select id, key, lang, form_id as form, created_time, content from applications a join latest_version lv on a.created_time = lv.latest_time for update;

-- name: yesql-get-application-organization-by-key
-- Get the related form's organization oid for access checks

with latest_version as (
    select max(created_time) as latest_time from applications a where a.key = :application_key
)
select f.organization_oid from applications a
join latest_version lv on a.created_time = lv.latest_time
join forms f on f.id = a.form_id;

-- name: yesql-get-application-review-organization-by-id
-- Get the related form's organization oid for access checks

select f.organization_oid
from application_reviews ar
join applications a on a.id = ar.application_id
join forms f on f.id = a.form_id
and ar.id = :review_id;

-- name: yesql-add-application-event!
-- Add application event
insert into application_events (application_id, application_key, event_type, new_review_state) values (:application_id, :application_key, :event_type, :new_review_state);

-- name: yesql-add-application-review!
-- Add application review
insert into application_reviews (application_id, application_key, state) values (:application_id, :application_key, :state);

-- name: yesql-save-application-review!
-- Save modifications for existing review record
update application_reviews set notes = :notes, modified_time = now(), state = :state
where application_id = :application_id;

-- name: yesql-add-person-oid!
-- Add person OID to an application
update applications set person_oid = :person_oid where id = :id;

-- name: yesql-get-hakukohteet-from-applications
-- Get application info from applications whose forms are still active
select distinct a.hakukohde, a.hakukohde_name, f.key as form_key
from applications a
join forms f on a.form_id = f.id
where f.deleted is not true and hakukohde is not null and hakukohde_name is not null;
