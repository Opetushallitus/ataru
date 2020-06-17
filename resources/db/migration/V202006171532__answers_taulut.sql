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
    PRIMARY KEY (application_id, key)
);

CREATE TABLE multi_answer_values (
    application_id bigint NOT NULL,
    key text NOT NULL,
    data_idx int NOT NULL,
    value text NOT NULL,
    PRIMARY KEY (application_id, key, data_idx),
    FOREIGN KEY (application_id, key) REFERENCES multi_answers (application_id, key) ON DELETE CASCADE
);

CREATE TABLE group_answers (
    application_id bigint NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    key text NOT NULL,
    field_type text NOT NULL,
    PRIMARY KEY (application_id, key)
);

CREATE TABLE group_answer_groups (
    application_id bigint NOT NULL,
    key text NOT NULL,
    group_idx int NOT NULL,
    is_null boolean NOT NULL,
    PRIMARY KEY (application_id, key, group_idx),
    FOREIGN KEY (application_id, key) REFERENCES group_answers (application_id, key) ON DELETE CASCADE
);

CREATE TABLE group_answer_values (
    application_id bigint NOT NULL,
    key text NOT NULL,
    group_idx int NOT NULL,
    data_idx int NOT NULL,
    value text,
    PRIMARY KEY (application_id, key, group_idx, data_idx),
    FOREIGN KEY (application_id, key, group_idx) REFERENCES group_answer_groups (application_id, key, group_idx) ON DELETE CASCADE
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
                  field_type,
                  (SELECT coalesce(jsonb_agg(value ORDER BY data_idx ASC), '[]'::jsonb)
                   FROM multi_answer_values
                   WHERE application_id = ma.application_id AND
                         key = ma.key) AS value
           FROM multi_answers AS ma)
          UNION ALL
          (SELECT application_id,
                  key,
                  field_type,
                  (SELECT jsonb_agg(CASE
                                        WHEN gag.is_null THEN 'null'::jsonb
                                        ELSE (SELECT coalesce(jsonb_agg(value ORDER BY data_idx ASC), '[]'::jsonb)
                                              FROM group_answer_values
                                              WHERE application_id = gag.application_id AND
                                                    key = gag.key AND
                                                    group_idx = gag.group_idx)
                                    END ORDER BY group_idx ASC)
                   FROM group_answer_groups AS gag
                   WHERE application_id = ga.application_id AND
                         key = ga.key) AS value
           FROM group_answers AS ga)) AS t
    GROUP BY application_id;
