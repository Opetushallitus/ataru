DROP VIEW latest_applications;

ALTER TABLE applications
  ADD COLUMN submitted timestamp with time zone;

UPDATE applications
SET submitted = (SELECT created_time
                 FROM applications AS fa
                 WHERE fa.key = applications.key
                 ORDER BY id ASC
                 LIMIT 1);

ALTER TABLE applications
  ALTER COLUMN submitted SET NOT NULL;

CREATE VIEW latest_applications AS
SELECT DISTINCT ON (key) * FROM applications ORDER BY key, id DESC;
