ALTER TABLE application_events
  ALTER COLUMN new_review_state TYPE text,
  ALTER COLUMN event_type TYPE text,
  ALTER COLUMN application_key TYPE text,
  ALTER COLUMN hakukohde TYPE text,
  ALTER COLUMN review_key TYPE text;
