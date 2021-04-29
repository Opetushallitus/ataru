-- name: yesql-add-application_hakukohde_reviews!
-- Add application_hakukohde_reviews
INSERT INTO application_hakukohde_reviews (application_key, requirement, state, hakukohde)
VALUES (:application, 'selection-state', 'processing', :hakukohde);

-- name: yesql-delete_application_hakukohde_reviews!
-- Delete application_hakukohde_reviews
DELETE FROM application_hakukohde_reviews ahr
WHERE ahr.application_key = :application;