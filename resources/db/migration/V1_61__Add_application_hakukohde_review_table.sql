CREATE TABLE application_hakukohde_reviews (
  id              BIGSERIAL PRIMARY KEY,
  application_key VARCHAR(40) NOT NULL,
  requirement     VARCHAR(40) NOT NULL,
  state           VARCHAR(40) NOT NULL,
  hakukohde       VARCHAR(40),
  modified_time   TIMESTAMP WITH TIME ZONE DEFAULT now(),
  UNIQUE (application_key, hakukohde, requirement)
);

CREATE INDEX application_hakukohde_reviews_application_key_idx
  ON application_hakukohde_reviews (application_key);
