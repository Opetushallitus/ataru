CREATE TABLE answers (
    application_id bigint NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    key text NOT NULL,
    field_type text NOT NULL,
    value text,
    PRIMARY KEY (application_id, key)
);

CREATE TABLE multi_answers (
    application_id bigint NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    key text NOT NULL,
    field_type text NOT NULL,
    data_idx int NOT NULL,
    value text NOT NULL,
    PRIMARY KEY (application_id, key, data_idx)
);

CREATE TABLE group_answers (
    application_id bigint NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    key text NOT NULL,
    field_type text NOT NULL,
    group_idx int NOT NULL,
    data_idx int NOT NULL,
    value text,
    PRIMARY KEY (application_id, key, group_idx, data_idx)
);

CREATE VIEW answers_as_content AS
    SELECT application_id,
           jsonb_build_object('answers', jsonb_agg(jsonb_build_object('key', key,
                                                                      'fieldType', field_type,
                                                                      'value', value))) AS content
    FROM ((SELECT application_id,
                  key,
                  field_type,
                  to_jsonb(value) AS value
           FROM answers)
          UNION ALL
          (SELECT application_id,
                  key,
                  max(field_type) AS field_type,
                  jsonb_agg(value ORDER BY data_idx ASC) AS value
           FROM multi_answers
           GROUP BY application_id, key)
          UNION ALL
          (SELECT application_id,
                  key,
                  max(field_type) AS field_type,
                  jsonb_agg(group_value ORDER BY group_idx ASC) AS value
           FROM (SELECT application_id,
                        key,
                        max(field_type) AS field_type,
                        group_idx,
                        jsonb_agg(value ORDER BY data_idx ASC) FILTER (WHERE data_idx > 0) AS group_value
                 FROM group_answers
                 GROUP BY application_id, key, group_idx) AS t
           GROUP BY application_id, key)) AS t
    GROUP BY application_id;
