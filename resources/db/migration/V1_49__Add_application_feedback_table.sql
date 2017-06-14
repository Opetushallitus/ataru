CREATE TABLE application_feedback (
  id           BIGSERIAL PRIMARY KEY,
  created_time TIMESTAMP WITH TIME ZONE DEFAULT now(),
  form_key     VARCHAR(40) NOT NULL,
  form_id      BIGINT REFERENCES forms (id),
  stars        INTEGER     NOT NULL,
  feedback     TEXT        NULL,
  user_agent   TEXT        NULL
);

