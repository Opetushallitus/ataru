-- :snip ensisijainen-hakukohde-snip
/*~ (if (contains? params :ensisijaisesti-hakukohteissa) */
AND (SELECT t.h
     FROM unnest(a.hakukohde) WITH ORDINALITY t(h, i)
     WHERE t.h = ANY (:ensisijaisesti-hakukohteissa)
     ORDER BY t.i ASC
     LIMIT 1) = ANY (:ensisijainen-hakukohde)
/*~*/
AND a.hakukohde[1] = ANY (:ensisijainen-hakukohde)
/*~   ) ~*/

-- :snip attachment-snip
AND EXISTS (SELECT 1
            FROM application_hakukohde_attachment_reviews
            WHERE application_key = a.key
              AND attachment_key = :attachment-key
/*~ (when (contains? params :states) */
              AND state = ANY (:states)
/*~   ) ~*/
/*~ (when (contains? params :hakukohde) */
              AND hakukohde = ANY (:hakukohde)
/*~   ) ~*/
           )

-- :snip option-answers-snip
AND ((EXISTS (SELECT 1
              FROM answers
              WHERE application_id = a.id
                AND key = :key
                AND (value IS NULL OR char_length(value) < 1000)
/*~ (if (contains? params :options) */
                AND value = ANY (:options)))
/*~*/
                AND (value IS NULL OR value = '')))
/*~   ) ~*/
     OR
/*~ (if (contains? params :options) */
     (EXISTS (SELECT 1
              FROM multi_answer_values
              WHERE application_id = a.id
                AND key = :key
                AND char_length(value) < 1000
                AND value = ANY (:options)))
/*~*/
     ((EXISTS (SELECT 1
               FROM multi_answers
               WHERE application_id = a.id
                 AND key = :key)) AND
      (NOT EXISTS (SELECT 1
                   FROM multi_answer_values
                   WHERE application_id = a.id
                     AND key = :key)))
/*~   ) ~*/
     OR
/*~ (if (contains? params :options) */
     (EXISTS (SELECT 1
              FROM group_answer_values
              WHERE application_id = a.id
                AND key = :key
                AND (value IS NULL OR char_length(value) < 1000)
                AND value = ANY (:options))))
/*~*/
     ((EXISTS (SELECT 1
               FROM group_answers
               WHERE application_id = a.id
                 AND key = :key)) AND
      (NOT EXISTS (SELECT 1
                   FROM group_answer_values
                   WHERE application_id = a.id
                     AND key = :key
                     AND value IS NOT NULL
                     AND value != ''))))
/*~   ) ~*/

-- :snip offset-snip
/*~ (case (:order-by params)
      "submitted" */
AND (date_trunc('second', a.submitted AT TIME ZONE 'Europe/Helsinki'), a.key) :sql:order (date_trunc('second', :submitted AT TIME ZONE 'Europe/Helsinki'), :key)
/*~   "created-time" */
AND (date_trunc('second', a.created_time AT TIME ZONE 'Europe/Helsinki'), a.key) :sql:order (date_trunc('second', :created-time AT TIME ZONE 'Europe/Helsinki'), :key)
/*~   "applicant-name" */
AND (a.last_name, a.preferred_name, a.key) :sql:order (:last-name COLLATE "fi_FI", :preferred-name COLLATE "fi_FI", :key)
/*~   ) ~*/

-- :snip order-by-snip
ORDER BY
/*~ (case (:order-by params)
      "submitted" */
  date_trunc('second', a.submitted AT TIME ZONE 'Europe/Helsinki') :sql:order, a.key :sql:order
/*~   "created-time" */
  date_trunc('second', a.created_time AT TIME ZONE 'Europe/Helsinki') :sql:order, a.key :sql:order
/*~   "applicant-name" */
  a.last_name COLLATE "fi_FI" :sql:order, a.preferred_name COLLATE "fi_FI" :sql:order, a.key :sql:order
/*~   ) ~*/

-- :name get-application-list :? :*
SELECT a.id,
       a.person_oid AS "person-oid",
       a.key,
       a.lang,
       a.preferred_name AS "preferred-name",
       a.last_name AS "last-name",
       a.created_time AS "created-time",
       a.submitted,
       a.form_id AS "form",
       a.haku,
       a.hakukohde,
       a.ssn,
       to_char(a.dob, 'DD.MM.YYYY') AS "dob",
       (SELECT array_agg(value ORDER BY data_idx ASC)
        FROM multi_answer_values
        WHERE application_id = a.id AND
              key = 'higher-completed-base-education') AS "base-education",
       (SELECT state
        FROM application_reviews
        WHERE application_key = a.key) AS "state",
       (SELECT score
        FROM application_reviews
        WHERE application_key = a.key) AS "score",
       (SELECT coalesce(array_agg(ae.hakukohde), '{}')
        FROM application_events AS ae
        LEFT JOIN application_events AS lae
          ON lae.application_key = ae.application_key AND
             lae.hakukohde = ae.hakukohde AND
             lae.review_key = ae.review_key AND
             lae.id > ae.id
        WHERE lae.id IS NULL AND
              ae.review_key = 'eligibility-state' AND
              ae.event_type = 'eligibility-state-automatically-changed' AND
              ae.application_key = a.key) AS "eligibility-set-automatically",
       (SELECT count(*)
        FROM application_events AS ae
        LEFT JOIN application_events AS lae
          ON lae.application_key = ae.application_key AND
             lae.hakukohde = ae.hakukohde AND
             lae.review_key = ae.review_key AND
             lae.id > ae.id
        WHERE lae.id IS NULL AND
              ae.review_key = 'processing-state' AND
              ae.new_review_state = 'information-request' AND
              ae.time < a.created_time AND
              ae.application_key = a.key) AS "new-application-modifications",
       (SELECT f.organization_oid
        FROM forms AS f
        LEFT JOIN forms AS lf
          ON lf.key = f.key AND
             lf.id > f.id
        WHERE lf.id IS NULL AND
              f.key = (SELECT key
                       FROM forms
                       WHERE id = a.form_id)) AS "organization-oid",
       (SELECT jsonb_agg(jsonb_build_object('requirement', requirement,
                                            'state', state,
                                            'hakukohde', hakukohde))
        FROM application_hakukohde_reviews
        WHERE application_key = a.key) AS "application-hakukohde-reviews",
       (SELECT jsonb_agg(jsonb_build_object('attachment-key', attachment_key,
                                            'state', state,
                                            'hakukohde', hakukohde))
        FROM application_hakukohde_attachment_reviews
        WHERE application_key = a.key) AS "application-attachment-reviews"
FROM applications AS a
LEFT JOIN applications AS la
  ON la.key = a.key AND
     la.id > a.id
WHERE la.id IS NULL
/*~ (when (contains? params :form) */
  AND a.haku IS NULL AND (SELECT key FROM forms WHERE id = a.form_id) = :form
/*~ ) ~*/
/*~ (when (contains? params :application-oid) */
  AND a.key = :application-oid
/*~ ) ~*/
/*~ (when (contains? params :person-oid) */
  AND a.person_oid = :person-oid
/*~ ) ~*/
/*~ (when (contains? params :name) */
  AND to_tsvector('unaccent_simple', a.preferred_name || a.last_name) @@ to_tsquery('unaccent_simple', :name)
/*~ ) ~*/
/*~ (when (contains? params :email) */
  AND lower(a.email) = lower(:email)
/*~ ) ~*/
/*~ (when (contains? params :dob) */
  AND a.dob = to_date(:dob, 'DD.MM.YYYY')
/*~ ) ~*/
/*~ (when (contains? params :ssn) */
  AND a.ssn = :ssn
/*~ ) ~*/
/*~ (when (contains? params :haku) */
  AND a.haku = :haku
/*~ ) ~*/
/*~ (when (contains? params :hakukohde) */
  AND a.hakukohde && :hakukohde
/*~ ) ~*/
--~ (when (contains? params :ensisijainen-hakukohde-snip) ":snip:ensisijainen-hakukohde-snip")
--~ (when (contains? params :attachment-snip) ":snip:attachment-snip")
--~ (when (contains? params :option-answers-snip) ":snip:option-answers-snip")
--~ (when (contains? params :offset-snip) ":snip:offset-snip")
:snip:order-by-snip
LIMIT 1000
