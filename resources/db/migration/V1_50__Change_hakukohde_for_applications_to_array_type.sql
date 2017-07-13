ALTER TABLE applications
  ALTER hakukohde TYPE VARCHAR(127) []
  USING
  CASE WHEN hakukohde IS NULL THEN '{}'
    ELSE ARRAY [hakukohde] :: VARCHAR(127) []
  END;
ALTER TABLE applications
  ALTER hakukohde SET NOT NULL;
