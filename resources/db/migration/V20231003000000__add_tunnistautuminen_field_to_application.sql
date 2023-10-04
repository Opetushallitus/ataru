DROP VIEW latest_applications;

alter table applications add column if not exists tunnistautuminen jsonb default '{}'::jsonb not null;

CREATE VIEW latest_applications AS
SELECT DISTINCT ON (key) * FROM applications ORDER BY key, id DESC;
