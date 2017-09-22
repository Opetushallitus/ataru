(ns ataru.hakija.hakija-form-service
  (:require [ataru.forms.form-store :as form-store]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [ataru.tarjonta-service.hakukohde :refer [populate-hakukohde-answer-options]]
            [taoensso.timbre :refer [warn]]
            [ataru.virkailija.component-data.component :as component]))

(defn inject-hakukohde-component-if-missing
  "Add hakukohde component to legacy forms (new ones have one added on creation)"
  [form]
  (let [has-hakukohde-component? (-> (filter #(= (keyword (:id %)) :hakukohteet) (:content form))
                                     (first)
                                     (not-empty))]
    (if has-hakukohde-component?
      form
      (update-in form [:content] #(into [(component/hakukohteet)] %)))))

(defn populate-can-submit-multiple-applications
  [form tarjonta-info]
  (let [csma :can-submit-multiple-applications
        multiple? (get-in tarjonta-info [:tarjonta csma] true)
        haku-oid (get-in tarjonta-info [:tarjonta :haku-oid])]
    (update form :content
            (fn [content]
              (clojure.walk/prewalk
               (fn [field]
                 (if (or (= "ssn" (:id field))
                         (= "email" (:id field)))
                   (cond-> (assoc-in field [:params csma] multiple?)
                     (not multiple?) (assoc-in [:params :haku-oid] haku-oid))
                   field))
               content)))))

(defn fetch-form-by-key
  [key]
  (let [form (form-store/fetch-by-key key)]
    (when (and (some? form)
               (not (true? (:deleted form))))
      (-> form
          (koodisto/populate-form-koodisto-fields)))))

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
      (throw (Exception. (str "No haku found for haku " haku-oid " and keys " (pr-str form-keys)))))
    (if form
      (-> form
          ; remove hakukohteet from form tarjonta for deduplication
          (merge (assoc-in tarjonta-info [:tarjonta :hakukohteet] []))
          (inject-hakukohde-component-if-missing)
          (populate-hakukohde-answer-options tarjonta-info)
          (populate-can-submit-multiple-applications tarjonta-info))
      (warn "could not find local form for haku" haku-oid "with keys" (pr-str form-keys)))))

(defn fetch-form-by-hakukohde-oid
  [tarjonta-service hakukohde-oid]
  (let [hakukohde (.get-hakukohde tarjonta-service hakukohde-oid)
        form      (fetch-form-by-haku-oid tarjonta-service (:hakuOid hakukohde))]
    (when form
      (-> form
          (assoc-in [:tarjonta :default-hakukohde] (tarjonta-parser/parse-hakukohde tarjonta-service hakukohde))))))
