-- name: yesql-get-1_100-form-ids
SELECT id
FROM forms
WHERE deleted IS NULL OR NOT deleted
ORDER BY id ASC;

-- name: yesql-get-1_100-form
SELECT *
FROM forms
WHERE id = :id;

-- name: yesql-insert-1_100-form<!
INSERT INTO forms (name, content, created_by, key, languages, organization_oid, deleted, created_time)
VALUES (:name, :content, :created_by, :key, :languages, :organization_oid, :deleted, :created_time);

-- name: yesql-get-1_100-applications
SELECT *
FROM latest_applications
WHERE form_id = :form_id;

-- name: yesql-insert-1_100-application!
INSERT INTO applications
(key, lang, form_id, content, preferred_name, last_name, person_oid, hakukohde, haku, ssn, dob, email, created_time)
VALUES (:key, :lang, :form_id, :content, :preferred_name, :last_name, :person_oid,
              ARRAY [:hakukohde] :: CHARACTER VARYING(127) [], :haku, :ssn, :dob, :email, :created_time);

-- name: yesql-insert-1_100-application-event<!
INSERT INTO application_events (application_key, event_type, new_review_state, virkailija_oid, hakukohde, review_key)
VALUES (:application_key, :event_type, :new_review_state, :virkailija_oid, :hakukohde, :review_key);
