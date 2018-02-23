CREATE INDEX applications_lower_case_email_idx
  ON applications (lower(email));