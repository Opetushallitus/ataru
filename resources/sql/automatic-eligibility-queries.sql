-- name: yesql-get-application
SELECT key AS key,
       person_oid AS "person-oid",
       haku AS "haku-oid",
       hakukohde AS "hakukohde-oids",
       form_id AS "form-id"
FROM applications
WHERE id = (SELECT max(id) FROM applications WHERE id = :id);

-- name: yesql-from-unreviewed-to-eligible!
INSERT INTO application_hakukohde_reviews
(application_key, requirement, state, hakukohde)
VALUES (:application_key, 'eligibility-state', 'eligible', :hakukohde)
ON CONFLICT (application_key, hakukohde, requirement)
DO UPDATE
SET state = EXCLUDED.state,
    modified_time = DEFAULT
WHERE application_hakukohde_reviews.state = 'unreviewed';

-- name: yesql-from-eligible-to-unreviewed!
UPDATE application_hakukohde_reviews
SET state = 'unreviewed',
    modified_time = DEFAULT
FROM application_events
WHERE application_events.id = (SELECT max(id)
                               FROM application_events
                               WHERE application_events.application_key = :application_key AND
                                     application_events.hakukohde = :hakukohde AND
                                     application_events.review_key = 'eligibility-state') AND
      application_events.event_type = 'eligibility-state-automatically-changed' AND
      application_hakukohde_reviews.application_key = :application_key AND
      application_hakukohde_reviews.hakukohde = :hakukohde AND
      application_hakukohde_reviews.requirement = 'eligibility-state' AND
      application_hakukohde_reviews.state = 'eligible';

-- name: yesql-insert-eligibility-state-automatically-changed-event!
INSERT INTO application_events
(new_review_state, event_type, application_key, hakukohde, review_key)
VALUES (:state,
        'eligibility-state-automatically-changed',
        :application_key,
        :hakukohde,
        'eligibility-state');

-- name: yesql-get-application-ids
SELECT a.id
FROM applications AS a
WHERE a.id = (SELECT max(id) FROM applications WHERE key = a.key) AND
      a.person_oid IN (:person_oids);
