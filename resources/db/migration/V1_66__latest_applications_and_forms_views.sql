CREATE VIEW latest_applications AS
SELECT DISTINCT ON (key) * FROM applications ORDER BY key, id DESC;

CREATE VIEW latest_forms AS
SELECT DISTINCT ON (key) * FROM forms ORDER BY key, id DESC;
