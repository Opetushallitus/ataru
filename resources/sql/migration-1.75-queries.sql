-- name: yesql-get-all-application-reviews
SELECT application_key, notes FROM application_reviews;

-- name: yesql-create-application-review-note!
INSERT INTO application_review_notes (application_key, notes) VALUES (:application_key, :notes);
