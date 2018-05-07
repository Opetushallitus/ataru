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
WITH latest_information_request_event AS (
    SELECT DISTINCT ON (application_key) * FROM application_events WHERE new_review_state = 'information-request' ORDER BY application_key, time DESC
), latest_modification_by_applicant AS (
    SELECT * FROM application_events WHERE event_type = 'updated-by-applicant' ORDER BY application_key, time DESC
), new_application_modifications AS (
    SELECT up.application_key
    FROM latest_information_request_event ir
      JOIN latest_modification_by_applicant up ON ir.application_key = up.application_key
    WHERE ir.time < up.time
)
SELECT
  a.id,
  a.person_oid,
  a.key,
  a.lang,
  a.preferred_name,
  a.last_name,
  a.created_time,
  a.haku,
  a.hakukohde,
  ar.state                            AS state,
  ar.score                            AS score,
  a.form_id                           AS form,
  lf.organization_oid,
  (SELECT json_agg(json_build_object('requirement', requirement,
                                     'state', state,
                                     'hakukohde', hakukohde))
   FROM application_hakukohde_reviews ahr
   WHERE ahr.application_key = a.key) AS application_hakukohde_reviews,
  (SELECT json_agg(json_build_object('attachment_key', attachment_key,
                                     'state', state,
                                     'hakukohde', hakukohde))
   FROM application_hakukohde_attachment_reviews aar
   WHERE aar.application_key = a.key
     AND (aar.hakukohde = 'form' OR aar.hakukohde = ANY (a.hakukohde))) AS application_attachment_reviews,
  (SELECT COUNT(*)
   FROM new_application_modifications am
   WHERE am.application_key = a.key)  AS new_application_modifications,
  (SELECT created_time
   FROM applications
   WHERE a.key = key
   ORDER BY id ASC
   LIMIT 1) AS original_created_time
FROM latest_applications AS a
  JOIN application_reviews AS ar ON a.key = ar.application_key
  JOIN forms AS f ON a.form_id = f.id
  JOIN latest_forms AS lf ON lf.key = f.key
WHERE
  CASE
  WHEN :query_key = 'form'
    THEN (lf.key = :query_value AND a.haku IS NULL)
  WHEN :query_key = 'application-oid'
    THEN a.key = :query_value
  WHEN :query_key = 'person-oid'
    THEN a.person_oid = :query_value
  WHEN :query_key = 'name'
    THEN to_tsvector('simple', a.preferred_name || ' ' || a.last_name) @@ to_tsquery(:query_value)
  WHEN :query_key = 'email'
    THEN lower(a.email) = lower(:query_value)
  WHEN :query_key = 'dob'
    THEN a.dob = to_date(:query_value, 'DD.MM.YYYY')
  WHEN :query_key = 'ssn'
    THEN a.ssn = :query_value
  WHEN :query_key = 'haku'
    THEN a.haku = :query_value
  WHEN :query_key = 'hakukohde'
    THEN :query_value = ANY (a.hakukohde)
  WHEN :query_key = 'ensisijainen-hakukohde'
    THEN :query_value = a.hakukohde[1]
  END
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
FROM latest_applications AS a
  JOIN application_reviews ar ON a.key = ar.application_key
  JOIN latest_application_secrets las ON a.key = las.application_key
WHERE a.person_oid = :person_oid
      AND a.haku IS NOT NULL
      AND ar.state <> 'inactivated'
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
JOIN virkailija_credentials AS vc ON vc.application_key = a.key
WHERE vc.secret = :virkailija_secret;

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
WITH latest_version AS (
    SELECT
      a.id AS latest_id,
      key
    FROM applications a
      JOIN virkailija_credentials AS vc
        ON a.key = vc.application_key
    WHERE vc.secret = :virkailija_secret
    ORDER BY id DESC
    LIMIT 1
), latest_secret_version AS (
    SELECT
      ass.secret AS latest_secret,
      ass.application_key
    FROM application_secrets ass
    WHERE ass.application_key = (SELECT key
                                 FROM latest_version)
    ORDER BY ass.id DESC
    LIMIT 1
)
SELECT
  a.id,
  a.key,
  las.latest_secret AS secret,
  a.lang,
  a.form_id         AS form,
  a.created_time,
  a.content,
  a.haku,
  a.hakukohde,
  a.person_oid
FROM applications a
  JOIN latest_version lv ON a.id = lv.latest_id
  JOIN latest_secret_version las ON las.application_key = a.key
FOR UPDATE;

-- name: yesql-add-application-event<!
-- Add application event
INSERT INTO application_events (application_key, event_type, new_review_state, virkailija_oid, hakukohde, review_key)
VALUES (:application_key, :event_type, :new_review_state, :virkailija_oid, :hakukohde, :review_key);

-- name: yesql-add-application-review!
-- Add application review, initially it doesn't have all fields. This is just a "skeleton"
INSERT INTO application_reviews (application_key, state) VALUES (:application_key, :state);

-- name: yesql-save-attachment-review!
-- Add not-checked state to new application attachments
INSERT INTO application_hakukohde_attachment_reviews (application_key, attachment_key, hakukohde, state)
VALUES (:application_key, :attachment_key, :hakukohde, :state);

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
WITH filtered_applications AS (
    SELECT
      a.key       AS key,
      a.form_id   AS form_id,
      a.haku      AS haku,
      a.hakukohde AS hakukohde,
      ar.state    AS state
    FROM latest_applications AS a
      JOIN application_reviews AS ar ON a.key = ar.application_key
    WHERE a.haku IS NOT NULL
), unnested_hakukohde AS (
    SELECT
      key,
      haku,
      unnest(hakukohde) AS hakukohde,
      state
    FROM filtered_applications
), haku_counts AS (
    SELECT
      haku,
      max(form_id) AS latest_application_form_id,
      count(key) AS application_count
    FROM filtered_applications
    GROUP BY haku
), unnested_hakukohde_with_processing_states AS (
    SELECT
      key,
      haku,
      unnested_hakukohde.hakukohde              AS hakukohde,
      unnested_hakukohde.state                  AS application_state,
      application_hakukohde_reviews.requirement AS hakukohde_review_requirement,
      application_hakukohde_reviews.state       AS hakukohde_review_state
    FROM unnested_hakukohde
      LEFT JOIN application_hakukohde_reviews
        ON unnested_hakukohde.key = application_hakukohde_reviews.application_key AND
           unnested_hakukohde.hakukohde = application_hakukohde_reviews.hakukohde AND
           application_hakukohde_reviews.requirement = 'processing-state'
), haku_review_complete_counts AS (
    SELECT
      haku,
      hakukohde,
      count(DISTINCT (key)) AS processed
    FROM unnested_hakukohde_with_processing_states
    WHERE
      hakukohde_review_state = 'processed'
      OR application_state = 'inactivated'
    GROUP BY haku, hakukohde
), haku_review_processing_counts AS (
    SELECT
      haku,
      hakukohde,
      count(DISTINCT (key)) AS processing
    FROM unnested_hakukohde_with_processing_states
    WHERE hakukohde_review_state NOT IN ('unprocessed', 'processed') AND
          application_state != 'inactivated'
    GROUP BY haku, hakukohde
)
SELECT
  unnested_hakukohde.haku,
  unnested_hakukohde.hakukohde,
  (SELECT lf.organization_oid
   FROM latest_forms AS lf
   JOIN forms AS f ON f.key = lf.key
   WHERE f.id = max(haku_counts.latest_application_form_id)) AS organization_oid,
  max(haku_counts.application_count)                         AS haku_application_count,
  count(DISTINCT (unnested_hakukohde.key))                   AS application_count,
  coalesce(max(haku_review_complete_counts.processed), 0)    AS processed,
  coalesce(max(haku_review_processing_counts.processing), 0) AS processing
FROM unnested_hakukohde
  JOIN haku_counts ON haku_counts.haku = unnested_hakukohde.haku
  LEFT JOIN haku_review_complete_counts ON haku_review_complete_counts.hakukohde = unnested_hakukohde.hakukohde
  LEFT JOIN haku_review_processing_counts ON haku_review_processing_counts.hakukohde = unnested_hakukohde.hakukohde
GROUP BY unnested_hakukohde.haku, unnested_hakukohde.hakukohde;

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

-- name: yesql-get-hakija-secret-by-virkailija-secret
SELECT las.secret
FROM latest_application_secrets AS las
  JOIN virkailija_credentials AS vc ON vc.application_key = las.application_key
WHERE vc.secret = :virkailija_secret;

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
  reviews.hakukohde
FROM application_hakukohde_attachment_reviews AS reviews
  JOIN latest_applications AS applications ON application_key = key
WHERE reviews.application_key = :application_key
      AND (reviews.hakukohde = 'form' OR reviews.hakukohde = ANY (applications.hakukohde));

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
UPDATE application_hakukohde_attachment_reviews
SET state = :state, modified_time = now()
WHERE application_key = :application_key AND attachment_key = :attachment_key AND hakukohde = :hakukohde;

-- name: yesql-get-existing-attachment-review
SELECT *
FROM application_hakukohde_attachment_reviews
WHERE application_key = :application_key AND attachment_key = :attachment_key AND hakukohde = :hakukohde;

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

--name: yesql-get-applciations-by-created-time
SELECT key, haku, hakukohde, person_oid, content
FROM latest_applications
WHERE created_time > :date::DATE
AND person_oid IS NOT NULL
ORDER BY created_time DESC;

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
