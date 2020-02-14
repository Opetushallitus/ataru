COMMENT ON TABLE application_events IS 'Logi hakemuksiin vaikuttavista tapahtumista';
COMMENT ON COLUMN application_events.new_review_state IS 'Muuttuneen tiedon uusi arvo. Tyypillisesti käsittelymerkkinnän uusi tila.';
COMMENT ON COLUMN application_events.time IS 'Tapahtumahetki.';
COMMENT ON COLUMN application_events.event_type IS 'Tapahtuman tyyppi.';
COMMENT ON COLUMN application_events.application_key IS 'Hakemus johon tapahtuma liittyy.';
COMMENT ON COLUMN application_events.virkailija_oid IS 'Muutoksen tehneen virkailijan OID jos olemassa.';
COMMENT ON COLUMN application_events.hakukohde IS 'Hakemuksen hakutoive johon tapahtuma liittyy jos olemassa.';
COMMENT ON COLUMN application_events.review_key IS 'Muuttuneen tiedon tunniste. Tyypillisesti käsittelymerkinnän tunniste.';
COMMENT ON COLUMN application_events.virkailija_organizations IS 'Muutoksen tehneen virkailijan organisaatiot muutoshetkellä jos olemassa.';

COMMENT ON TABLE application_feedback IS 'Käyttäjäpalaute';
COMMENT ON COLUMN application_feedback.created_time IS 'Palautteen jättöhetki.';
COMMENT ON COLUMN application_feedback.form_key IS 'Lomakkeen avain jonka täytön jälkeen palaute on annettu.';
COMMENT ON COLUMN application_feedback.form_id IS 'Lomakkeen id jonka täytön jälkeen palaute on annettu.';
COMMENT ON COLUMN application_feedback.form_name IS 'Lomakkeen nimi jonka täytön jälkeen palaute on annettu.';
COMMENT ON COLUMN application_feedback.stars IS 'Numeerinen arvio 1-5.';
COMMENT ON COLUMN application_feedback.feedback IS 'Kirjallinen arvio.';
COMMENT ON COLUMN application_feedback.user_agent IS 'Käyttäjän päätesovelluksen user agent.';
COMMENT ON COLUMN application_feedback.extra_data IS 'Ei käytössä.';

COMMENT ON TABLE application_hakukohde_attachment_reviews IS 'Hakemus- ja hakutoivekohtaiset liitepyyntöjen käsittelymerkinnät';
COMMENT ON COLUMN application_hakukohde_attachment_reviews.application_key IS 'Hakemus johon käsittelymerkintä liittyy.';
COMMENT ON COLUMN application_hakukohde_attachment_reviews.attachment_key IS 'Liitepyyntökysymyksen tunniste.';
COMMENT ON COLUMN application_hakukohde_attachment_reviews.hakukohde IS 'Hakutoive johon käsittelymerkintä liittyy.';
COMMENT ON COLUMN application_hakukohde_attachment_reviews.state IS 'Käsittelymerkinnän tila.';
COMMENT ON COLUMN application_hakukohde_attachment_reviews.modified_time IS 'Käsittelymerkinnän muutoksen ajankohta.';

COMMENT ON TABLE application_hakukohde_reviews IS 'Hakemus- ja hakutoivekohtaiset hakemuksen käsittelymerkinnät';
COMMENT ON COLUMN application_hakukohde_reviews.application_key IS 'Hakemus johon käsittelymerkintä liittyy.';
COMMENT ON COLUMN application_hakukohde_reviews.requirement IS 'Käsittelymerkinnän tunniste.';
COMMENT ON COLUMN application_hakukohde_reviews.state IS 'Käsittelymerkinnän tila.';
COMMENT ON COLUMN application_hakukohde_reviews.hakukohde IS 'Hakutoive johon käsittelymerkintä liittyy.';
COMMENT ON COLUMN application_hakukohde_reviews.modified_time IS 'Käsittelymerkinnän muutoksen ajankohta.';

COMMENT ON TABLE application_review_notes IS 'Hakemus-, hakutoive- ja mahdollisesti käsittelymerkintäkohtaiset muistiinpanot';
COMMENT ON COLUMN application_review_notes.created_time IS 'Muistiinpanon jätön ajankohta.';
COMMENT ON COLUMN application_review_notes.application_key IS 'Hakemus johon muistiinpano liittyy.';
COMMENT ON COLUMN application_review_notes.notes IS 'Muistiinpanon sisältö.';
COMMENT ON COLUMN application_review_notes.virkailija_oid IS 'Muistiinpanon jättäneen virkailijan OID.';
COMMENT ON COLUMN application_review_notes.removed IS 'Muistiinpanon poiston ajankohta jos poistettu.';
COMMENT ON COLUMN application_review_notes.hakukohde IS 'Hakutoive johon muistiinpanon liittyy.';
COMMENT ON COLUMN application_review_notes.state_name IS 'Käsittelymerkinnän tunniste jos muistiinpano koskee käsittelymerkintää.';
COMMENT ON COLUMN application_review_notes.virkailija_organizations IS 'Muistiinpanon jättäneen virkailijan organisaatiot jättöhetkellä.';

COMMENT ON TABLE application_reviews IS 'Hakemuksen tila ja pisteet';
COMMENT ON COLUMN application_reviews.modified_time IS 'Tilan muutoksen ajankohta.';
COMMENT ON COLUMN application_reviews.state IS 'Onko hakemus aktiivinen vai passiivinen. Muut tilat eivät käytössä.';
COMMENT ON COLUMN application_reviews.application_key IS 'Hakemuksen tunniste.';
COMMENT ON COLUMN application_reviews.score IS 'Hakemuksen pisteet.';

COMMENT ON TABLE application_secrets IS 'Hakijan hakemuksen muokkaussalaisuudet';
COMMENT ON COLUMN application_secrets.created_time IS 'Salaisuuden luontihetki.';
COMMENT ON COLUMN application_secrets.application_key IS 'Hakemus jota salaisuudella voi muokata.';
COMMENT ON COLUMN application_secrets.secret IS 'Salainen arvo.';

COMMENT ON TABLE applications IS 'Hakemuksien versiot';
COMMENT ON COLUMN applications.id IS 'Hakemuksen version tunniste. Suurempi arvo tarkoittaa uudempaa versiota.';
COMMENT ON COLUMN applications.key IS 'Hakemuksen versiosta riippumaton tunniste.';
COMMENT ON COLUMN applications.lang IS 'Hakemuksen jättökieli.';
COMMENT ON COLUMN applications.form_id IS 'Lomakkeen id jota vasten hakemus on jätetty.';
COMMENT ON COLUMN applications.content IS 'Hakemuksen vastaukset.';
COMMENT ON COLUMN applications.preferred_name IS 'Hakijan syöttämä tai ONR:stä saatu kutsumanimi. Hakuindeksiä varten.';
COMMENT ON COLUMN applications.last_name IS 'Hakijan syöttämä tai ONR:stä saatu sukunimi. Hakuindeksiä varten.';
COMMENT ON COLUMN applications.person_oid IS 'Hakijan henkilö OID jos olemassa.';
COMMENT ON COLUMN applications.hakukohde IS 'Hakutoiveiden OID:t järjestyksessä jos olemassa.';
COMMENT ON COLUMN applications.created_time IS 'Hakemuksen tämän version jättöajankohta.';
COMMENT ON COLUMN applications.haku IS 'Haun OID johon hakemus on jätetty jos olemassa.';
COMMENT ON COLUMN applications.ssn IS 'Hakijan syöttämä tai ONR:stä saatu henkilötunnus jos olemassa. Hakuindeksiä varten.';
COMMENT ON COLUMN applications.dob IS 'Hakijan syöttämä tai ONR:stä saatu syntymäaika. Hakuindeksiä varten.';
COMMENT ON COLUMN applications.email IS 'Hakijan syöttämä sähköpostiosoite.';
COMMENT ON COLUMN applications.submitted IS 'Hakemuksen alkuperäinen jättöajankohta.';

COMMENT ON TABLE cas_ticketstore IS 'Kirjautumisessa käytetyt CAS service ticketit';
COMMENT ON COLUMN cas_ticketstore.ticket IS 'CAS service ticket.';
COMMENT ON COLUMN cas_ticketstore.login_time IS 'Kirjautumisen ajankohta.';

COMMENT ON TABLE email_templates IS 'Hakukohtaiset hakemus jätetty -sähköpostien pohjat';
COMMENT ON COLUMN email_templates.created_time IS 'Pohjan muokkauksen ajankohta.';
COMMENT ON COLUMN email_templates.form_key IS 'Lomakkeen tunniste jonka yhteydessä pohjaa käytetään.';
COMMENT ON COLUMN email_templates.haku_oid IS 'Haun OID jonka yhteydessä pohjaa käytetään jos olemassa.';
COMMENT ON COLUMN email_templates.virkailija_oid IS 'Pohjaa muokanneen virkailijan OID.';
COMMENT ON COLUMN email_templates.lang IS 'Sähköpostin kieli.';
COMMENT ON COLUMN email_templates.content IS 'Sähköpostin pääsisältö.';
COMMENT ON COLUMN email_templates.subject IS 'Sähköpostin aihe.';
COMMENT ON COLUMN email_templates.content_ending IS 'Sähköpostin loppusisältö.';

COMMENT ON TABLE field_deadlines IS 'Hakemus ja kysymyskohtaiset muokkauksen takarajat';
COMMENT ON COLUMN field_deadlines.application_key IS 'Hakemus jota takaraja koskee.';
COMMENT ON COLUMN field_deadlines.field_id IS 'Kysymys jota takaraja koskee.';
COMMENT ON COLUMN field_deadlines.deadline IS 'Takaraja.';
COMMENT ON COLUMN field_deadlines.modified IS 'Takarajan muokkauksen ajankohta.';

COMMENT ON TABLE forms IS 'Lomakkeiden versiot';
COMMENT ON COLUMN forms.id IS 'Lomakkeen version tunniste. Suurempi arvo tarkoittaa uudempaa versiota.';
COMMENT ON COLUMN forms.content IS 'Lomakkeen sisältö.';
COMMENT ON COLUMN forms.key IS 'Lomakkeen versiosta riippumaton tunniste.';
COMMENT ON COLUMN forms.created_time IS 'Lomakkeen tämän version luontiajankohta.';
COMMENT ON COLUMN forms.created_by IS 'Lomakkeen luoneen virkailijan OID.';
COMMENT ON COLUMN forms.languages IS 'Lomakkeen tuetut kieliversiot.';
COMMENT ON COLUMN forms.organization_oid IS 'Lomakkeen omistavan organisaation OID.';
COMMENT ON COLUMN forms.deleted IS 'Onko lomake poistettu.';
COMMENT ON COLUMN forms.name IS 'Lomakkeen nimi tuetuilla kielillä.';
COMMENT ON COLUMN forms.locked IS 'Ajanhetki jolloin lomake on lukittu jos lomake on lukittu.';
COMMENT ON COLUMN forms.locked_by IS 'Lomakkeen lukinneen virkailijan OID.';

COMMENT ON TABLE information_requests IS 'Hakemusten täydennyspyynnöt';
COMMENT ON COLUMN information_requests.application_key IS 'Hakemus johon täydennyspyyntö liittyy.';
COMMENT ON COLUMN information_requests.subject IS 'Täydennyspyyntösähköpostin aihe.';
COMMENT ON COLUMN information_requests.message IS 'Täydennyspyyntösähköpostin sisältö.';
COMMENT ON COLUMN information_requests.created_time IS 'Täydennyspyynnön luontiajankohta.';
COMMENT ON COLUMN information_requests.virkailija_oid IS 'Täydennyspyynnön luoneen virkailijan OID.';
COMMENT ON COLUMN information_requests.message_type IS 'Onko kyseessä yhteen vai useaan hakemukseen liittyvä täydennyspyyntö.';

COMMENT ON TABLE initial_selections IS 'Alustavat vastausvaihtoehtovaraukset';
COMMENT ON COLUMN initial_selections.selection_id IS 'Varauksen tunniste.';
COMMENT ON COLUMN initial_selections.question_id IS 'Kysymyksen tunniste.';
COMMENT ON COLUMN initial_selections.answer_id IS 'Vastausvaihtoehdon tunniste.';
COMMENT ON COLUMN initial_selections.selection_group_id IS 'Saman varauskäsittelyn piirissä olevien varausten tunniste.';
COMMENT ON COLUMN initial_selections.valid IS 'Varauksen voimassaoloaika.';
COMMENT ON COLUMN initial_selections.transaction_id IS 'Rivin kirjoittaneen transaktion id. Käytetään versioinnissa.';
COMMENT ON COLUMN initial_selections.system_time IS 'Rivin voimassaoloaika. Käytetään versioinnissa.';

COMMENT ON TABLE initial_selections_history IS 'Alustavat vastausvaihtoehtovaraukset -taulun historia';

COMMENT ON TABLE job_iterations IS 'Tausta-ajojärjestelmän ajovaiheet';
COMMENT ON COLUMN job_iterations.job_id IS 'Työn tunniste johon ajovaihe liittyy.';
COMMENT ON COLUMN job_iterations.step IS 'Ajovaiheen tunniste.';
COMMENT ON COLUMN job_iterations.transition IS 'Ajovaiheen seuraavan tapahtuman tunniste.';
COMMENT ON COLUMN job_iterations.state IS 'Työn tila.';
COMMENT ON COLUMN job_iterations.next_activation IS 'Työn seuraavan ajovaiheen tai tapahtuman tuleva suoritusajankohta.';
COMMENT ON COLUMN job_iterations.retry_count IS 'Ajovaiheen tehtyjen uudelleenyritysten määrä.';
COMMENT ON COLUMN job_iterations.executed IS 'Onko ajovaihe suoritettu.';
COMMENT ON COLUMN job_iterations.execution_time IS 'Ajovaiheen suorituksen ajankohta.';
COMMENT ON COLUMN job_iterations.final IS 'Oliko tämä työn viimeinen ajovaihe.';
COMMENT ON COLUMN job_iterations.caused_by_error IS 'Ajovaiheen suorituksen keskeyttänyt virhe.';

COMMENT ON TABLE jobs IS 'Tausta-ajojärjestelmän työt';
COMMENT ON COLUMN jobs.job_type IS 'Työn tyyppi.';
COMMENT ON COLUMN jobs.stop IS 'Tuleeko työn suoritus keskeyttää ennen seuraavan ajovaiheen suoritusta.';

COMMENT ON TABLE priorisoivat_hakukohderyhmat IS 'Priorisoivat hakukohderyhmat';
COMMENT ON COLUMN priorisoivat_hakukohderyhmat.haku_oid IS 'Haun OID jossa prioriteetit ovat käytössä.';
COMMENT ON COLUMN priorisoivat_hakukohderyhmat.hakukohderyhma_oid IS 'Hakukohderyhmän OID.';
COMMENT ON COLUMN priorisoivat_hakukohderyhmat.prioriteetit IS 'Hakukohteet ryhmiteltynä, siinä järjestyksessä kun ne tulee asettaa hakutoiveiksi.';
COMMENT ON COLUMN priorisoivat_hakukohderyhmat.created_time IS 'Muokkauksen ajankohta.';

COMMENT ON TABLE rajaavat_hakukohderyhmat IS 'Rajaavata hakukohderyhmät';
COMMENT ON COLUMN rajaavat_hakukohderyhmat.haku_oid  IS 'Haun OID jossa prioriteetit ovat käytössä.';
COMMENT ON COLUMN rajaavat_hakukohderyhmat.hakukohderyhma_oid  IS 'Hakukohderyhmän OID.';
COMMENT ON COLUMN rajaavat_hakukohderyhmat.raja IS 'Hakutoiveiden enimmäismäärä tästä ryhmästä.';
COMMENT ON COLUMN rajaavat_hakukohderyhmat.created_time IS 'Muokkauksen ajankohta.';

COMMENT ON TABLE selections IS 'Vastausvaihtoehtovaraukset';
COMMENT ON COLUMN selections.application_key IS 'Hakemus jolla varaus on tehty.';
COMMENT ON COLUMN selections.question_id IS 'Kysymyksen tunniste.';
COMMENT ON COLUMN selections.answer_id IS 'Vastausvaihtoehdon tunniste.';
COMMENT ON COLUMN selections.selection_group_id IS 'Saman varauskäsittelyn piirissä olevien varausten tunniste.';
COMMENT ON COLUMN selections.transaction_id IS 'Rivin kirjoittaneen transaktion id. Käytetään versioinnissa.';
COMMENT ON COLUMN selections.system_time IS 'Rivin voimassaoloaika. Käytetään versioinnissa.';

COMMENT ON TABLE selections_history IS 'Vastausvaihtoehtovaraukset-taulun historia';

COMMENT ON TABLE sessions IS 'Sovelluksen sessiot';
COMMENT ON COLUMN sessions.key IS 'Session tunniste.';
COMMENT ON COLUMN sessions.data IS 'Sessioon liitetyt tiedot.';

COMMENT ON TABLE virkailija IS 'Virkailijoiden tiedot ja asetukset';
COMMENT ON COLUMN virkailija.oid IS 'Virkailijan OID.';
COMMENT ON COLUMN virkailija.first_name IS 'Virkailijan etunimi.';
COMMENT ON COLUMN virkailija.last_name IS 'Virkailijan sukunimi.';
COMMENT ON COLUMN virkailija.settings IS 'Virkailijan asetukset.';

COMMENT ON TABLE virkailija_create_secrets IS 'Virkailijan tekemään hakemuksen jättöön käytettävät salaisuudet';
COMMENT ON COLUMN virkailija_create_secrets.virkailija_oid IS 'Salaisuuden luoneen virkailijan OID.';
COMMENT ON COLUMN virkailija_create_secrets.secret IS 'Salainen arvo.';
COMMENT ON COLUMN virkailija_create_secrets.valid IS 'Salaisuuden voimassaoloaika.';

COMMENT ON TABLE virkailija_credentials IS 'Ei käytössä.';

COMMENT ON TABLE virkailija_rewrite_secrets IS 'Virkailijan tekemään hakemuksen muokkaukseen jolla muokataan henkilötietoja käytettävät salaisuudet';
COMMENT ON COLUMN virkailija_rewrite_secrets.virkailija_oid IS 'Salaisuuden luoneen virkailijan OID.';
COMMENT ON COLUMN virkailija_rewrite_secrets.application_key IS 'Sen hakemuksen tunniste jota salaisuudella voi muokata.';
COMMENT ON COLUMN virkailija_rewrite_secrets.secret IS 'Salainen arvo.';
COMMENT ON COLUMN virkailija_rewrite_secrets.valid IS 'Salaisuuden voimassaoloaika.';

COMMENT ON TABLE virkailija_update_secrets IS 'Virkailijan tekemään hakemuksen muokkaukseen käytettävät salaisuudet';
COMMENT ON COLUMN virkailija_update_secrets.virkailija_oid IS 'Salaisuuden luoneen virkailijan OID.';
COMMENT ON COLUMN virkailija_update_secrets.application_key IS 'Sen hakemuksen tunniste jota salaisuudella voi muokata.';
COMMENT ON COLUMN virkailija_update_secrets.secret IS 'Salainen arvo.';
COMMENT ON COLUMN virkailija_update_secrets.valid IS 'Salaisuuden voimassaoloaika.';
