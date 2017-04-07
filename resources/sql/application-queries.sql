-- name: yesql-add-application-query<!
-- Add application
insert into applications
(form_id, key, content, lang, preferred_name, last_name, hakukohde, haku, secret)
values
(:form_id, :key, :content, :lang, :preferred_name, :last_name, :hakukohde, :haku, :secret);

-- name: yesql-get-application-list-by-form
select a.id,
  a.key,
  a.lang,
  a.preferred_name || ' ' ||  a.last_name as applicant_name,
  a.created_time,
  ar.state as state,
  ar.score as score
from applications a
  join application_reviews ar on a.key = ar.application_key
  join forms f on f.id = a.form_id and f.key = :form_key
where a.hakukohde is null
order by a.created_time desc;

-- name: yesql-get-application-list-by-hakukohde
select a.id,
  a.key,
  a.lang,
  a.preferred_name || ' ' ||  a.last_name as applicant_name,
  a.created_time,
  ar.state as state,
  ar.score as score,
  a.form_id as form
from applications a
join application_reviews ar on a.key = ar.application_key
join forms f on a.form_id = f.id
where a.hakukohde = :hakukohde_oid
and (:query_type = 'ALL' OR f.organization_oid in (:authorized_organization_oids))
order by a.created_time desc;

-- name: yesql-get-application-list-by-haku
select a.id,
  a.key,
  a.lang,
  a.preferred_name || ' ' ||  a.last_name as applicant_name,
  a.created_time,
  ar.state as state,
  ar.score as score,
  a.form_id as form
from applications a
join application_reviews ar on a.key = ar.application_key
join forms f on a.form_id = f.id
where a.haku = :haku_oid
and (:query_type = 'ALL' OR f.organization_oid in (:authorized_organization_oids))
order by a.created_time desc;

-- name: yesql-get-application-events
select event_type, time, new_review_state, application_key, id from application_events
where application_key = :application_key order by time asc;

-- name: yesql-get-application-review
select id, modified_time, state, notes, score, application_key
from application_reviews
where application_key = :application_key;

-- name: yesql-get-applications-for-form
-- Gets applications only for forms (omits hakukohde applications)
select a.id, a.key, a.lang, a.form_id as form, a.created_time, a.content, ar.state as state
from applications a
join forms f on f.id = a.form_id and f.key = :form_key
join application_reviews ar on a.key = ar.application_key
where a.hakukohde is null and state in (:filtered_states);

-- name: yesql-get-applications-for-hakukohde
-- Get applications for form-key/hakukohde
select
  a.id,
  a.key,
  a.lang,
  a.form_id AS form,
  a.created_time,
  a.content,
  a.hakukohde,
  ar.state as state,
  f.key as form_key
from applications a
join application_reviews ar on a.key = ar.application_key
join forms f on a.form_id = f.id
where state in (:filtered_states)
and a.hakukohde = :hakukohde_oid;

-- name: yesql-get-applications-for-haku
-- Get applications for form-key/haku
select
  a.id,
  a.key,
  a.lang,
  a.form_id AS form,
  a.created_time,
  a.content,
  a.hakukohde,
  a.haku,
  ar.state as state,
  f.key as form_key
from applications a
join application_reviews ar on a.key = ar.application_key
join forms f on a.form_id = f.id
where state in (:filtered_states) and a.haku = :haku_oid;

-- name: yesql-get-application-by-id
select id, key, lang, form_id as form, created_time, content, secret from applications where id = :application_id;

-- name: yesql-get-latest-application-by-key
with latest_version as (
    select max(created_time) as latest_time from applications a where a.key = :application_key
)
select id, key, lang, form_id as form, created_time, content, hakukohde from applications a join latest_version lv on a.created_time = lv.latest_time;

-- name: yesql-get-latest-application-by-secret
with latest_version as (
    select max(created_time) as latest_time from applications a where a.secret = :secret
)
select a.id, a.key, a.lang, a.form_id as form, a.created_time, a.content, a.hakukohde, f.key as form_key
from applications a
join latest_version lv on a.created_time = lv.latest_time
join forms f on a.form_id = f.id;

-- name: yesql-get-latest-version-by-secret-lock-for-update
with latest_version as (
    select max(created_time) as latest_time from applications a where a.secret = :secret
)
select id, key, lang, form_id as form, created_time, content, haku, hakukohde
from applications a join latest_version lv on a.created_time = lv.latest_time for update;

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
join applications a on a.key = ar.application_key
join forms f on f.id = a.form_id
and ar.id = :review_id;

-- name: yesql-add-application-event!
-- Add application event
insert into application_events (application_key, event_type, new_review_state)
values (:application_key, :event_type, :new_review_state);

-- name: yesql-add-application-review!
-- Add application review, initially it doesn't have all fields. This is just a "skeleton"
insert into application_reviews (application_key, state) values (:application_key, :state);

-- name: yesql-save-application-review!
-- Save modifications for existing review record
update application_reviews
set
notes = :notes,
score = :score,
modified_time = now(),
state = :state
where application_key = :application_key;

-- name: yesql-add-person-oid!
-- Add person OID to an application
update applications set person_oid = :person_oid where id = :id;

-- name: yesql-get-haut-and-hakukohteet-from-applications
WITH latest_applications AS (
    SELECT
    a.key,
    a.haku,
    a.hakukohde,
    ar.state,
    max(a.created_time) AS latest_time
    FROM applications a
      INNER JOIN forms f ON (a.form_id = f.id)
      INNER JOIN application_reviews ar on a.key = ar.application_key
    WHERE a.haku IS NOT NULL AND a.hakukohde IS NOT NULL
    AND (:query_type = 'ALL' OR f.organization_oid IN (:authorized_organization_oids))
    GROUP BY a.key, a.haku, a.hakukohde, ar.state
)
SELECT
  la.haku,
  la.hakukohde,
  COUNT (la.key) AS application_count,
  SUM (CASE WHEN la.state = 'unprocessed' THEN 1 ELSE 0 END) as unprocessed,
  SUM (CASE WHEN la.state in (:incomplete_states) THEN 1 ELSE 0 END) as incomplete
FROM latest_applications la
GROUP BY la.haku, la.hakukohde;

-- name: yesql-get-direct-form-haut
WITH latest_applications AS (
    SELECT
    a1.key,
    f1.key as form_key,
    ar.state,
    max(a1.created_time) AS latest_time
    FROM applications a1
      INNER JOIN forms f1 ON (a1.form_id = f1.id)
      INNER JOIN application_reviews ar on a1.key = ar.application_key
    WHERE a1.haku IS NULL AND a1.hakukohde IS NULL
    AND (:query_type = 'ALL' OR f1.organization_oid IN (:authorized_organization_oids))
    GROUP BY a1.key, form_key, ar.state
),
latest_forms AS (
  SELECT key, MAX(id) AS max_id
  FROM forms
  WHERE (:query_type = 'ALL' OR organization_oid IN (:authorized_organization_oids))
  GROUP BY key
)
SELECT
  f.name,
  f.key,
  COUNT (la.key) AS application_count,
  SUM (CASE WHEN la.state = 'unprocessed' THEN 1 ELSE 0 END) as unprocessed,
  SUM (CASE WHEN la.state in (:incomplete_states) THEN 1 ELSE 0 END) as incomplete
FROM latest_applications la
JOIN latest_forms lf ON lf.key = la.form_key
JOIN forms f ON f.id = lf.max_id
GROUP BY f.name, f.key;
