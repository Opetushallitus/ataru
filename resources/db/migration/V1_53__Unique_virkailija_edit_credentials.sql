ALTER TABLE virkailija_credentials ADD CONSTRAINT secret_application_key_idx UNIQUE(secret, application_key);
