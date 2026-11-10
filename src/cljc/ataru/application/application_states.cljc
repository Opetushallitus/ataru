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
   Dokumenttivaihtoehdon puuttuviksi merkittyjä liitteitä ei oteta huomioon, kun virkailija on tarkastanut hakijan
   oleskeluluvan ja todennut sen löytyvän Migrin palvelusta, maksuvelvollisuuden peruste on merkitty ja
   hakemusmaksuvelvollisuus on tarkastettu (:kk-payment-permit-found-in-migri-attachment-keys). Tarkistus ratkaisee
   silloin hakemusmaksuvelvollisuuden liitteistä riippumatta, joten hakijalle ei tarvitse lähettää niistä
   muistutusta. Muut puuttuvat liitteet löytyvät suodatuksessa edelleen."
  [application attachment-reviews]
  (let [ignored-attachment-keys (->> (:kk-payment-permit-found-in-migri-attachment-keys application)
                                     (mapcat payment-module/kk-application-payment-attachment-keys-by-residence-permit-attachment-key)
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
