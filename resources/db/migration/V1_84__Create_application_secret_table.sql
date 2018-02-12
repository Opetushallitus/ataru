CREATE TABLE application_secrets (
  id              BIGSERIAL PRIMARY KEY,
  created_time    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  application_key VARCHAR(40)              NOT NULL,
  secret          VARCHAR(512)             NOT NULL UNIQUE
);

CREATE VIEW latest_application_secrets AS
  SELECT DISTINCT ON (application_key) *
  FROM application_secrets
  ORDER BY application_key, id DESC;

CREATE INDEX application_secrets_application_key_idx
  ON application_secrets (application_key);

CREATE INDEX application_secrets_application_key_id_idx
  ON application_secrets (application_key, id DESC);

INSERT INTO application_secrets (application_key, secret, created_time)
  SELECT
    key,
    secret,
    created_time
  FROM latest_applications
  WHERE secret IS NOT NULL;

DROP VIEW latest_applications;

ALTER TABLE applications
  DROP COLUMN secret;

CREATE VIEW latest_applications AS
  SELECT DISTINCT ON (key) *
  FROM applications
  ORDER BY key, id DESC;
