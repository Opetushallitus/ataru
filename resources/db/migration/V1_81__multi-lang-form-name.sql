ALTER TABLE forms ADD COLUMN multi_lang_name jsonb;

UPDATE forms AS f
SET multi_lang_name = (SELECT jsonb_object_agg(lang, name)
                       FROM (SELECT name,
                                    jsonb_array_elements_text(languages->'languages') as lang
                             FROM forms
                             WHERE id = f.id) AS t);

DROP VIEW latest_forms;

ALTER TABLE forms DROP COLUMN name;
ALTER TABLE forms RENAME COLUMN multi_lang_name TO name;

CREATE VIEW latest_forms AS
SELECT DISTINCT ON (key) * FROM forms ORDER BY key, id DESC;
