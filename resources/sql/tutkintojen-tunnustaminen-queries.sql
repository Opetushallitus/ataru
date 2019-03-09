-- name: yesql-get-application
SELECT a.id,
       a.key,
       f.key AS "form-key",
       a.person_oid AS "person-oid",
       a.submitted,
       a.lang AS "lang",
       (SELECT value->'value'
        FROM jsonb_array_elements(a.content->'answers')
        WHERE value->>'key' = :country_question_id) AS "country",
       (SELECT jsonb_agg(tt.value)
        FROM (SELECT case jsonb_typeof(t.value)
                       when 'string' then t.value
                       when 'array' then jsonb_array_elements(t.value)
                     end AS value
              FROM (SELECT jsonb_array_elements(value->'value') AS value
                    FROM jsonb_array_elements(a.content->'answers')
                    WHERE value->>'fieldType' = 'attachment') AS t) AS tt) AS "attachment-keys"
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
