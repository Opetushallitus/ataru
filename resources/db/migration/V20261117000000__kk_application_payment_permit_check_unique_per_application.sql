-- Hakemuksella on enintään yksi oleskelulupatarkistus: hakijan valitseman dokumenttivaihtoehdon oleskelulupaliitteen
-- tarkistus.
--
-- Taulu lukitaan kirjoituksilta ennen siivousta, jotta tarkistuksia ei voi tallentaa siivouksen ja yksilöivyysehdon
-- lisäämisen välissä.
LOCK TABLE kk_application_payment_permit_checks IN SHARE ROW EXCLUSIVE MODE;

-- Poistetaan tarkistukset, jotka eivät koske hakemuksen nykyisen dokumenttivaihtoehdon oleskelulupaliitettä. Niitä on
-- jäänyt hakemuksille, joilla dokumenttivaihtoehto vaihdettiin tarkistuksen jälkeen ennen kuin vaihto alkoi poistaa
-- edellisen vaihtoehdon tarkistuksen. Maksuvelvollisuus päätellään vain nykyisen vaihtoehdon liitteen tarkistuksesta,
-- joten poisto ei muuta maksuvelvollisuutta eikä käsittelymerkintöjä tarvitse palauttaa. Poisto kirjataan hakemuksen
-- historiaan samana tapahtumana kuin vaihtoehdon vaihtuessa.
--
-- Vaihtoehdon arvo -> oleskelulupaliite, kuten kk-application-payment-residence-permit-attachment-key-by-option.
-- Passilla (0) ja ilman dokumenttia (8) ei ole oleskelulupaliitettä, joten niiden kaikki tarkistukset poistetaan.
WITH permit_attachment_by_option (option_value, attachment_key) AS (
    VALUES ('1', 'eu-blue-card-attachment'),
           ('2', 'eu-family-member-permit'),
           ('3', 'continuous-residence-permit-front'),
           ('4', 'longterm-permit-attachment'),
           ('5', 'brexit-permit-attachment'),
           ('6', 'permanent-residence-permit'),
           ('7', 'temporary-protection-permit')
),
removed AS (
    DELETE FROM kk_application_payment_permit_checks c
    WHERE c.attachment_key IS DISTINCT FROM (
        SELECT p.attachment_key
        FROM latest_applications a
        JOIN answers ans ON ans.application_id = a.id AND ans.key = 'kk-application-payment-option'
        JOIN permit_attachment_by_option p ON p.option_value = ans.value
        WHERE a.key = c.application_key
    )
    RETURNING c.application_key, c.attachment_key
)
INSERT INTO application_events (application_key, event_type, review_key)
SELECT application_key, 'kk-payment-permit-check-removed', attachment_key
FROM removed;

-- Aiempi yksilöivyysehto (application_key, attachment_key) säilytetään, jotta asennuksen aikana vielä käynnissä olevan
-- edellisen version tallennukset (ON CONFLICT (application_key, attachment_key)) toimivat. Sen voi poistaa
-- myöhemmin.
ALTER TABLE kk_application_payment_permit_checks
    ADD CONSTRAINT kk_application_payment_permit_checks_application_key_key UNIQUE (application_key);
