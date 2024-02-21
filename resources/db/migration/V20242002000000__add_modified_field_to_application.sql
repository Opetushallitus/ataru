DROP VIEW latest_applications;

alter table applications add column if not exists modified_time timestamp with time zone default now();
update applications set modified_time = created_time where modified_time is null;
COMMENT ON COLUMN applications.modified_time IS 'Milloin hakemukseen liittyvät tiedot (esimerkiksi oppijanumero) ovat viimeksi muuttuneet.';

CREATE VIEW latest_applications AS
SELECT DISTINCT ON (key) * FROM applications ORDER BY key, id DESC;
