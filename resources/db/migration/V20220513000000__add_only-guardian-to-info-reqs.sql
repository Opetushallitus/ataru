ALTER TABLE information_requests
    ADD COLUMN only_guardian BOOLEAN NOT NULL DEFAULT false;
