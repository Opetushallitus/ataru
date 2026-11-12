(ns ataru.application.application-states
  (:require [ataru.application.review-states :as review-states]
            [ataru.component-data.kk-application-payment-module :as payment-module]
            [clojure.set :as set]))

(defn get-review-state-label-by-name
  [states name lang]
  (->> states
       (filter #(= (first %) name))
       first
       second
       lang))

(defn get-all-reviews-for-requirement
  "Adds default (incomplete) reviews where none have yet been created"
  [review-requirement-name application selected-hakukohde-oids]
  (let [application-hakukohteet (set (:hakukohde application))
        hakukohde-filter        (set selected-hakukohde-oids)
        has-hakukohteet?        (not (empty? application-hakukohteet))
        review-targets          (cond
                                  (not-empty selected-hakukohde-oids) hakukohde-filter
                                  has-hakukohteet? application-hakukohteet
                                  :else #{"form"})
        relevant-states         (filter #(and
                                           (= (:requirement %) review-requirement-name)
                                           (= has-hakukohteet? (not= (:hakukohde %) "form"))
                                           (or (empty? hakukohde-filter)
                                               (contains? hakukohde-filter (:hakukohde %))))
                                        (:application-hakukohde-reviews application))
        unreviewed-targets      (set/difference review-targets (set (map :hakukohde relevant-states)))
        default-state-name      (-> (filter #(= (keyword review-requirement-name) (first %))
                                            review-states/hakukohde-review-types)
                                    (first)
                                    (last)
                                    (ffirst)
                                    (name))]
    (into relevant-states (map
                            (fn [oid] {:requirement review-requirement-name
                                       :state       default-state-name
                                       :hakukohde   oid})
                            unreviewed-targets))))

(defn get-all-reviews-for-all-requirements
  ([application selected-hakukohde-oids]
   (mapcat
     #(get-all-reviews-for-requirement % application selected-hakukohde-oids)
     review-states/hakukohde-review-type-names))
  ([application]
    (get-all-reviews-for-all-requirements application nil)))

(defn attachment-reviews-with-no-requirements [application]
  (let [reviews (:application-attachment-reviews application)
        no-reqs (set/difference (set (:hakukohde application))
                  (set (map :hakukohde reviews)))]
    (concat reviews (map (fn [oid] {:hakukohde oid :state review-states/no-attachment-requirements}) no-reqs))))

(defn remove-attachment-reviews-ignored-in-filtering
  "Poistaa liitemerkinnöistä ne, joita ei oteta huomioon, kun hakemuslistaa suodatetaan liitteiden tilan perusteella.
   Dokumenttivaihtoehdon puuttuviksi merkittyjä liitteitä ei oteta huomioon, kun virkailijan oleskelulupatarkistus
   ratkaisee hakemusmaksuvelvollisuuden liitemerkinnöistä riippumatta (ks.
   payment-module/permit-check-decides-payment-obligation?). Hakijalle ei silloin tarvitse lähettää niistä
   muistutusta. Muut puuttuvat liitteet löytyvät suodatuksessa edelleen.
   Hakemukselta luetaan tarkistukset (:kk-payment-permit-checks-for-filtering) ja hakukohteiden käsittelymerkinnät
   (:application-hakukohde-reviews)."
  [application attachment-reviews]
  (let [ignored-attachment-keys (->> (:kk-payment-permit-checks-for-filtering application)
                                     (filter #(payment-module/permit-check-decides-payment-obligation?
                                                %
                                                (:application-hakukohde-reviews application)))
                                     (mapcat (comp payment-module/kk-application-payment-attachment-keys-by-residence-permit-attachment-key
                                                   :attachment-key))
                                     set)]
    (remove #(and (= "attachment-missing" (:state %))
                  (contains? ignored-attachment-keys (:attachment-key %)))
            attachment-reviews)))

(defn attachment-reviews-for-filtering
  "Liitemerkinnät, joiden mukaan hakemuslistaa suodatetaan liitteiden tilan perusteella (ks.
   remove-attachment-reviews-ignored-in-filtering). Hakukohteella, jolle ei jää merkintöjä, ei ole liitepyyntöjä,
   jotta hakemus löytyy edelleen, kun kaikki tilat on valittu."
  [application]
  (attachment-reviews-with-no-requirements
    (update application :application-attachment-reviews
            (partial remove-attachment-reviews-ignored-in-filtering application))))

(defn attachment-review-states-filter-matches?
  "Löytyykö hakemus kenttäkohtaisella liitesuodattimella, kun suodatuksessa ohitettavia liitemerkintöjä ei oteta
   huomioon (ks. remove-attachment-reviews-ignored-in-filtering). Hakemuslistan kysely rajaa hakemukset samoilla
   ehdoilla ottaen huomioon kaikki merkinnät, joten tämä voi vain karsia kyselyn tuloksia.
   - attachment-key: suodatettava liite
   - states: hyväksytyt tilat, tai tyhjä, jolloin kaikki tilat hyväksytään
   - hakukohteet: hakukohteet, joiden merkinnät otetaan huomioon, tai tyhjä, jolloin kaikki"
  [application attachment-key states hakukohteet]
  (let [states      (set states)
        hakukohteet (set hakukohteet)]
    (boolean
      (some #(and (= attachment-key (:attachment-key %))
                  (or (empty? states) (contains? states (:state %)))
                  (or (empty? hakukohteet) (contains? hakukohteet (:hakukohde %))))
            (remove-attachment-reviews-ignored-in-filtering application (:application-attachment-reviews application))))))
