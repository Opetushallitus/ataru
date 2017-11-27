-- name: yesql-update-application-state!
UPDATE application_reviews SET state = :state WHERE application_key = :key;

-- name: yesql-get-latest-versions-of-all-applications
SELECT * from latest_applications ORDER BY id DESC;