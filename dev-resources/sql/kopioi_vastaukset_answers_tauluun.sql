INSERT INTO answers (application_id, key, field_type, value)
SELECT id, t->>'key', t->>'fieldType', t->'value'->>0
FROM applications
CROSS JOIN jsonb_array_elements(content->'answers') AS t
WHERE jsonb_typeof(t->'value') = 'string' OR
      jsonb_typeof(t->'value') = 'null'
ON CONFLICT (application_id, key) DO NOTHING;

INSERT INTO multi_answers (application_id, key, field_type)
SELECT id, t->>'key', t->>'fieldType'
FROM applications
CROSS JOIN jsonb_array_elements(content->'answers') AS t
WHERE jsonb_typeof(t->'value') = 'array' AND
      (jsonb_array_length(t->'value') = 0 OR
       jsonb_typeof(t->'value'->0) = 'string')
ON CONFLICT (application_id, key) DO NOTHING;

INSERT INTO multi_answer_values (application_id, key, data_idx, value)
SELECT id, t->>'key', tt.data_idx, tt.value->>0
FROM applications
CROSS JOIN jsonb_array_elements(content->'answers') AS t
CROSS JOIN jsonb_array_elements(t->'value') WITH ORDINALITY AS tt(value, data_idx)
WHERE jsonb_typeof(t->'value') = 'array' AND
      jsonb_typeof(t->'value'->0) = 'string'
ON CONFLICT (application_id, key, data_idx) DO NOTHING;

INSERT INTO group_answers (application_id, key, field_type)
SELECT id, t->>'key', t->>'fieldType'
FROM applications
CROSS JOIN jsonb_array_elements(content->'answers') AS t
WHERE jsonb_typeof(t->'value') = 'array' AND
      (jsonb_typeof(t->'value'->0) = 'array' OR
       jsonb_typeof(t->'value'->0) = 'null')
ON CONFLICT (application_id, key) DO NOTHING;

INSERT INTO group_answer_groups (application_id, key, group_idx, is_null)
SELECT id, t->>'key', tt.group_idx, jsonb_typeof(tt.value) = 'null'
FROM applications
CROSS JOIN jsonb_array_elements(content->'answers') AS t
CROSS JOIN jsonb_array_elements(t->'value') WITH ORDINALITY AS tt(value, group_idx)
WHERE jsonb_typeof(t->'value') = 'array' AND
      (jsonb_typeof(t->'value'->0) = 'array' OR
       jsonb_typeof(t->'value'->0) = 'null')
ON CONFLICT (application_id, key, group_idx) DO NOTHING;

INSERT INTO group_answer_values (application_id, key, group_idx, data_idx, value)
SELECT id, t->>'key', tt.group_idx, ttt.data_idx, ttt.value->>0
FROM applications
CROSS JOIN jsonb_array_elements(content->'answers') AS t
CROSS JOIN jsonb_array_elements(t->'value') WITH ORDINALITY AS tt(group_value, group_idx)
CROSS JOIN jsonb_array_elements(CASE jsonb_typeof(tt.group_value)
                                    WHEN 'array' THEN tt.group_value
                                    ELSE '[]'::jsonb
                                END) WITH ORDINALITY AS ttt(value, data_idx)
WHERE jsonb_typeof(t->'value') = 'array' AND
      (jsonb_typeof(t->'value'->0) = 'array' OR
       jsonb_typeof(t->'value'->0) = 'null')
ON CONFLICT (application_id, key, group_idx, data_idx) DO NOTHING;

CREATE INDEX IF NOT EXISTS answers_key_value_idx
ON answers (key, value) WHERE (value IS NULL OR char_length(value) < 1000);

CREATE INDEX IF NOT EXISTS multi_answer_values_key_value_idx
ON multi_answer_values (key, value) WHERE char_length(value) < 1000;

CREATE INDEX IF NOT EXISTS group_answer_values_key_value_idx
ON group_answer_values (key, value) WHERE (value IS NULL OR char_length(value) < 1000);
