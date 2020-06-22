-- name: yesql-get-application
SELECT a.id,
       a.key,
       a.form_id AS "form-id",
       f.key AS "form-key",
       a.person_oid AS "person-oid",
       a.submitted,
       a.lang AS "lang",
       ((SELECT value
         FROM answers
         WHERE application_id = a.id AND
               key = 'last-name') || ' ' ||
        (SELECT value
         FROM answers
         WHERE application_id = a.id AND
               key = 'first-name')) AS "name",
       (SELECT value
        FROM answers
        WHERE application_id = a.id AND
              key = :country_question_id) AS "country",
       (SELECT jsonb_agg(t.value)
        FROM ((SELECT value
               FROM multi_answers
               WHERE application_id = a.id AND
                     field_type = 'attachment')
              UNION ALL
              (SELECT value
               FROM group_answers
               WHERE application_id = a.id AND
                     field_type = 'attachment' AND
                     value IS NOT NULL)) AS t) AS "attachment-keys",
       (SELECT content
        FROM answers_as_content
        WHERE application_id = a.id) AS content
FROM applications AS a
JOIN forms AS f ON f.id = a.form_id
WHERE a.id = :id;

-- name: yesql-get-application-id-and-state-by-event-id
SELECT a.id AS id,
       ae.review_key AS "review-key",
       ae.new_review_state AS state
FROM application_events AS ae
JOIN applications AS a ON a.key = ae.application_key
WHERE ae.id = :id AND
      a.created_time < ae.time
ORDER BY a.id DESC
LIMIT 1;
