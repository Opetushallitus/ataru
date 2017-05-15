ALTER TABLE applications ADD COLUMN email VARCHAR(255);
CREATE INDEX applications_email_idx ON applications (email);
