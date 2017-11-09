CREATE TABLE information_requests (
  id BIGSERIAL PRIMARY KEY,
  application_key VARCHAR(40) NOT NULL,
  subject TEXT NOT NULL,
  message TEXT NOT NULL,
  created_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  virkailija_oid VARCHAR(50) NOT NULL REFERENCES virkailija(oid)
);

CREATE INDEX information_requests_application_key_idx ON information_requests (application_key);
