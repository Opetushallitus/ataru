INSERT INTO answers (application_id, key, field_type, value)
SELECT id, t->>'key', t->>'fieldType', t->'value'->>0
FROM applications
CROSS JOIN jsonb_array_elements(content->'answers') AS t
WHERE jsonb_typeof(t->'value') = 'string' OR
      jsonb_typeof(t->'value') = 'null'
ON CONFLICT (application_id, key) DO NOTHING;

INSERT INTO multi_answers (application_id, key, field_type, data_idx, value)
SELECT id, t->>'key', t->>'fieldType', tt.data_idx, tt.value->>0
FROM applications
CROSS JOIN jsonb_array_elements(content->'answers') AS t
CROSS JOIN jsonb_array_elements(t->'value') WITH ORDINALITY AS tt(value, data_idx)
WHERE jsonb_typeof(t->'value') = 'array' AND
      jsonb_typeof(tt.value) = 'string'
ON CONFLICT (application_id, key, data_idx) DO NOTHING;

INSERT INTO group_answers (application_id, key, field_type, group_idx, data_idx, value)
SELECT id, key, field_type, group_idx, data_idx, value
FROM ((SELECT id,
              t->>'key' AS key,
              t->>'fieldType' AS field_type,
              tt.group_idx AS group_idx,
              ttt.data_idx AS data_idx,
              ttt.value->>0 AS value
       FROM applications
       CROSS JOIN jsonb_array_elements(content->'answers') AS t
       CROSS JOIN jsonb_array_elements(t->'value') WITH ORDINALITY AS tt(group_value, group_idx)
       CROSS JOIN jsonb_array_elements(tt.group_value) WITH ORDINALITY AS ttt(value, data_idx)
       WHERE jsonb_typeof(t->'value') = 'array' AND
             jsonb_typeof(tt.group_value) = 'array')
      UNION ALL
      (SELECT id,
              t->>'key' AS key,
              t->>'fieldType' AS field_type,
              tt.group_idx AS group_idx,
              0 AS data_idx,
              null AS value
       FROM applications
       CROSS JOIN jsonb_array_elements(content->'answers') AS t
       CROSS JOIN jsonb_array_elements(t->'value') WITH ORDINALITY AS tt(group_value, group_idx)
       WHERE jsonb_typeof(t->'value') = 'array' AND
             jsonb_typeof(tt.group_value) = 'null')) AS t
ON CONFLICT (application_id, key, group_idx, data_idx) DO NOTHING;
