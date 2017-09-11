CREATE TABLE application_hakukohde_reviews (
  id            BIGSERIAL PRIMARY KEY,
  application   BIGINT REFERENCES applications (id) NOT NULL,
  requirement   VARCHAR(40)                         NOT NULL,
  value         VARCHAR(40)                         NOT NULL,
  modified_time TIMESTAMP WITH TIME ZONE DEFAULT now()
)



