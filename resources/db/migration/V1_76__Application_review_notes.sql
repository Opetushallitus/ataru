CREATE TABLE application_review_notes (
  id           BIGSERIAL PRIMARY KEY,
  created_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  application_key VARCHAR(40) NOT NULL,
  notes TEXT NOT NULL,
  virkailija_oid VARCHAR(50) REFERENCES virkailija(oid)
);

CREATE INDEX application_review_notes_application_key ON application_review_notes (application_key);
