CREATE TABLE information_request (
  id BIGSERIAL PRIMARY KEY,
  application_key VARCHAR(40) NOT NULL,
  subject TEXT NOT NULL,
  message TEXT NOT NULL,
  state VARCHAR(40) NOT NULL
);

CREATE INDEX information_request_application_key_idx ON information_request (application_key);
