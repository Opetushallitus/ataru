-- name: yesql-get-kk-application-payment-permit-checks
SELECT c.attachment_key                                AS "attachment-key",
       c.found_in_migri                                AS "found-in-migri",
       to_char(c.permit_valid_until, 'YYYY-MM-DD')     AS "permit-valid-until",
       c.payment_obligation_basis                      AS "payment-obligation-basis",
       c.payment_obligation_other_reason               AS "payment-obligation-other-reason",
       c.virkailija_oid                                AS "virkailija-oid",
       v.first_name                                    AS "virkailija-first-name",
       v.last_name                                     AS "virkailija-last-name",
       c.organization_oid                              AS "organization-oid",
       c.checked_time                                  AS "checked-time",
       c.inherited_from_application_key                AS "inherited-from-application-key",
       c.inherited_time                                AS "inherited-time"
FROM kk_application_payment_permit_checks c
JOIN virkailija v ON v.oid = c.virkailija_oid
WHERE c.application_key = :application_key
ORDER BY c.attachment_key;

-- name: yesql-upsert-kk-application-payment-permit-check
INSERT INTO kk_application_payment_permit_checks (application_key,
                                                  attachment_key,
                                                  found_in_migri,
                                                  permit_valid_until,
                                                  payment_obligation_basis,
                                                  payment_obligation_other_reason,
                                                  virkailija_oid,
                                                  organization_oid,
                                                  checked_time)
VALUES (:application_key,
        :attachment_key,
        :found_in_migri,
        :permit_valid_until::date,
        :payment_obligation_basis,
        :payment_obligation_other_reason,
        :virkailija_oid,
        :organization_oid,
        now())
ON CONFLICT (application_key, attachment_key) DO UPDATE
    SET found_in_migri                  = EXCLUDED.found_in_migri,
        permit_valid_until              = EXCLUDED.permit_valid_until,
        payment_obligation_basis        = EXCLUDED.payment_obligation_basis,
        payment_obligation_other_reason = EXCLUDED.payment_obligation_other_reason,
        virkailija_oid                  = EXCLUDED.virkailija_oid,
        organization_oid                = EXCLUDED.organization_oid,
        checked_time                    = EXCLUDED.checked_time,
        inherited_from_application_key  = NULL,
        inherited_time                  = NULL,
        modified_time                   = now()
RETURNING attachment_key                                AS "attachment-key",
          found_in_migri                                AS "found-in-migri",
          to_char(permit_valid_until, 'YYYY-MM-DD')     AS "permit-valid-until",
          payment_obligation_basis                      AS "payment-obligation-basis",
          payment_obligation_other_reason               AS "payment-obligation-other-reason",
          virkailija_oid                                AS "virkailija-oid",
          organization_oid                              AS "organization-oid",
          checked_time                                  AS "checked-time",
          inherited_from_application_key                AS "inherited-from-application-key",
          inherited_time                                AS "inherited-time";

-- name: yesql-insert-inherited-kk-application-payment-permit-check
-- Kopio hakijan aiemmasta tarkistuksesta. Hakemuksen oma tarkistus säilyy ennallaan, jos sellainen on jo
INSERT INTO kk_application_payment_permit_checks (application_key,
                                                  attachment_key,
                                                  found_in_migri,
                                                  permit_valid_until,
                                                  payment_obligation_basis,
                                                  payment_obligation_other_reason,
                                                  virkailija_oid,
                                                  organization_oid,
                                                  checked_time,
                                                  inherited_from_application_key,
                                                  inherited_time)
VALUES (:application_key,
        :attachment_key,
        :found_in_migri,
        :permit_valid_until::date,
        :payment_obligation_basis,
        :payment_obligation_other_reason,
        :virkailija_oid,
        :organization_oid,
        :checked_time,
        :inherited_from_application_key,
        now())
ON CONFLICT (application_key, attachment_key) DO NOTHING
RETURNING attachment_key                                AS "attachment-key",
          found_in_migri                                AS "found-in-migri",
          to_char(permit_valid_until, 'YYYY-MM-DD')     AS "permit-valid-until",
          payment_obligation_basis                      AS "payment-obligation-basis",
          payment_obligation_other_reason               AS "payment-obligation-other-reason",
          virkailija_oid                                AS "virkailija-oid",
          organization_oid                              AS "organization-oid",
          checked_time                                  AS "checked-time",
          inherited_from_application_key                AS "inherited-from-application-key",
          inherited_time                                AS "inherited-time";

-- name: yesql-delete-kk-application-payment-permit-checks
-- Hetken :modified_until jälkeen tallennetut tarkistukset säilytetään, koska niissä on voitu jo huomioida poiston syy
DELETE FROM kk_application_payment_permit_checks
WHERE application_key = :application_key
  AND modified_time <= :modified_until
RETURNING attachment_key                                AS "attachment-key",
          found_in_migri                                AS "found-in-migri",
          to_char(permit_valid_until, 'YYYY-MM-DD')     AS "permit-valid-until",
          payment_obligation_basis                      AS "payment-obligation-basis",
          payment_obligation_other_reason               AS "payment-obligation-other-reason",
          virkailija_oid                                AS "virkailija-oid",
          organization_oid                              AS "organization-oid",
          checked_time                                  AS "checked-time",
          inherited_from_application_key                AS "inherited-from-application-key",
          inherited_time                                AS "inherited-time";
