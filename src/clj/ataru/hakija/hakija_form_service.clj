(ns ataru.hakija.hakija-form-service
  (:require [ataru.forms.form-store :as form-store]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [taoensso.timbre :refer [warn]]))

(defn fetch-form-by-key
  [key]
  (let [form (form-store/fetch-by-key key)]
    (when (and (some? form)
               (not (true? (:deleted form))))
      (-> form
          (koodisto/populate-form-koodisto-fields)))))

(defn- koulutukset->str
  "Produces a condensed string to better identify a hakukohde by its koulutukset"
  [koulutukset]
  (->> koulutukset
       (map (fn [koulutus]
              (->> [(:koulutuskoodi-name koulutus)
                    (:tutkintonimike-name koulutus)
                    (:tarkenne koulutus)]
                   (remove clojure.string/blank?)
                   (distinct)
                   (clojure.string/join ", "))))
       (remove clojure.string/blank?)
       (distinct)
       (clojure.string/join "; ")))

(defn- populate-hakukohde-answer-options [form tarjonta-info]
  (update form :content
          (fn [content]
            (clojure.walk/prewalk
             (fn [field]
               (if (= (:fieldType field) "hakukohteet")
                 (-> field
                     (assoc :options
                            (map (fn [{:keys [oid name koulutukset]}]
                                   {:value oid
                                    :label {:fi (or (:fi name) "")
                                            :sv (or (:sv name) "")
                                            :en (or (:en name) "")}
                                    :description {:fi (koulutukset->str koulutukset)
                                                  :sv ""
                                                  :en ""}})
                                 (get-in tarjonta-info [:tarjonta :hakukohteet])))
                     (assoc-in [:params :max-hakukohteet] (get-in tarjonta-info [:tarjonta :max-hakukohteet])))
                 field))
             content))))

(defn fetch-form-by-haku-oid
  [tarjonta-service haku-oid]
  (let [tarjonta-info (tarjonta-parser/parse-tarjonta-info-by-haku tarjonta-service haku-oid)
        form-keys     (->> (-> tarjonta-info :tarjonta :hakukohteet)
                           (map :form-key)
                           (distinct)
                           (remove nil?))
        form          (when (= 1 (count form-keys))
                        (fetch-form-by-key (first form-keys)))]
    (when (not tarjonta-info)
      (throw (Exception. (str "No haku found for haku" haku-oid "and keys" (pr-str form-keys)))))
    (if form
      (populate-hakukohde-answer-options (merge form (assoc-in tarjonta-info [:tarjonta :hakukohteet] []))
                                         tarjonta-info)
      (warn "could not find local form for haku" haku-oid "with keys" (pr-str form-keys)))))

(defn fetch-form-by-hakukohde-oid
  [tarjonta-service hakukohde-oid]
  (let [hakukohde (.get-hakukohde tarjonta-service hakukohde-oid)
        form      (fetch-form-by-haku-oid tarjonta-service (:hakuOid hakukohde))]
    (when form
      (assoc-in
        form
        [:tarjonta :default-hakukohde]
        (tarjonta-parser/parse-hakukohde tarjonta-service hakukohde)))))
