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
  :person_oid,
  upper(:ssn),
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
  :person_oid,
  upper(:ssn),
  :dob,
  :email
);

-- name: yesql-add-application-secret!
INSERT INTO application_secrets (application_key, secret) VALUES (:application_key, :secret);

-- name: yesql-get-application-list-for-virkailija
SELECT
  a.id,
  a.person_oid AS "person-oid",
  a.key,
  a.lang,
  a.preferred_name AS "preferred-name",
  a.last_name AS "last-name",
  a.created_time AS "created-time",
  a.haku,
  a.hakukohde,
  a.ssn,
  (SELECT cast(value as JSON)
   FROM jsonb_to_recordset(a.content->'answers') x(key text, value text)
   WHERE key = 'higher-completed-base-education') AS "base-education",
  ar.state                            AS state,
  ar.score                            AS score,
  a.form_id                           AS form,
  lf.organization_oid AS "organization-oid",
  (SELECT json_agg(json_build_object('requirement', requirement,
                                     'state', state,
                                     'hakukohde', hakukohde))
   FROM application_hakukohde_reviews ahr
   WHERE ahr.application_key = a.key) AS "application-hakukohde-reviews",
  (SELECT json_agg(json_build_object('attachment-key', attachment_key,
                                     'state', state,
                                     'hakukohde', hakukohde))
   FROM application_hakukohde_attachment_reviews aar
   WHERE aar.application_key = a.key) AS "application-attachment-reviews",
  (SELECT count(*)
   FROM application_events AS ae
   WHERE ae.application_key = a.key AND
         ae.event_type = 'updated-by-applicant' AND
         ae.time > (SELECT max(time)
                    FROM application_events
                    WHERE application_key = ae.application_key AND
                          new_review_state = 'information-request') IS NOT DISTINCT FROM true) AS "new-application-modifications",
  (SELECT min(created_time) FROM applications WHERE a.key = key) AS "original-created-time"
FROM latest_applications AS a
  JOIN application_reviews AS ar ON a.key = ar.application_key
  JOIN forms AS f ON a.form_id = f.id
  JOIN latest_forms AS lf ON lf.key = f.key
WHERE (:form::text IS NULL OR (lf.key = :form AND a.haku IS NULL))
  AND (:application_oid::text IS NULL OR a.key = :application_oid)
  AND (:application_oids::text[] IS NULL OR a.key = ANY (:application_oids))
  AND (:person_oid::text IS NULL OR a.person_oid = :person_oid)
  AND (:name::text IS NULL OR to_tsvector('simple', a.preferred_name || ' ' || a.last_name) @@ to_tsquery(:name))
  AND (:email::text IS NULL OR lower(a.email) = lower(:email))
  AND (:dob::text IS NULL OR a.dob = to_date(:dob, 'DD.MM.YYYY'))
  AND (:ssn::text IS NULL OR a.ssn = :ssn)
  AND (:haku::text IS NULL OR a.haku = :haku)
  AND (:hakukohde::text IS NULL OR :hakukohde = ANY (a.hakukohde))
  AND (:ensisijainen_hakukohde::text IS NULL OR a.hakukohde[1] = :ensisijainen_hakukohde)
ORDER BY a.created_time DESC;

-- name: yesql-get-application-list-by-person-oid-for-omatsivut
SELECT
  a.key       AS oid,
  a.key       AS key,
  las.secret  AS secret,
  ar.state    AS state,
  a.haku      AS haku,
  a.email     AS email,
  a.hakukohde AS hakukohteet
FROM applications AS a
  JOIN application_reviews ar ON a.key = ar.application_key
  JOIN latest_application_secrets las ON a.key = las.application_key
WHERE a.person_oid = :person_oid
      AND a.haku IS NOT NULL
      AND ar.state <> 'inactivated'
      AND a.id = (SELECT id FROM latest_applications WHERE key = a.key)
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
  score,
  application_key
FROM application_reviews
WHERE application_key = :application_key;

-- name: yesql-get-application-reviews-by-keys
SELECT
  id,
  modified_time,
  state,
  score,
  application_key
FROM application_reviews
WHERE application_key IN (:application_keys);

-- name: yesql-get-application-review-notes
SELECT rn.id, rn.created_time, rn.application_key, rn.notes, rn.hakukohde, rn.state_name, v.first_name, v.last_name
FROM application_review_notes rn
LEFT JOIN virkailija v ON rn.virkailija_oid = v.oid
WHERE rn.application_key = :application_key AND (removed IS NULL OR removed > NOW())
ORDER BY rn.created_time DESC;

-- name: yesql-get-application-review-notes-by-keys
SELECT rn.id, rn.created_time, rn.application_key, rn.notes, rn.hakukohde, rn.state_name, v.first_name, v.last_name
FROM application_review_notes rn
  LEFT JOIN virkailija v ON rn.virkailija_oid = v.oid
WHERE rn.application_key IN (:application_keys) AND (rn.removed IS NULL OR rn.removed > NOW())
ORDER BY rn.created_time DESC;

-- name: yesql-get-applications-by-keys
-- Get list of applications by their keys
SELECT
  a.id,
  a.key,
  a.lang,
  a.form_id                           AS form,
  a.created_time,
  a.content,
  a.person_oid,
  a.hakukohde,
  a.haku,
  ar.state                            AS state,
  f.key                               AS form_key,
  (SELECT json_agg(json_build_object('requirement', requirement,
                                     'state', state,
                                     'hakukohde', hakukohde))
   FROM application_hakukohde_reviews ahr
   WHERE ahr.application_key = a.key) AS application_hakukohde_reviews
FROM latest_applications AS a
  JOIN application_reviews AS ar ON a.key = ar.application_key
  JOIN forms AS f ON a.form_id = f.id
WHERE a.key IN (:application_keys)
ORDER BY a.created_time DESC;

-- name: yesql-get-applications-for-form
-- Gets applications only for forms (omits hakukohde applications)
SELECT
  a.id,
  a.key,
  a.lang,
  a.form_id AS form,
  a.created_time,
  a.content,
  a.person_oid,
  ar.state  AS state
FROM latest_applications AS a
JOIN forms AS f ON f.id = a.form_id
JOIN application_reviews AS ar ON a.key = ar.application_key
WHERE a.haku IS NULL
  AND state IN (:filtered_states)
  AND f.key = :form_key
ORDER BY a.created_time DESC;

-- name: yesql-get-applications-for-hakukohde
-- Get applications for form-key/hakukohde
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
FROM latest_applications AS a
JOIN application_reviews AS ar ON a.key = ar.application_key
JOIN forms AS f ON a.form_id = f.id
WHERE state IN (:filtered_states)
      AND :hakukohde_oid = ANY (a.hakukohde)
ORDER BY a.created_time DESC;

-- name: yesql-get-applications-for-haku
-- Get applications for form-key/haku
SELECT
  a.id,
  a.key,
  a.person_oid,
  a.lang,
  a.form_id AS form,
  a.created_time,
  a.content,
  a.hakukohde,
  a.haku,
  a.person_oid,
  ar.state  AS state,
  f.key     AS form_key
FROM latest_applications AS a
JOIN application_reviews AS ar ON a.key = ar.application_key
JOIN forms AS f ON a.form_id = f.id
WHERE state IN (:filtered_states)
  AND a.haku = :haku_oid
ORDER BY a.created_time DESC;

-- name: yesql-get-application-by-id
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
  las.secret
FROM applications a
  JOIN latest_application_secrets las ON a.key = las.application_key
WHERE a.id = :application_id;

-- name: yesql-has-ssn-applied
SELECT EXISTS (SELECT 1 FROM latest_applications
               JOIN application_reviews
               ON application_key = key
               WHERE haku = :haku_oid
                   AND ssn = upper(:ssn)
                   AND state <> 'inactivated') AS has_applied;

-- name: yesql-has-email-applied
SELECT EXISTS (SELECT 1 FROM latest_applications
               JOIN application_reviews
               ON application_key = key
               WHERE haku = :haku_oid
                   AND email = :email
                   AND state <> 'inactivated') AS has_applied;

-- name: yesql-get-latest-application-by-key
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
  lf.organization_oid,
  CASE
  WHEN a.ssn IS NOT NULL
    THEN (SELECT count(*)
          FROM latest_applications AS aa
          WHERE aa.ssn = a.ssn)
  WHEN a.email IS NOT NULL
    THEN (SELECT count(*)
          FROM latest_applications AS aa
          WHERE aa.email = a.email)
  END AS applications_count
FROM latest_applications AS a
JOIN forms AS f ON f.id = a.form_id
JOIN latest_forms AS lf ON lf.key = f.key
WHERE a.key = :application_key;

-- name: yesql-applications-authorization-data
SELECT
  a.haku,
  a.hakukohde,
  lf.organization_oid
FROM latest_applications as a
JOIN forms AS f ON f.id = a.form_id
JOIN latest_forms AS lf on lf.key = f.key
WHERE a.key IN (:application_keys);

-- name: yesql-persons-applications-authorization-data
SELECT
  a.haku,
  a.hakukohde,
  lf.organization_oid
FROM latest_applications as a
JOIN forms AS f ON f.id = a.form_id
JOIN latest_forms AS lf on lf.key = f.key
WHERE a.person_oid IN (:person_oids);

-- name: yesql-get-latest-application-by-key-with-hakukohde-reviews
SELECT
  a.id,
  a.key,
  a.lang,
  a.form_id                           AS form,
  a.created_time,
  a.content,
  a.hakukohde,
  a.haku,
  a.person_oid,
  las.secret,
  CASE
  WHEN ssn IS NOT NULL
    THEN (SELECT count(*)
          FROM latest_applications AS aa
          WHERE aa.ssn = a.ssn)
  WHEN email IS NOT NULL
    THEN (SELECT count(*)
          FROM latest_applications AS aa
          WHERE aa.email = a.email)
  END                                 AS applications_count,
  (SELECT json_agg(json_build_object('requirement', requirement,
                                     'state', state,
                                     'hakukohde', hakukohde))
   FROM application_hakukohde_reviews ahr
   WHERE ahr.application_key = a.key) AS application_hakukohde_reviews
FROM latest_applications AS a
  JOIN latest_application_secrets las ON a.key = las.application_key
WHERE a.key = :application_key;

-- name: yesql-get-latest-application-by-secret
SELECT
  a.id,
  a.person_oid,
  a.key,
  a.lang,
  a.form_id AS form,
  a.created_time,
  a.content,
  a.haku,
  a.hakukohde,
  f.key     AS form_key
FROM latest_applications AS a
  JOIN forms AS f ON a.form_id = f.id
  JOIN latest_application_secrets las ON a.key = las.application_key
WHERE las.secret = :secret
      AND las.created_time > now() - INTERVAL '30 days';

-- name: yesql-get-latest-application-by-virkailija-secret
SELECT
  a.id,
  a.key,
  a.person_oid,
  a.lang,
  a.form_id AS form,
  a.created_time,
  a.content,
  a.haku,
  a.hakukohde,
  f.key     AS form_key
FROM latest_applications AS a
JOIN forms f ON a.form_id = f.id
JOIN virkailija_update_secrets AS vus ON vus.application_key = a.key
WHERE vus.secret = :virkailija_secret;

-- name: yesql-get-latest-version-by-secret-lock-for-update
WITH application_secret AS (SELECT
                              application_key,
                              id
                            FROM application_secrets
                            WHERE secret = :secret),
    latest_secret AS (SELECT
                        application_key,
                        id
                      FROM application_secrets
                      WHERE application_key = (SELECT application_key
                                               FROM application_secret)
                      ORDER BY id DESC
                      LIMIT 1)
SELECT *
FROM applications a
  JOIN application_secret ON application_secret.application_key = a.key
  JOIN latest_secret ON latest_secret.application_key = a.key
WHERE application_secret.id = latest_secret.id
ORDER BY a.id DESC
LIMIT 1
FOR UPDATE;

-- name: yesql-get-application-count-for-secret
SELECT count(id) AS count
FROM application_secrets
WHERE secret = :secret;

-- name: yesql-get-application-key-for-any-version-of-secret
SELECT application_key
FROM application_secrets
WHERE secret = :secret
ORDER BY id DESC
LIMIT 1;

-- name: yesql-get-latest-application-language-by-any-version-of-secret
SELECT lang
FROM latest_applications
WHERE key = (SELECT application_key
             FROM application_secrets
             WHERE secret = :secret);

-- name: yesql-get-latest-version-by-virkailija-secret-lock-for-update
SELECT
  a.id,
  a.key,
  a.lang,
  a.form_id AS form,
  a.created_time,
  a.content,
  a.haku,
  a.hakukohde,
  a.person_oid
FROM applications a
WHERE a.id = (SELECT max(a.id)
              FROM applications AS a
              JOIN virkailija_update_secrets AS vus
                ON vus.application_key = a.key
              WHERE vus.secret = :virkailija_secret)
FOR UPDATE;

-- name: yesql-add-application-event<!
-- Add application event
INSERT INTO application_events (application_key, event_type, new_review_state, virkailija_oid, hakukohde, review_key)
VALUES (:application_key, :event_type, :new_review_state, :virkailija_oid, :hakukohde, :review_key);

-- name: yesql-add-application-review!
-- Add application review, initially it doesn't have all fields. This is just a "skeleton"
INSERT INTO application_reviews (application_key, state) VALUES (:application_key, :state);

-- name: yesql-save-attachment-review!
INSERT INTO application_hakukohde_attachment_reviews (application_key, attachment_key, hakukohde, state)
VALUES (:application_key, :attachment_key, :hakukohde, :state)
ON CONFLICT (application_key, attachment_key, hakukohde)
  DO NOTHING;

-- name: yesql-save-application-review!
-- Save modifications for existing review record
UPDATE application_reviews
SET
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
SELECT a.haku,
       a.hakukohde,
       max(lf.organization_oid) AS organization_oid,
       max(haku_application_count.n) AS haku_application_count,
       count(*) AS application_count,
       count(*) FILTER (WHERE ahr.state IS NOT DISTINCT FROM 'processed') AS processed,
       count(*) FILTER (WHERE ahr.state IS NOT NULL AND
                              ahr.state NOT IN ('unprocessed', 'processed')) AS processing
FROM (SELECT key, form_id, haku, unnest(hakukohde) AS hakukohde
      FROM latest_applications
      WHERE haku IS NOT NULL) AS a
JOIN forms AS f
  ON f.id = a.form_id
JOIN latest_forms AS lf
  ON lf.key = f.key
JOIN (SELECT haku, count(*) AS n
      FROM latest_applications AS a
      JOIN application_reviews AS ar
        ON ar.application_key = a.key
      WHERE ar.state != 'inactivated'
      GROUP BY haku) AS haku_application_count
  ON haku_application_count.haku = a.haku
JOIN application_reviews AS ar
  ON ar.application_key = a.key
LEFT JOIN application_hakukohde_reviews AS ahr
  ON ahr.application_key = a.key AND
     ahr.hakukohde = a.hakukohde AND
     ahr.requirement = 'processing-state'
WHERE ar.state != 'inactivated'
GROUP BY a.haku, a.hakukohde;

-- name: yesql-get-direct-form-haut
SELECT
  lf.name,
  lf.key,
  lf.organization_oid,
  count(a.key)    AS haku_application_count,
  count(a.key)    AS application_count,
  sum(CASE WHEN ar.state = 'inactivated' OR ahr.state = 'processed'
    THEN 1
      ELSE 0 END) AS processed,
  sum(CASE WHEN ar.state != 'inactivated' AND (ahr.state IS NULL OR ahr.state = 'unprocessed')
    THEN 1
      ELSE 0 END) AS unprocessed
FROM latest_applications AS a
  JOIN forms AS f ON f.id = a.form_id
  JOIN latest_forms AS lf ON lf.key = f.key
  JOIN application_reviews AS ar ON a.key = ar.application_key
  LEFT JOIN application_hakukohde_reviews AS ahr
    ON ahr.application_key = a.key
       AND ahr.hakukohde = 'form'
       AND ahr.requirement = 'processing-state'
WHERE a.haku IS NULL
GROUP BY lf.name, lf.key, lf.organization_oid;

-- name: yesql-add-application-feedback<!
INSERT INTO application_feedback (created_time, form_key, form_id, form_name, stars, feedback, user_agent)
VALUES
  (now(), :form_key, :form_id, :form_name, :rating, left(:feedback, 2000), :user_agent);

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

-- name: yesql-get-application-attachment-reviews
SELECT
  attachment_key,
  state,
  hakukohde
FROM application_hakukohde_attachment_reviews
WHERE application_key = :application_key;

-- name: yesql-get-payment-obligation-for-applications
SELECT DISTINCT ON (application_key, hakukohde) application_key, hakukohde, state
FROM application_hakukohde_reviews
WHERE application_key IN (:hakemus_oids)
AND requirement = 'payment-obligation'
ORDER BY application_key, hakukohde, id DESC;

-- name: yesql-upsert-application-hakukohde-review!
INSERT INTO application_hakukohde_reviews (application_key, requirement, state, hakukohde)
VALUES (:application_key, :requirement, :state, :hakukohde)
ON CONFLICT (application_key, requirement, hakukohde)
  WHERE hakukohde IS NOT NULL
  DO UPDATE SET state = :state;

-- name: yesql-get-existing-application-hakukohde-review
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
WHERE application_key = :application_key AND requirement = :requirement AND hakukohde = :hakukohde;

-- name: yesql-update-attachment-hakukohde-review!
INSERT INTO application_hakukohde_attachment_reviews (application_key, attachment_key, hakukohde, state)
VALUES (:application_key, :attachment_key, :hakukohde, :state)
ON CONFLICT (application_key, attachment_key, hakukohde)
DO UPDATE SET state = :state, modified_time = now();

-- name: yesql-get-existing-attachment-review
SELECT *
FROM application_hakukohde_attachment_reviews
WHERE application_key = :application_key AND attachment_key = :attachment_key AND hakukohde = :hakukohde;

-- name: yesql-delete-application-attachment-reviews!
DELETE FROM application_hakukohde_attachment_reviews
WHERE application_key = :application_key
      AND (attachment_key NOT IN (:attachment_keys)
           OR hakukohde NOT IN (:applied_hakukohteet));

-- name: yesql-applications-by-haku-and-hakukohde-oids
SELECT
  la.key,
  haku,
  person_oid,
  lang,
  preferred_name,
  email,
  ssn,
  hakukohde,
  lf.organization_oid,
  la.content,
  (SELECT json_agg(json_build_object('requirement', requirement,
                                     'state', state,
                                     'hakukohde', hakukohde))
   FROM application_hakukohde_reviews AS ahr
   WHERE ahr.application_key = la.key) AS application_hakukohde_reviews
FROM latest_applications AS la
JOIN application_reviews as ar ON ar.application_key = la.key
JOIN forms AS f ON la.form_id = f.id
JOIN latest_forms AS lf ON lf.key = f.key
WHERE
  person_oid IS NOT NULL
  AND haku IS NOT NULL
  AND (:haku_oid::text IS NULL OR haku = :haku_oid)
  -- Parameter list contains empty string to avoid empty lists
  AND (array_length(ARRAY[:hakemus_oids], 1) < 2 OR la.key IN (:hakemus_oids))
  AND (array_length(ARRAY[:hakukohde_oids], 1) < 2 OR ARRAY[:hakukohde_oids] && hakukohde)
  AND state <> 'inactivated'
ORDER BY la.created_time DESC;

-- name: yesql-valinta-ui-applications
SELECT
  a.key AS oid,
  haku AS haku_oid,
  person_oid AS person_oid,
  hakukohde AS hakukohde,
  (SELECT answers->>'value'
   FROM jsonb_array_elements(a.content->'answers') AS answers
   WHERE answers->>'key' = 'address') AS lahiosoite,
  (SELECT answers->>'value'
   FROM jsonb_array_elements(a.content->'answers') AS answers
   WHERE answers->>'key' = 'postal-code') AS postinumero,
  (SELECT json_agg(json_build_object('requirement', requirement,
                                     'state', state,
                                     'hakukohde', hakukohde))
   FROM application_hakukohde_reviews AS ahr
   WHERE ahr.application_key = a.key) AS application_hakukohde_reviews
FROM applications AS a
JOIN application_reviews as ar ON ar.application_key = a.key
WHERE
  a.id = (SELECT id FROM latest_applications WHERE key = a.key)
  AND person_oid IS NOT NULL
  AND haku IS NOT NULL
  AND state <> 'inactivated'
  AND (:application_oids::text[] IS NULL OR a.key = ANY (:application_oids))
  AND (:name::text IS NULL OR to_tsvector('simple', a.preferred_name || ' ' || a.last_name) @@ to_tsquery(:name))
  AND (:haku::text IS NULL OR a.haku = :haku)
  AND (:hakukohde::text IS NULL OR :hakukohde = ANY (a.hakukohde))
ORDER BY a.created_time DESC;

--name: yesql-applications-for-hakurekisteri
SELECT
  key,
  haku,
  hakukohde,
  person_oid,
  lang,
  email,
  content
FROM latest_applications
JOIN application_reviews ON application_key = key
WHERE person_oid IS NOT NULL
  AND haku IS NOT NULL
  AND (:haku_oid::text IS NULL OR haku = :haku_oid)
  -- Parameter list contains empty string to avoid empty lists
  AND (array_length(ARRAY[:hakukohde_oids], 1) < 2 OR ARRAY[:hakukohde_oids] && hakukohde)
  AND (array_length(ARRAY[:person_oids], 1) < 2 OR person_oid IN (:person_oids))
  AND (:modified_after::text IS NULL OR created_time > :modified_after::TIMESTAMPTZ)
  AND state <> 'inactivated'
ORDER BY created_time DESC;

--name: yesql-get-applications-by-created-time
SELECT
  key,
  haku,
  hakukohde,
  person_oid,
  content,
  application_reviews.state
FROM latest_applications
  LEFT JOIN application_reviews ON latest_applications.key = application_reviews.application_key
WHERE created_time > :date :: DATE
      AND person_oid IS NOT NULL
ORDER BY created_time DESC
LIMIT :limit
OFFSET :offset;

--name: yesql-onr-applications
SELECT a.key AS key,
       a.haku AS haku,
       f.key AS form,
       a.email AS email,
       a.content AS content
FROM latest_applications AS a
JOIN forms AS f ON f.id = a.form_id
WHERE a.person_oid = :person_oid
ORDER BY a.created_time DESC;

--name: yesql-add-review-note<!
INSERT INTO application_review_notes (application_key, notes, virkailija_oid, hakukohde, state_name)
VALUES (:application_key, :notes, :virkailija_oid, :hakukohde, :state_name);

-- name: yesql-remove-review-note!
UPDATE application_review_notes SET removed = NOW() WHERE id = :id;

--name: yesql-tilastokeskus-applications
SELECT
  haku AS haku_oid,
  key AS hakemus_oid,
  person_oid henkilo_oid,
  hakukohde AS hakukohde_oids
FROM latest_applications
  JOIN application_reviews ON application_key = key
WHERE person_oid IS NOT NULL
  AND haku IS NOT NULL
  AND haku = :haku_oid
  AND state <> 'inactivated'
  AND (:hakukohde_oid::TEXT IS NULL OR :hakukohde_oid = ANY (hakukohde))
ORDER BY created_time DESC;

--name: yesql-valintalaskenta-applications
SELECT
  key,
  person_oid,
  haku,
  hakukohde AS hakutoiveet,
  content,
  (SELECT json_object_agg(hakukohde, state)
   FROM application_hakukohde_reviews AS ahr
   WHERE ahr.application_key = key
         AND ahr.requirement = 'eligibility-state') AS application_hakukohde_reviews
FROM latest_applications
  JOIN application_reviews ON application_key = key
WHERE person_oid IS NOT NULL
  AND haku IS NOT NULL
  AND (:hakukohde_oid::TEXT IS NULL OR :hakukohde_oid = ANY (hakukohde))
  AND (array_length(ARRAY[:application_keys], 1) < 2 OR key IN (:application_keys))
  AND state <> 'inactivated';

--name: yesql-siirto-applications
SELECT
  a.key,
  a.person_oid AS "person-oid",
  a.haku,
  a.hakukohde AS hakutoiveet,
  a.content,
  a.lang,
  lf.organization_oid AS "organization-oid"
FROM latest_applications AS a
JOIN application_reviews AS ar ON ar.application_key = a.key
JOIN forms AS f ON form_id = f.id
JOIN latest_forms AS lf ON lf.key = f.key
WHERE a.person_oid IS NOT NULL
  AND (:hakukohde_oid::TEXT IS NULL OR :hakukohde_oid = ANY (a.hakukohde))
  AND (array_length(ARRAY[:application_keys], 1) < 2 OR a.key IN (:application_keys))
  AND ar.state <> 'inactivated';

--name: yesql-get-latest-application-ids-distinct-by-person-oid
SELECT DISTINCT ON (person_oid) id FROM latest_applications ORDER BY person_oid, id DESC;

--name: yesql-get-latest-application-secret
SELECT secret
FROM latest_applications
  JOIN latest_application_secrets ON latest_applications.key = latest_application_secrets.application_key
ORDER BY latest_applications.id DESC
LIMIT 1;

--name: yesql-set-application-hakukohteet-by-secret!
UPDATE applications
SET hakukohde = ARRAY [:hakukohde] :: CHARACTER VARYING(127) []
FROM application_secrets
WHERE application_secrets.secret = :secret AND application_secrets.application_key = applications.key;

--name: yesql-get-application-versions
SELECT content, form_id
FROM applications
WHERE key = :application_key
ORDER BY id ASC;

-- name: yesql-get-expiring-secrets-for-applications
SELECT *
FROM latest_application_secrets
WHERE application_key IN (:application_keys) AND created_time < now() - INTERVAL '29 days';
