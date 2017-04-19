ALTER TABLE applications ADD COLUMN ssn VARCHAR(11);
CREATE INDEX applications_ssn_idx ON applications (ssn);
