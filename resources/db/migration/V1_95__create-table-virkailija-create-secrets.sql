CREATE TABLE virkailija_create_secrets (
  virkailija_oid text NOT NULL REFERENCES virkailija(oid),
  secret text NOT NULL UNIQUE,
  valid tstzrange NOT NULL DEFAULT tstzrange(now(), now() + INTERVAL '1 hour', '[)')
);
