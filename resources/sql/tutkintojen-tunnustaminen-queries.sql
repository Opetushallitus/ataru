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

-- name: yesql-get-application-by-event-id
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
                    WHERE value->>'fieldType' = 'attachment') AS t) AS tt) AS "attachment-keys",
       e.new_review_state AS state
FROM application_events AS e
JOIN applications AS a ON a.key = e.application_key
JOIN forms AS f ON f.id = a.form_id
WHERE e.id = :id AND
      a.id = (SELECT max(id)
              FROM applications
              WHERE key = e.application_key AND
                    created_time < e.time);
