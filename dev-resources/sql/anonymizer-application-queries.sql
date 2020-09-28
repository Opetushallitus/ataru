-- name: sql-get-all-applications
SELECT id FROM applications;

-- name: sql-get-application
SELECT id, person_oid, content
FROM applications
WHERE id = :id;

-- name: sql-update-application!
UPDATE applications
SET preferred_name = :preferred_name,
    last_name = :last_name,
    ssn = :ssn,
    email = :email,
    dob = :dob::DATE,
    content = :content
WHERE id = :id;

-- name: sql-update-application-answers!
UPDATE answers
SET value = t.value
FROM (SELECT t->>'key' AS key, t->>'value' AS value
      FROM jsonb_array_elements(:answers) AS t
      WHERE jsonb_typeof(t->'value') = 'string' OR
            jsonb_typeof(t->'value') = 'null') AS t
WHERE answers.application_id = :application_id AND
      answers.key = t.key;

-- name: sql-update-application-multi-answer-values!
UPDATE multi_answer_values
SET value = t.value
FROM (SELECT t->>'key' AS key, tt.data_idx AS data_idx, tt.value->>0 AS value
      FROM jsonb_array_elements(:answers) AS t
      CROSS JOIN jsonb_array_elements(t->'value') WITH ORDINALITY AS tt(value, data_idx)
      WHERE jsonb_typeof(t->'value') = 'array' AND
            jsonb_typeof(t->'value'->0) = 'string') AS t
WHERE multi_answer_values.application_id = :application_id AND
      multi_answer_values.key = t.key AND
      multi_answer_values.data_idx = t.data_idx;

-- name: sql-update-application-group-answer-values!
UPDATE group_answer_values
SET value = t.value
FROM (SELECT t->>'key' AS key, tt.group_idx AS group_idx, ttt.data_idx AS data_idx, ttt.value->>0 AS value
      FROM jsonb_array_elements(:answers) AS t
      CROSS JOIN jsonb_array_elements(t->'value') WITH ORDINALITY AS tt(group_value, group_idx)
      CROSS JOIN jsonb_array_elements(CASE jsonb_typeof(tt.group_value)
                                          WHEN 'array' THEN tt.group_value
                                          ELSE '[]'::jsonb
                                      END) WITH ORDINALITY AS ttt(value, data_idx)
      WHERE jsonb_typeof(t->'value') = 'array' AND
            (jsonb_typeof(t->'value'->0) = 'array' OR
             jsonb_typeof(t->'value'->0) = 'null')) AS t
WHERE group_answer_values.application_id = :application_id AND
      group_answer_values.key = t.key AND
      group_answer_values.group_idx = t.group_idx AND
      group_answer_values.data_idx = t.data_idx;


-- name: sql-application-secret-ids
SELECT id FROM application_secrets;
