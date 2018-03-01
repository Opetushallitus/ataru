CREATE TABLE email_templates (
  id             BIGSERIAL PRIMARY KEY,
  created_time   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  form_key       VARCHAR(40)              NOT NULL,
  haku_oid       VARCHAR(40),
  virkailija_oid VARCHAR(40)              NOT NULL REFERENCES virkailija (oid),
  lang           VARCHAR(40)              NOT NULL,
  template       TEXT,
  UNIQUE (form_key, haku_oid, lang)
);

