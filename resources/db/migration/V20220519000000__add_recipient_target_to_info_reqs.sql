ALTER TABLE information_requests
    DROP COLUMN only_guardian;

ALTER TABLE information_requests
    ADD COLUMN recipient_target TEXT NOT NULL DEFAULT 'hakija';

COMMENT ON COLUMN information_requests.recipient_target IS 'Hakija, huoltajat tai hakija_ja_huoltajat. Vakiona hakija.';
