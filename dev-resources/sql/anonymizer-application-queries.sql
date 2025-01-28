-- name: sql-get-all-applications
SELECT id FROM applications ORDER BY id;

-- name: sql-delete-application!
DELETE FROM applications WHERE id = :id;

-- name: sql-get-application
SELECT id, person_oid, tunnistautuminen, content
FROM applications
WHERE id = :id;

-- name: sql-update-application!
UPDATE applications
SET preferred_name = :preferred_name,
    last_name = :last_name,
    ssn = :ssn,
    email = :email,
    dob = :dob::DATE,
    tunnistautuminen = :tunnistautuminen,
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

-- name: sql-update-multi-by-key!
UPDATE multi_answer_values SET value = :val WHERE key = :key;

-- name: sql-anonymize-long-textareas-group!
WITH anonymisoitavat AS (
    SELECT gav.application_id,
           gav.key
    FROM group_answer_values gav
         JOIN group_answers ga ON gav.application_id = ga.application_id AND gav.key = ga.key
    WHERE field_type IN ('textArea', 'textField')
      AND gav.value !~ '(^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$)|(^[0-9]{1,2}[.][0-9]{1,2}[.][0-9]{4}$)'
      AND length(gav.value) >= 6
)
UPDATE group_answer_values gav
SET value = substring('Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris ' ||
                      'nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, ' ||
                      'sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut ' ||
                      'enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat ' ||
                      'nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do ' ||
                      'eiusmod tempor incididunt ut labore et dolore magna aliqua.' FROM 0 FOR length(gav.value)+1)
FROM anonymisoitavat a
WHERE gav.application_id = a.application_id
  AND gav.key = a.key;

-- name: sql-anonymize-long-textareas-multi!
WITH anonymisoitavat AS (
    SELECT mav.application_id,
           mav.key
    FROM multi_answer_values mav
         JOIN multi_answers ma ON mav.application_id = ma.application_id AND mav.key = ma.key
    WHERE field_type IN ('textArea', 'textField')
      AND ma.key NOT IN ('guardian-phone', 'guardian-firstname', 'guardian-lastname', 'guardian-name', 'guardian-email', 'guardian-phone-secondary', 'guardian-firstname-secondary', 'guardian-lastname-secondary', 'guardian-name-secondary', 'guardian-email-secondary')
      AND mav.value !~ '(^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$)|(^[0-9]{1,2}[.][0-9]{1,2}[.][0-9]{4}$)'
      AND length(mav.value) >= 6
)
UPDATE multi_answer_values mav
SET value = substring('Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris ' ||
                      'nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, ' ||
                      'sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut ' ||
                      'enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat ' ||
                      'nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do ' ||
                      'eiusmod tempor incididunt ut labore et dolore magna aliqua.' FROM 0 FOR length(mav.value)+1)
FROM anonymisoitavat a
WHERE mav.application_id = a.application_id
  AND mav.key = a.key;

-- name: sql-anonymize-long-textareas!
UPDATE answers ans
SET value = substring('Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris ' ||
                      'nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, ' ||
                      'sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut ' ||
                      'enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat ' ||
                      'nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do ' ||
                      'eiusmod tempor incididunt ut labore et dolore magna aliqua.' FROM 0 FOR length(ans.value)+1)
WHERE field_type IN ('textArea', 'textField')
  AND ans.key NOT IN ('gender', 'first-name', 'birth-date', 'home-town', 'ssn', 'email', 'preferred-name', 'last-name', 'address', 'phone', 'postal-office', 'postal-code')
  AND ans.value !~ '(^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$)|(^[0-9]{1,2}[.][0-9]{1,2}[.][0-9]{4}$)'
  AND length(ans.value) >= 6;
