CREATE TABLE orphaned_forms AS
  WITH moved_rows AS (
    DELETE FROM forms
    WHERE id NOT IN (SELECT id
                     FROM latest_forms) AND
          id NOT IN (SELECT form_id
                     FROM applications)
    RETURNING *
  )
  SELECT * FROM moved_rows;