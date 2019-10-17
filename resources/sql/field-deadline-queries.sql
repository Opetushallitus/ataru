-- name: yesql-get-field-deadlines
SELECT field_id AS "field-id",
       deadline,
       modified AS "last-modified"
FROM field_deadlines
WHERE application_key = :application_key;

-- name: yesql-get-field-deadline
SELECT field_id AS "field-id",
       deadline,
       modified AS "last-modified"
FROM field_deadlines
WHERE application_key = :application_key AND
      field_id = :field_id;

-- name: yesql-insert-field-deadline
INSERT INTO field_deadlines (application_key, field_id, deadline)
VALUES (:application_key, :field_id, :deadline)
RETURNING field_id AS "field-id",
          deadline,
          modified AS "last-modified";

-- name: yesql-update-field-deadline
UPDATE field_deadlines
SET deadline = :deadline,
    modified = DEFAULT
WHERE application_key = :application_key AND
      field_id = :field_id AND
      NOT modified > :if_unmodified_since::timestamptz
RETURNING field_id AS "field-id",
          deadline,
          modified AS "last-modified";

-- name: yesql-delete-field-deadline
DELETE FROM field_deadlines
WHERE application_key = :application_key AND
      field_id = :field_id AND
      NOT modified > :if_unmodified_since::timestamptz
RETURNING field_id AS "field-id",
          deadline,
          modified AS "last-modified";
