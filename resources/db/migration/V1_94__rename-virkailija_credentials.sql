CREATE TABLE virkailija_update_secrets (
  virkailija_oid text NOT NULL REFERENCES virkailija(oid),
  application_key text NOT NULL,
  secret text NOT NULL UNIQUE,
  valid tstzrange NOT NULL DEFAULT tstzrange(now(), now() + INTERVAL '1 hour', '[)')
);

CREATE INDEX ON virkailija_update_secrets (application_key);

INSERT INTO virkailija_update_secrets
(virkailija_oid, application_key, secret, valid)
SELECT oid,
       application_key,
       secret,
       CASE WHEN valid
         THEN tstzrange(created_time, created_time + INTERVAL '1 hour', '[)')
         ELSE tstzrange(created_time, now(), '[)')
       END
FROM virkailija_credentials;
