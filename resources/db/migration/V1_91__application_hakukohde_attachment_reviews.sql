CREATE TABLE application_hakukohde_attachment_reviews (
  id              BIGSERIAL PRIMARY KEY,
  application_key VARCHAR(40) NOT NULL,
  attachment_key  VARCHAR(40) NOT NULL,
  hakukohde       VARCHAR(40) NOT NULL,
  state           VARCHAR(40) NOT NULL,
  modified_time   TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE UNIQUE INDEX application_hakukohde_attachment_reviews_idx
  ON application_hakukohde_attachment_reviews (application_key, hakukohde, attachment_key);
