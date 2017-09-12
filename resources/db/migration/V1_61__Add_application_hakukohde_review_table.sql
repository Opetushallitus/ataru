CREATE TABLE application_hakukohde_reviews (
  id            BIGSERIAL PRIMARY KEY,
  application   BIGINT REFERENCES applications (id) NOT NULL,
  requirement   VARCHAR(40)                         NOT NULL,
  value         VARCHAR(40)                         NOT NULL,
  hakukohde     VARCHAR(40),
  created_time  TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX application_hakukohde_reviews_created_time_idx
  ON application_hakukohde_reviews (created_time);

