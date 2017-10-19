SELECT id, key INTO application_key_backup FROM applications;
SELECT id, application_key INTO application_events_key_backup FROM application_events;
SELECT id, application_key INTO application_hakukohde_reviews_key_backup FROM application_hakukohde_reviews;
SELECT id, application_key INTO application_reviews_key_backup FROM application_reviews;

CREATE SEQUENCE application_oid;

SELECT DISTINCT ON (key) key, '1.2.246.562.8.' || lpad(nextval('application_oid')::text, 20, '0') as oid
INTO TEMPORARY key_oid FROM applications ORDER BY key, id;

UPDATE applications SET key = (SELECT oid FROM key_oid WHERE key_oid.key = applications.key);

ALTER TABLE applications
  ALTER COLUMN key SET DEFAULT '1.2.246.562.11.' || lpad(nextval('application_oid')::text, 20, '0'),
  ALTER COLUMN key SET NOT NULL;

UPDATE application_events
SET application_key = (select oid from key_oid where application_key = key);

UPDATE application_hakukohde_reviews
SET application_key = (select oid from key_oid where application_key = key);

UPDATE application_reviews
SET application_key = (select oid from key_oid where application_key = key);