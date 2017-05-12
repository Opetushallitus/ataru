ALTER TABLE applications ADD COLUMN dob DATE;
CREATE INDEX applications_dob_idx ON applications (dob);
