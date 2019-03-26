CREATE TABLE virkailija_rewrite_secrets (
  virkailija_oid text NOT NULL REFERENCES virkailija(oid),
  application_key text NOT NULL,
  secret text NOT NULL UNIQUE,
  valid tstzrange NOT NULL DEFAULT tstzrange(now(), now() + INTERVAL '1 hour', '[)')
);

CREATE INDEX ON virkailija_rewrite_secrets (application_key);
