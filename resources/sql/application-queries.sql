-- name: yesql-add-application-query<!
-- Add application
INSERT INTO applications
(form_id, key, content, lang, preferred_name, last_name, hakukohde, haku, secret, person_oid, ssn, dob, email)
VALUES
  (:form_id, :key, :content, :lang, :preferred_name, :last_name, :hakukohde, :haku, :secret, :person_oid, :ssn, :dob, :email);

-- name: yesql-get-application-list-by-form
SELECT
  a.id,
  a.key,
  a.lang,
  a.preferred_name,
  a.last_name,
  a.created_time,
  ar.state                               AS state,
  ar.score                               AS score
FROM applications a
  JOIN application_reviews ar ON a.key = ar.application_key
  JOIN forms f ON f.id = a.form_id AND f.key = :form_key
WHERE a.hakukohde IS NULL
ORDER BY a.created_time DESC;

-- name: yesql-get-application-list-by-hakukohde
SELECT
  a.id,
  a.key,
  a.lang,
  a.preferred_name,
  a.last_name,
  a.created_time,
  ar.state                               AS state,
  ar.score                               AS score,
  a.form_id                              AS form
FROM applications a
  JOIN application_reviews ar ON a.key = ar.application_key
  JOIN forms f ON a.form_id = f.id
WHERE a.hakukohde = :hakukohde_oid
      AND (:query_type = 'ALL' OR f.organization_oid IN (:authorized_organization_oids))
ORDER BY a.created_time DESC;

-- name: yesql-get-application-list-by-haku
SELECT
  a.id,
  a.key,
  a.lang,
  a.preferred_name,
  a.last_name,
  a.created_time,
  ar.state                               AS state,
  ar.score                               AS score,
  a.form_id                              AS form
FROM applications a
  JOIN application_reviews ar ON a.key = ar.application_key
  JOIN forms f ON a.form_id = f.id
WHERE a.haku = :haku_oid
      AND (:query_type = 'ALL' OR f.organization_oid IN (:authorized_organization_oids))
ORDER BY a.created_time DESC;

-- name: yesql-get-application-list-by-ssn
SELECT
  a.id,
  a.key,
  a.lang,
  a.preferred_name,
  a.last_name,
  a.created_time,
  ar.state                               AS state,
  ar.score                               AS score,
  a.form_id                              AS form
FROM applications a
  JOIN application_reviews ar ON a.key = ar.application_key
  JOIN forms f ON a.form_id = f.id
WHERE a.ssn = :ssn
      AND (:query_type = 'ALL' OR f.organization_oid IN (:authorized_organization_oids))
ORDER BY a.created_time DESC;

-- name: yesql-get-application-list-by-dob
SELECT
  a.id,
  a.key,
  a.lang,
  a.preferred_name,
  a.last_name,
  a.created_time,
  ar.state                               AS state,
  ar.score                               AS score,
  a.form_id                              AS form
FROM applications a
  JOIN application_reviews ar ON a.key = ar.application_key
  JOIN forms f ON a.form_id = f.id
WHERE a.dob = :dob
      AND (:query_type = 'ALL' OR f.organization_oid IN (:authorized_organization_oids))
ORDER BY a.created_time DESC;

-- name: yesql-get-application-list-by-email
SELECT
  a.id,
  a.key,
  a.lang,
  a.preferred_name,
  a.last_name,
  a.created_time,
  ar.state                               AS state,
  ar.score                               AS score,
  a.form_id                              AS form
FROM applications a
  JOIN application_reviews ar ON a.key = ar.application_key
  JOIN forms f ON a.form_id = f.id
WHERE a.email = :email
      AND (:query_type = 'ALL' OR f.organization_oid IN (:authorized_organization_oids))
ORDER BY a.created_time DESC;

-- name: yesql-get-application-events
SELECT
  event_type,
  time,
  new_review_state,
  application_key,
  id
FROM application_events
WHERE application_key = :application_key
ORDER BY time ASC;

-- name: yesql-get-application-review
SELECT
  id,
  modified_time,
  state,
  notes,
  score,
  application_key
FROM application_reviews
WHERE application_key = :application_key;

-- name: yesql-get-applications-for-form
-- Gets applications only for forms (omits hakukohde applications)
SELECT
  a.id,
  a.key,
  a.lang,
  a.form_id AS form,
  a.created_time,
  a.content,
  ar.state  AS state
FROM applications a
  JOIN forms f ON f.id = a.form_id AND f.key = :form_key
  JOIN application_reviews ar ON a.key = ar.application_key
WHERE a.hakukohde IS NULL AND state IN (:filtered_states);

-- name: yesql-get-applications-for-hakukohde
-- Get applications for form-key/hakukohde
SELECT
  a.id,
  a.key,
  a.lang,
  a.form_id AS form,
  a.created_time,
  a.content,
  a.hakukohde,
  ar.state  AS state,
  f.key     AS form_key
FROM applications a
  JOIN application_reviews ar ON a.key = ar.application_key
  JOIN forms f ON a.form_id = f.id
WHERE state IN (:filtered_states)
      AND a.hakukohde = :hakukohde_oid;

-- name: yesql-get-applications-for-haku
-- Get applications for form-key/haku
SELECT
  a.id,
  a.key,
  a.lang,
  a.form_id AS form,
  a.created_time,
  a.content,
  a.hakukohde,
  a.haku,
  ar.state  AS state,
  f.key     AS form_key
FROM applications a
  JOIN application_reviews ar ON a.key = ar.application_key
  JOIN forms f ON a.form_id = f.id
WHERE state IN (:filtered_states) AND a.haku = :haku_oid;

-- name: yesql-get-application-by-id
SELECT
  id,
  key,
  lang,
  form_id AS form,
  created_time,
  content,
  secret
FROM applications
WHERE id = :application_id;

-- name: yesql-get-latest-application-by-key
WITH latest_version AS (
    SELECT max(created_time) AS latest_time
    FROM applications a
    WHERE a.key = :application_key
)
SELECT
  id,
  key,
  lang,
  form_id AS form,
  created_time,
  content,
  hakukohde,
  person_oid,
  CASE
    WHEN ssn IS NOT NULL THEN (SELECT COUNT(*) FROM (SELECT DISTINCT(a2.key)
                                                     FROM applications a2
                                                       JOIN forms f2 ON a2.form_id = f2.id
                                                     WHERE a2.ssn = a.ssn
                                                           AND (:query_type = 'ALL' OR f2.organization_oid IN (:authorized_organization_oids))) AS temp)
    WHEN email IS NOT NULL THEN (SELECT COUNT(*) FROM (SELECT DISTINCT(a3.key)
                                                       FROM applications a3
                                                         JOIN forms f3 ON a3.form_id = f3.id
                                                       WHERE a3.email = a.email
                                                             AND (:query_type = 'ALL' OR f3.organization_oid IN (:authorized_organization_oids))) AS temp)
  END AS applications_count
  FROM applications a
  JOIN latest_version lv ON a.created_time = lv.latest_time;

-- name: yesql-get-latest-application-by-secret
WITH latest_version AS (
    SELECT max(created_time) AS latest_time
    FROM applications a
    WHERE a.secret = :secret
)
SELECT
  a.id,
  a.key,
  a.lang,
  a.form_id AS form,
  a.created_time,
  a.content,
  a.hakukohde,
  f.key     AS form_key
FROM applications a
  JOIN latest_version lv ON a.created_time = lv.latest_time
  JOIN forms f ON a.form_id = f.id;

-- name: yesql-get-latest-version-by-secret-lock-for-update
WITH latest_version AS (
    SELECT max(created_time) AS latest_time
    FROM applications a
    WHERE a.secret = :secret
)
SELECT
  id,
  key,
  lang,
  form_id AS form,
  created_time,
  content,
  haku,
  hakukohde,
  person_oid
FROM applications a
  JOIN latest_version lv ON a.created_time = lv.latest_time
FOR UPDATE;

-- name: yesql-get-application-organization-by-key
-- Get the related form's organization oid for access checks

WITH latest_version AS (
    SELECT max(created_time) AS latest_time
    FROM applications a
    WHERE a.key = :application_key
)
SELECT f.organization_oid
FROM applications a
  JOIN latest_version lv ON a.created_time = lv.latest_time
  JOIN forms f ON f.id = a.form_id;

-- name: yesql-get-application-review-organization-by-id
-- Get the related form's organization oid for access checks

SELECT f.organization_oid
FROM application_reviews ar
  JOIN applications a ON a.key = ar.application_key
  JOIN forms f ON f.id = a.form_id
                  AND ar.id = :review_id;

-- name: yesql-add-application-event!
-- Add application event
INSERT INTO application_events (application_key, event_type, new_review_state)
VALUES (:application_key, :event_type, :new_review_state);

-- name: yesql-add-application-review!
-- Add application review, initially it doesn't have all fields. This is just a "skeleton"
INSERT INTO application_reviews (application_key, state) VALUES (:application_key, :state);

-- name: yesql-save-application-review!
-- Save modifications for existing review record
UPDATE application_reviews
SET
  notes         = :notes,
  score         = :score,
  modified_time = now(),
  state         = :state
WHERE application_key = :application_key;

-- name: yesql-add-person-oid!
-- Add person OID to an application. Update also new versions of application if the user has updated
-- the application while we have been talking to person service (ONR)
UPDATE applications
SET person_oid = :person_oid
WHERE key IN (select key from applications where id = :id)
      AND id >= :id;

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
      INNER JOIN application_reviews ar ON a.key = ar.application_key
    WHERE a.haku IS NOT NULL AND a.hakukohde IS NOT NULL
          AND (:query_type = 'ALL' OR f.organization_oid IN (:authorized_organization_oids))
    GROUP BY a.key, a.haku, a.hakukohde, ar.state
)
SELECT
  la.haku,
  la.hakukohde,
  COUNT(la.key)   AS application_count,
  SUM(CASE WHEN la.state = 'unprocessed'
    THEN 1
      ELSE 0 END) AS unprocessed,
  SUM(CASE WHEN la.state IN (:incomplete_states)
    THEN 1
      ELSE 0 END) AS incomplete
FROM latest_applications la
GROUP BY la.haku, la.hakukohde;

-- name: yesql-get-direct-form-haut
WITH latest_applications AS (
    SELECT
      a1.key,
      f1.key               AS form_key,
      ar.state,
      max(a1.created_time) AS latest_time
    FROM applications a1
      INNER JOIN forms f1 ON (a1.form_id = f1.id)
      INNER JOIN application_reviews ar ON a1.key = ar.application_key
    WHERE a1.haku IS NULL AND a1.hakukohde IS NULL
          AND (:query_type = 'ALL' OR f1.organization_oid IN (:authorized_organization_oids))
    GROUP BY a1.key, form_key, ar.state
),
    latest_forms AS (
      SELECT
        key,
        MAX(id) AS max_id
      FROM forms
      WHERE (:query_type = 'ALL' OR organization_oid IN (:authorized_organization_oids))
      GROUP BY key
  )
SELECT
  f.name,
  f.key,
  COUNT(la.key)   AS application_count,
  SUM(CASE WHEN la.state = 'unprocessed'
    THEN 1
      ELSE 0 END) AS unprocessed,
  SUM(CASE WHEN la.state IN (:incomplete_states)
    THEN 1
      ELSE 0 END) AS incomplete
FROM latest_applications la
  JOIN latest_forms lf ON lf.key = la.form_key
  JOIN forms f ON f.id = lf.max_id
GROUP BY f.name, f.key;
