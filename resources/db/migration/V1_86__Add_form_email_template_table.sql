CREATE TABLE email_templates (
  id             BIGSERIAL PRIMARY KEY,
  created_time   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  form_key       VARCHAR(40)              NOT NULL,
  haku_oid       VARCHAR(40)                       DEFAULT '',
  virkailija_oid VARCHAR(40)              NOT NULL REFERENCES virkailija (oid),
  lang           VARCHAR(40)              NOT NULL,
  content        TEXT,
  UNIQUE (form_key, haku_oid, lang)
);

CREATE INDEX ON email_templates (form_key);