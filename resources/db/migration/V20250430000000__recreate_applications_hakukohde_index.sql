DROP INDEX IF EXISTS applications_hakukohde_idx;
CREATE INDEX applications_hakukohde_idx ON applications USING GIN (hakukohde);
