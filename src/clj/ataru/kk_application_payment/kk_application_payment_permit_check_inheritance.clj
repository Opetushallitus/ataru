(ns ataru.kk-application-payment.kk-application-payment-permit-check-inheritance
  "Kopioi virkailijan oleskelulupatarkistuksen hakijan myöhemmille saman alkamiskauden hakemuksille.

   Myöhempi hakemus saa tarkistuksen, liitemerkinnät ja tarkastetun maksuvelvollisuuden hakemukselta, jonka
   maksuvelvollisuus on tarkastettu. Pääehdot (kaikki ks. inherit-permit-checks! ja inheritance-for) ovat, että
   myöhemmällä hakemuksella
   - on sama dokumenttivaihtoehto,
   - lupakortti on voimassa hakemuksen jättöpäivänä ja
   - on oleskelulupaliite. Liitetiedostoja ei verrata, koska hakija voi ottaa lupakortista uudet kuvat jokaiseen
     hakemukseen.

   Jos tarkistuksessa hakija on todettu ei-maksuvelvolliseksi eikä sen hakemuksen maksua, jolta tarkistus kopioidaan,
   vaadita, tarkistus kopioidaan myös silloin, kun liite puuttuu (ks. missing-attachment-source-candidates).
   Puuttuva liite ei silloin tee hakijasta maksuvelvollista niin kauan kuin peritty tarkistus on ennallaan ja
   maksuvelvollisuus on tarkastettu (ks. kk-application-payment/attachments-invalid-and-deadline-passed?). Jos lupa on
   löytynyt Migrin palvelusta ja peruste on merkitty, tarkistus ratkaisee tarkastetun maksuvelvollisuuden liitteistä
   riippumatta (ks. kk-application-payment/permit-check-payment-obligation).

   Tarkistus kopioidaan vain kerran, eivätkä alkuperäisen hakemuksen myöhemmät muutokset muuta kopiota tai
   sen vaikutusta maksuun."
  (:require [ataru.applications.application-store :as application-store]
            [ataru.component-data.kk-application-payment-module :as payment-module]
            [ataru.db.db :as db]
            [ataru.kk-application-payment.kk-application-payment-permit-check-store :as store]
            [ataru.log.audit-log :as audit-log]
            [ataru.time :as time]
            [ataru.util :as util]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [taoensso.timbre :as log]))

(def inherited-event-type "kk-payment-permit-check-inherited")

(def ^:private audit-log-session {:cronjob "inherit-kk-application-payment-permit-checks"})

(defn- answer-value
  [answers attachment-key]
  (:value (get answers (keyword attachment-key))))

(defn delivered?
  "Onko hakija toimittanut liitteen."
  [answers attachment-key]
  (boolean (some #(and (string? %) (not (string/blank? %)))
                 (flatten [(answer-value answers attachment-key)]))))

(defn- obligation-reviews
  [application-key]
  (filter #(= "kk-application-payment-obligation" (:requirement %))
          (application-store/get-application-hakukohde-reviews application-key)))

(defn- reviewed-reviews
  [reviews]
  (filter #(= "reviewed" (:state %)) reviews))

(defn- submitted-date
  [application]
  (.toLocalDate (time/to-time-zone (:submitted application) (time/time-zone-for-id "Europe/Helsinki"))))

(defn not-obligated?
  "Tarkistus, jossa hakijaa ei ole todettu maksuvelvolliseksi."
  [check]
  (contains? #{nil "not-obligated"} (:payment-obligation-basis check)))

(defn- valid-at-submission?
  [check application]
  (when-let [valid-until (some-> (:permit-valid-until check) java.time.LocalDate/parse)]
    (not (.isBefore valid-until (submitted-date application)))))

(defn- source-candidates
  "Liitteen tarkistukset ennen kohdehakemusta jätetyillä hakemuksilla, joiden maksuvelvollisuus on tarkastettu."
  [applications target attachment-key]
  (for [source applications
        :when (time/before? (:submitted source) (:submitted target))
        ; Luetaan kannasta, koska lähde on voinut periä tarkistuksen aiemmin samassa ajossa
        :when (seq (reviewed-reviews (obligation-reviews (:key source))))
        :let [check (store/get-permit-check (:key source) attachment-key)]
        :when (and (some? check) (valid-at-submission? check target))]
    {:application source
     :check       check}))

(defn- invalid-attachments?
  [application-key]
  (some #(and (contains? payment-module/kk-application-payment-exempt-attachment-keys (:attachment-key %))
              (contains? payment-module/kk-application-payment-invalid-attachment-states (:state %)))
        (application-store/get-application-attachment-reviews application-key false)))

(defn- missing-attachment-source-candidates
  "Lähteet hakemukselle, jolta oleskelulupaliite puuttuu. Lähteen tarkistuksessa hakija on todettu
   ei-maksuvelvolliseksi, eikä lähteen maksua vaadita ajon alussa. Lähteellä ei saa olla puuttuviksi tai
   virheellisiksi merkittyjä liitteitä, koska ne voivat tehdä siitä maksuvelvollisen vielä samassa ajossa.
   Liitteillä ei ole merkitystä, jos lupa on löytynyt Migrin palvelusta ja peruste on merkitty (ks.
   kk-application-payment/permit-check-payment-obligation)."
  [applications not-required-application-keys target attachment-key]
  (->> (source-candidates (filter (comp not-required-application-keys :key) applications) target attachment-key)
       (filter (comp not-obligated? :check))
       (remove (fn [{:keys [application check]}]
                 (and (not (and (true? (:found-in-migri check))
                                (= "not-obligated" (:payment-obligation-basis check))))
                      (invalid-attachments? (:key application)))))))

(defn- latest-source
  [candidates]
  (last (sort-by (comp time/to-instant :checked-time :check) candidates)))

; Tarkistus periytyy vain kerran. Se ei periydy lainkaan, jos virkailija on jo tarkistanut liitteen hakemuksella tai
; tarkistus on nollattu uuden liitteen vuoksi, jotta uusi liite tarkastetaan
(def ^:private handled-event-types
  #{inherited-event-type "kk-payment-permit-check-updated" "kk-payment-permit-check-reset"})

(defn- handled-before?
  [application-key attachment-key]
  (some #(and (contains? handled-event-types (:event-type %))
              (= attachment-key (:review-key %)))
        (application-store/get-application-events application-key)))

(defn- decided-after-check?
  "Periytyminen ei ylikirjoita kohdehakemuksen maksuvelvollisuutta, josta virkailija on päättänyt (esim. merkinnyt
   tarkastetuksi tai lähettänyt Migrin tarkastettavaksi) alkuperäisen tarkistuksen jälkeen."
  [target-obligation-reviews check]
  (some #(and (not= "unreviewed" (:state %))
              (time/after? (:modified-time %) (:checked-time check)))
        target-obligation-reviews))

(defn- latest-attachment-reviews
  "Lähdehakemuksen dokumenttivaihtoehdon kunkin liitteen viimeisin merkintä, kun liite on tarkastettu."
  [source-key attachment-keys]
  (->> (application-store/get-application-attachment-reviews source-key false)
       (filter #(contains? attachment-keys (:attachment-key %)))
       (group-by :attachment-key)
       (map (fn [[attachment-key reviews]]
              [attachment-key (:state (last (sort-by (comp time/to-instant :modified-time) reviews)))]))
       (remove (comp #{"not-checked"} second))
       (into {})))

(defn- reviewed-after-check?
  "Liite, jonka virkailija on tarkastanut kohdehakemuksella alkuperäisen tarkistuksen jälkeen, säilyttää
   merkintänsä."
  [attachment-review check]
  (and (not= "not-checked" (:state attachment-review))
       (time/after? (:modified-time attachment-review) (:checked-time check))))

(defn- inheritance-for
  "Kohdehakemukselle tehtävä periytyminen tai nil."
  [applications not-required-application-keys target]
  (let [answers        (util/application-answers-by-key target)
        option         (:value (get answers (keyword payment-module/kk-application-payment-choice-key)))
        attachment-key (get payment-module/kk-application-payment-residence-permit-attachment-key-by-option option)]
    (when (and (some? attachment-key)
               (nil? (store/get-permit-check (:key target) attachment-key))
               (not (handled-before? (:key target) attachment-key)))
      (when-let [{:keys [application check]} (latest-source
                                               (if (delivered? answers attachment-key)
                                                 (source-candidates applications target attachment-key)
                                                 (missing-attachment-source-candidates
                                                   applications not-required-application-keys target
                                                   attachment-key)))]
        (let [target-obligation-reviews (obligation-reviews (:key target))]
          (if (decided-after-check? target-obligation-reviews check)
            (log/info "Not inheriting kk application payment permit check of attachment" attachment-key
                      "from application" (:key application) "to application" (:key target)
                      "as the payment obligation has been decided on after the check")
            {:target            target
             :source            application
             :check             check
             :attachment-key    attachment-key
             :attachment-states (->> (get payment-module/kk-application-payment-attachment-keys-by-option option)
                                     (filter (partial delivered? answers))
                                     set
                                     (latest-attachment-reviews (:key application)))}))))))

(defn- inherit!
  "Kopioi tarkistuksen, liitemerkinnät ja tarkastetun maksuvelvollisuuden yhdessä transaktiossa. Palauttaa perityn
   tarkistuksen."
  [{:keys [target source check attachment-key attachment-states]} audit-logger]
  (let [target-key (:key target)
        now        (time/now)]
    (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
      (when-let [inherited (store/insert-inherited-permit-check!
                             connection
                             {:application_key                 target-key
                              :attachment_key                  attachment-key
                              :found_in_migri                  (:found-in-migri check)
                              :permit_valid_until              (:permit-valid-until check)
                              :payment_obligation_basis        (:payment-obligation-basis check)
                              :payment_obligation_other_reason (:payment-obligation-other-reason check)
                              :virkailija_oid                  (:virkailija-oid check)
                              :organization_oid                (:organization-oid check)
                              :checked_time                    (:checked-time check)
                              ; Eteenpäin periytyvä tarkistus viittaa hakemukseen, jolla se alun perin tehtiin
                              :inherited_from_application_key  (or (:inherited-from-application-key check)
                                                                   (:key source))})]
        (log/info "Inheriting kk application payment permit check of attachment" attachment-key
                  "from application" (:key source) "to application" target-key)
        (application-store/add-application-event-in-tx connection
                                                       {:application-key target-key
                                                        :event-type      inherited-event-type
                                                        :review-key      attachment-key}
                                                       nil)
        (doseq [{:keys [hakukohde] review-attachment-key :attachment-key :as attachment-review}
                (application-store/get-application-attachment-reviews target-key false)
                :let [state (get attachment-states review-attachment-key)]
                :when (and (some? state) (not (reviewed-after-check? attachment-review check)))]
          (application-store/save-inherited-attachment-review-in-tx connection target-key hakukohde
                                                                    review-attachment-key state now
                                                                    audit-log-session audit-logger))
        (doseq [hakukohde (:hakukohde target)]
          (application-store/save-inherited-application-hakukohde-review-in-tx connection target-key hakukohde
                                                                               "kk-application-payment-obligation"
                                                                               "reviewed" now audit-log-session
                                                                               audit-logger))
        inherited))))

(defn inherit-permit-checks!
  "Periyttää tarkistukset hakijan saman alkamiskauden uusimmille hakemuksille.
   - Hakemukset käsitellään jättöjärjestyksessä, jotta ajossa periytynyt tarkistus voi periytyä edelleen.
   - Hakemukset, joiden maksu ei voi enää muuttua (final-application-keys), eivät peri, mutta niiden tarkistukset
     periytyvät.
   - Kun liite puuttuu, lähteen maksua ei saa vaatia ajon alussa (not-required-application-keys), vaikka lähde olisi
     perinyt tarkistuksensa aiemmin samassa ajossa.
   Palauttaa niiden hakemusten avaimet, jotka perivät tarkistuksen."
  [audit-logger applications final-application-keys not-required-application-keys]
  (let [applications (sort-by (comp time/to-instant :submitted) applications)]
    (->> applications
         (remove (comp final-application-keys :key))
         (keep (fn [target]
                 (when-let [inheritance (inheritance-for applications not-required-application-keys target)]
                   (when-let [inherited (inherit! inheritance audit-logger)]
                     (audit-log/log audit-logger
                                    {:new       inherited
                                     :id        {:applicationOid (:key target)
                                                 :attachmentKey  (:attachment-key inheritance)}
                                     :session   audit-log-session
                                     :operation audit-log/operation-new})
                     (:key target)))))
         doall)))
