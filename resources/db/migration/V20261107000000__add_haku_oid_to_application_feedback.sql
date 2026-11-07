ALTER TABLE application_feedback ADD COLUMN IF NOT EXISTS haku_oid TEXT;

COMMENT ON COLUMN application_feedback.haku_oid IS 'Haun OID jonka kautta lomaketta täytettiin, jos lomake oli haun käytössä. Tyhjä, jos palaute on annettu hausta riippumattomalla lomakkeella tai ennen kuin haun OID:tä alettiin tallentaa.';

CREATE INDEX IF NOT EXISTS application_feedback_form_key_idx ON application_feedback (form_key);
