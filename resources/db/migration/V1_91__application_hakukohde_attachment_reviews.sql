CREATE TABLE application_hakukohde_attachment_reviews (
  id              BIGSERIAL PRIMARY KEY,
  application_key text NOT NULL,
  attachment_key  text NOT NULL,
  hakukohde       text NOT NULL,
  state           text NOT NULL,
  modified_time   TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE UNIQUE INDEX application_hakukohde_attachment_reviews_idx
  ON application_hakukohde_attachment_reviews (application_key, hakukohde, attachment_key);
