ALTER TABLE application_events ADD COLUMN virkailija_oid VARCHAR(50) REFERENCES virkailija(oid);
