-- name: yesql-add-application<!
-- Add application
INSERT INTO applications (
  form_id,
  content,
  lang,
  preferred_name,
  last_name,
  hakukohde,
  haku,
  secret,
  person_oid,
  ssn,
  dob,
  email
) VALUES (
  :form_id,
  :content,
  :lang,
  :preferred_name,
  :last_name,
  ARRAY[:hakukohde]::character varying(127)[],
  :haku,
  :secret,
  :person_oid,
  :ssn,
  :dob,
  :email
);

-- name: yesql-add-application-version<!
-- Add application version
INSERT INTO applications (
  form_id,
  key,
  content,
  lang,
  preferred_name,
  last_name,
  hakukohde,
  haku,
  secret,
  person_oid,
  ssn,
  dob,
  email
) VALUES (
  :form_id,
  :key,
  :content,
  :lang,
  :preferred_name,
  :last_name,
  ARRAY[:hakukohde]::character varying(127)[],
  :haku,
  :secret,
  :person_oid,
  :ssn,
  :dob,
  :email
);

-- name: yesql-get-application-list-by-form
WITH latest AS (
  SELECT DISTINCT ON (key) * FROM applications ORDER BY key, created_time DESC
)
SELECT
  a.id,
  a.key,
  a.lang,
  a.preferred_name,
  a.last_name,
  a.created_time,
  ar.state                               AS state,
  ar.score                               AS score
FROM latest a
  JOIN application_reviews ar ON a.key = ar.application_key
  JOIN forms f ON f.id = a.form_id AND f.key = :form_key
WHERE a.haku IS NULL
ORDER BY a.created_time DESC;

-- name: yesql-get-application-list-by-hakukohde
WITH latest AS (
  SELECT DISTINCT ON (key) * FROM applications ORDER BY key, created_time DESC
)
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
FROM latest a
  JOIN application_reviews ar ON a.key = ar.application_key
  JOIN forms f ON a.form_id = f.id
WHERE :hakukohde_oid = ANY (a.hakukohde)
      AND (:query_type = 'ALL' OR f.organization_oid IN (:authorized_organization_oids))
ORDER BY a.created_time DESC;

-- name: yesql-get-application-list-by-haku
WITH latest AS (
  SELECT DISTINCT ON (key) * FROM applications ORDER BY key, created_time DESC
)
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
FROM latest a
  JOIN application_reviews ar ON a.key = ar.application_key
  JOIN forms f ON a.form_id = f.id
WHERE a.haku = :haku_oid
      AND (:query_type = 'ALL' OR f.organization_oid IN (:authorized_organization_oids))
ORDER BY a.created_time DESC;

-- name: yesql-get-application-list-by-ssn
WITH latest AS (
  SELECT DISTINCT ON (key) * FROM applications ORDER BY key, created_time DESC
)
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
FROM latest a
  JOIN application_reviews ar ON a.key = ar.application_key
  JOIN forms f ON a.form_id = f.id
WHERE a.ssn = :ssn
      AND (:query_type = 'ALL' OR f.organization_oid IN (:authorized_organization_oids))
ORDER BY a.created_time DESC;

-- name: yesql-get-application-list-by-dob
WITH latest AS (
  SELECT DISTINCT ON (key) * FROM applications ORDER BY key, created_time DESC
)
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
FROM latest a
  JOIN application_reviews ar ON a.key = ar.application_key
  JOIN forms f ON a.form_id = f.id
WHERE a.dob = :dob
      AND (:query_type = 'ALL' OR f.organization_oid IN (:authorized_organization_oids))
ORDER BY a.created_time DESC;

-- name: yesql-get-application-list-by-email
WITH latest AS (
  SELECT DISTINCT ON (key) * FROM applications ORDER BY key, created_time DESC
)
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
FROM latest a
  JOIN application_reviews ar ON a.key = ar.application_key
  JOIN forms f ON a.form_id = f.id
WHERE a.email = :email
      AND (:query_type = 'ALL' OR f.organization_oid IN (:authorized_organization_oids))
ORDER BY a.created_time DESC;

-- name: yesql-get-application-list-by-name
WITH latest AS (
  SELECT DISTINCT ON (key) * FROM applications ORDER BY key, created_time DESC
)
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
FROM latest a
  JOIN application_reviews ar ON a.key = ar.application_key
  JOIN forms f ON a.form_id = f.id
WHERE to_tsvector('simple', a.preferred_name || ' ' || a.last_name) @@ to_tsquery(:name)
      AND (:query_type = 'ALL' OR f.organization_oid IN (:authorized_organization_oids))
ORDER BY a.created_time DESC;

-- name: yesql-get-application-list-by-person-oid-for-omatsivut
WITH latest AS (
  SELECT DISTINCT ON (key) * FROM applications ORDER BY key, created_time DESC
)
SELECT
  a.id,
  a.key,
  a.lang,
  a.preferred_name,
  a.last_name,
  a.created_time,
  ar.state                               AS state,
  ar.score                               AS score,
  a.form_id                              AS form,
  a.haku,
  a.secret
FROM latest a
  JOIN application_reviews ar ON a.key = ar.application_key
  JOIN forms f ON a.form_id = f.id
WHERE a.person_oid = :person_oid
      AND (:query_type = 'ALL' OR f.organization_oid IN (:authorized_organization_oids))
ORDER BY a.created_time DESC;

-- name: yesql-get-application-events
SELECT
  ae.event_type,
  ae.time,
  ae.new_review_state,
  ae.application_key,
  ae.id,
  ae.hakukohde,
  ae.review_key,
  v.first_name,
  v.last_name
FROM application_events ae
LEFT JOIN virkailija v ON ae.virkailija_oid = v.oid
WHERE ae.application_key = :application_key
ORDER BY ae.time ASC;

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
WITH latest AS (
  SELECT DISTINCT ON (key) * FROM applications ORDER BY key, created_time DESC
)
SELECT
  a.id,
  a.key,
  a.lang,
  a.form_id AS form,
  a.created_time,
  a.content,
  a.person_oid,
  ar.state  AS state
FROM latest a
  JOIN forms f ON f.id = a.form_id AND f.key = :form_key
  JOIN application_reviews ar ON a.key = ar.application_key
WHERE a.haku IS NULL AND state IN (:filtered_states)
ORDER BY a.created_time DESC;

-- name: yesql-get-applications-for-hakukohde
-- Get applications for form-key/hakukohde
WITH latest AS (
  SELECT DISTINCT ON (key) * FROM applications ORDER BY key, created_time DESC
)
SELECT
  a.id,
  a.key,
  a.lang,
  a.form_id AS form,
  a.created_time,
  a.content,
  a.haku,
  a.hakukohde,
  a.person_oid,
  ar.state  AS state,
  f.key     AS form_key
FROM latest a
  JOIN application_reviews ar ON a.key = ar.application_key
  JOIN forms f ON a.form_id = f.id
WHERE state IN (:filtered_states)
      AND :hakukohde_oid = ANY (a.hakukohde)
ORDER BY a.created_time DESC;

-- name: yesql-get-applications-for-haku
-- Get applications for form-key/haku
WITH latest AS (
  SELECT DISTINCT ON (key) * FROM applications ORDER BY key, created_time DESC
)
SELECT
  a.id,
  a.key,
  a.lang,
  a.form_id AS form,
  a.created_time,
  a.content,
  a.hakukohde,
  a.haku,
  a.person_oid,
  ar.state  AS state,
  f.key     AS form_key
FROM latest a
  JOIN application_reviews ar ON a.key = ar.application_key
  JOIN forms f ON a.form_id = f.id
WHERE state IN (:filtered_states) AND a.haku = :haku_oid
ORDER BY a.created_time DESC;

-- name: yesql-get-application-by-id
SELECT
  id,
  key,
  lang,
  form_id AS form,
  created_time,
  content,
  person_oid,
  secret
FROM applications
WHERE id = :application_id;

-- name: yesql-has-ssn-applied
WITH latest AS (
  SELECT DISTINCT ON (key) * FROM applications ORDER BY key, created_time DESC
)
SELECT EXISTS (SELECT 1 FROM latest
               JOIN application_reviews
               ON application_key = key
               WHERE haku = :haku_oid
                   AND ssn = upper(:ssn)
                   AND state <> 'inactivated') AS has_applied;

-- name: yesql-has-email-applied
WITH latest AS (
  SELECT DISTINCT ON (key) * FROM applications ORDER BY key, created_time DESC
)
SELECT EXISTS (SELECT 1 FROM latest
               JOIN application_reviews
               ON application_key = key
               WHERE haku = :haku_oid
                   AND email = :email
                   AND state <> 'inactivated') AS has_applied;

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
  haku,
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
  a.haku,
  a.hakukohde,
  f.key     AS form_key
FROM applications a
  JOIN latest_version lv ON a.created_time = lv.latest_time
  JOIN forms f ON a.form_id = f.id;

-- name: yesql-get-latest-application-by-virkailija-secret
WITH latest_version AS (
    SELECT max(a.created_time) AS latest_time
    FROM applications a
    JOIN virkailija_credentials AS vc
    ON a.key = vc.application_key
    WHERE vc.secret = :virkailija_secret
)
SELECT
  a.id,
  a.key,
  a.lang,
  a.form_id AS form,
  a.created_time,
  a.content,
  a.haku,
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

-- name: yesql-get-latest-version-by-virkailija-secret-lock-for-update
WITH latest_version AS (
    SELECT max(a.created_time) AS latest_time
    FROM applications a
    JOIN virkailija_credentials AS vc
      ON a.key = vc.application_key
    WHERE vc.secret = :virkailija_secret
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

-- name: yesql-organization-oids-of-applications-of-persons
-- Get the organization oids of the related forms

WITH latest_version AS (
    SELECT max(created_time) AS latest_time
    FROM applications a
    WHERE a.person_oid IN (:person_oids)
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
INSERT INTO application_events (application_key, event_type, new_review_state, virkailija_oid, hakukohde, review_key)
VALUES (:application_key, :event_type, :new_review_state, :virkailija_oid, :hakukohde, :review_key);

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
      unnest(a.hakukohde) as hakukohde,
      ar.state,
      max(a.created_time) AS latest_time
    FROM applications a
      INNER JOIN forms f ON (a.form_id = f.id)
      INNER JOIN application_reviews ar ON a.key = ar.application_key
    WHERE a.haku IS NOT NULL
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
    WHERE a1.haku IS NULL
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

-- name: yesql-add-application-feedback<!
INSERT INTO application_feedback (created_time, form_key, form_id, form_name, stars, feedback, user_agent)
VALUES
  (now(), :form_key, :form_id, :form_name, :rating, left(:feedback, 2000), :user_agent);

-- name: yesql-get-hakija-secret-by-virkailija-secret
SELECT a.secret FROM applications a
INNER JOIN virkailija_credentials c ON a.key = c.application_key
WHERE c.secret = :virkailija_secret
ORDER BY a.created_time DESC LIMIT 1;

-- name: yesql-get-application-hakukohde-reviews
SELECT
  id,
  modified_time,
  requirement,
  state,
  hakukohde,
  application_key
FROM application_hakukohde_reviews
WHERE application_key = :application_key;

-- name: yesql-upsert-application-hakukohde-review!
INSERT INTO application_hakukohde_reviews (application_key, requirement, state, hakukohde)
VALUES (:application_key, :requirement, :state, :hakukohde)
ON CONFLICT (application_key, requirement, hakukohde)
  WHERE hakukohde IS NOT NULL
  DO UPDATE SET state = :state;

-- name: yesql-get-existing-application-review
SELECT id
FROM application_hakukohde_reviews
WHERE application_key = :application_key AND requirement = :requirement AND state = :state AND hakukohde = :hakukohde;

-- name: yesql-get-existing-requirement-review
SELECT
  id,
  modified_time,
  requirement,
  state,
  hakukohde,
  application_key
FROM application_hakukohde_reviews
WHERE application_key = :application_key AND state = :state AND hakukohde = :hakukohde;

-- name: yesql-applications-by-haku
SELECT
  key,
  haku,
  person_oid,
  lang,
  preferred_name,
  email,
  ssn,
  hakukohde
FROM applications
WHERE haku = :haku_oid;
