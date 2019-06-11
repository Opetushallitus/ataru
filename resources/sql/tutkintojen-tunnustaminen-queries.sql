-- name: yesql-get-application
SELECT a.id,
       a.key,
       f.key AS "form-key",
       a.person_oid AS "person-oid",
       a.submitted,
       a.lang AS "lang",
       ((SELECT value->>'value'
         FROM jsonb_array_elements(a.content->'answers')
         WHERE value->>'key' = 'last-name') || ' ' ||
        (SELECT value->>'value'
         FROM jsonb_array_elements(a.content->'answers')
         WHERE value->>'key' = 'first-name')) AS "name",
       (SELECT value->>'value'
        FROM jsonb_array_elements(a.content->'answers')
        WHERE value->>'key' = :country_question_id) AS "country",
       (SELECT jsonb_agg(attachment_key)
        FROM jsonb_array_elements(a.content->'answers') AS answers
        JOIN LATERAL jsonb_array_elements(answers->'value') AS s_or_a ON true
        JOIN LATERAL jsonb_array_elements(CASE jsonb_typeof(s_or_a)
                                            WHEN 'string' THEN jsonb_build_array(s_or_a)
                                            WHEN 'array' THEN s_or_a
                                          END) AS attachment_key ON true
        WHERE answers->>'fieldType' = 'attachment') AS "attachment-keys",
       a.content::text
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
