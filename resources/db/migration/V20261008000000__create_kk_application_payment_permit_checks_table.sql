-- Virkailija's notes on a kk application payment residence permit attachment: whether the permit was found
-- in Migri's permit information service, permit card validity and the payment obligation basis.
CREATE TABLE IF NOT EXISTS kk_application_payment_permit_checks (
    id                              BIGSERIAL PRIMARY KEY,
    application_key                 TEXT NOT NULL REFERENCES application_reviews (application_key),
    attachment_key                  TEXT NOT NULL,
    found_in_migri                  BOOLEAN,
    permit_valid_until              DATE,
    payment_obligation_basis        TEXT
        CHECK (payment_obligation_basis IN ('not-obligated',
                                            'obligated-own-studies',
                                            'obligated-family-member-studies',
                                            'obligated-other')),
    payment_obligation_other_reason TEXT,
    virkailija_oid                  TEXT NOT NULL REFERENCES virkailija (oid),
    -- Tarjoaja organization of an applied hakukohde that the virkailija has edit rights to,
    -- resolved when the check is saved since virkailija's organizations may change afterwards.
    organization_oid                TEXT,
    -- Time of the actual check. Kept separate from modified_time so that an inherited check can carry the
    -- time of the original check.
    checked_time                    TIMESTAMP WITH TIME ZONE NOT NULL,
    -- TODO OPHHAMA-17: filled when the check is inherited from an earlier application of the same applicant
    inherited_from_application_key  TEXT,
    inherited_time                  TIMESTAMP WITH TIME ZONE,
    modified_time                   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (application_key, attachment_key)
);
