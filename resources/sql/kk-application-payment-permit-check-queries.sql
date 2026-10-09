-- name: yesql-get-kk-application-payment-permit-checks
SELECT attachment_key                                AS "attachment-key",
       found_in_migri                                AS "found-in-migri",
       to_char(permit_valid_until, 'YYYY-MM-DD')     AS "permit-valid-until",
       payment_obligation_basis                      AS "payment-obligation-basis",
       payment_obligation_other_reason               AS "payment-obligation-other-reason",
       virkailija_oid                                AS "virkailija-oid",
       organization_oid                              AS "organization-oid",
       checked_time                                  AS "checked-time",
       inherited_from_application_key                AS "inherited-from-application-key",
       inherited_time                                AS "inherited-time"
FROM kk_application_payment_permit_checks
WHERE application_key = :application_key
ORDER BY attachment_key;

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

-- name: yesql-delete-kk-application-payment-permit-checks
-- Checks saved after :checked_until are kept, as the decision to remove the checks was made without them
DELETE FROM kk_application_payment_permit_checks
WHERE application_key = :application_key
  AND checked_time <= :checked_until
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
