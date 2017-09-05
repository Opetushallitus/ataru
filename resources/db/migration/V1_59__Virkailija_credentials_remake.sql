DROP TABLE virkailija_credentials;
CREATE TABLE virkailija_credentials (
  oid             VARCHAR(50)              NOT NULL REFERENCES virkailija (oid),
  application_key VARCHAR(50)              NOT NULL,
  secret          VARCHAR(36)              NOT NULL,
  valid           BOOLEAN                  NOT NULL DEFAULT TRUE,
  created_time    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  UNIQUE (application_key, secret)
);
