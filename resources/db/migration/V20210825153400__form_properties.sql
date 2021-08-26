DROP VIEW latest_forms;

ALTER TABLE forms ADD COLUMN properties JSONB NOT NULL DEFAULT '{}'::jsonb;
COMMENT ON COLUMN forms.properties IS 'Lomakkeen asetukset';

CREATE VIEW latest_forms AS
SELECT DISTINCT ON (key) * FROM forms ORDER BY key, id DESC;
