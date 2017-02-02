(ns ataru.hakija.hakija-form-service
  (:require [ataru.forms.form-store :as form-store]
            [ataru.koodisto.koodisto :as koodisto]
            [clj-time.core :as time]
            [clj-time.coerce :as time-coerce]
            [taoensso.timbre :refer [warn]]))

(defn fetch-form-by-key
  [key]
  (let [form (form-store/fetch-by-key key)]
    (when (and (some? form)
               (not (true? (:deleted form))))
      (-> form
          (koodisto/populate-form-koodisto-fields)))))

(defn- time-within?
  ([instant {:keys [alkuPvm loppuPvm]}]
   (time-within? instant alkuPvm loppuPvm))
  ([instant start end]
   (time/within?
    (time/interval (time-coerce/from-long start)
                   (time-coerce/from-long end))
    instant)))

(defn- find-current-or-last-hakuaika
  [hakuaikas]
  (let [now              (time/now)
        current-hakuaika (first (filter (partial time-within? now) hakuaikas))]
    (or
      current-hakuaika
      (last hakuaikas))))

(defn- parse-hakuaika
  "Hakuaika from hakuaika can override hakuaika from haku. Haku may have multiple hakuaikas defined."
  [hakukohde haku]
  ; TODO need to check that hakukohde hakuaika is valid wrt. haku hakuaika?
  (if (and (:hakuaikaAlkuPvm hakukohde) (:hakuaikaLoppuPvm hakukohde))
    {:start (:hakuaikaAlkuPvm hakukohde)
     :end   (:hakuaikaLoppuPvm hakukohde)}
    (let [this-haku-hakuaika (find-current-or-last-hakuaika (:hakuaikas haku))]
      {:start (:alkuPvm this-haku-hakuaika)
       :end   (:loppuPvm this-haku-hakuaika)})))

(defn- get-hakuaika-info [hakukohde haku]
  (let [{start :start end :end :as interval} (parse-hakuaika hakukohde haku)]
    (if (and start end (time-within? (time/now) start end))
      (assoc interval :on true)
      (assoc interval :on false))))

(defn fetch-form-by-hakukohde-oid
  [tarjonta-service hakukohde-oid]
  (let [hakukohde (.get-hakukohde tarjonta-service hakukohde-oid)
        haku-oid  (:hakuOid hakukohde)
        haku      (when haku-oid (.get-haku tarjonta-service haku-oid))
        form-key  (:ataruLomakeAvain hakukohde)
        form      (when form-key (fetch-form-by-key form-key))]
    (when (and hakukohde
               (not haku))
      (throw (Exception. (str "No haku found for hakukohde" hakukohde-oid))))
    (if form
      (merge form
             {:tarjonta
              {:hakukohde-oid      hakukohde-oid
               :hakukohde-name     (-> hakukohde :hakukohteenNimet :kieli_fi)
               :haku-tarjoaja-name (-> hakukohde :tarjoajaNimet :fi)
               :haku-oid           haku-oid
               :haku-name          (-> haku :nimi :kieli_fi)
               :hakuaika-dates     (get-hakuaika-info hakukohde haku)}})
      (warn "could not find local form for hakukohde" hakukohde-oid "with key" form-key))))
