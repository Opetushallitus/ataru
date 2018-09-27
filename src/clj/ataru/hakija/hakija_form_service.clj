(ns ataru.hakija.hakija-form-service
  (:require [ataru.config.core :refer [config]]
            [ataru.forms.form-store :as form-store]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.hakija.person-info-fields :refer [viewing-forbidden-person-info-field-ids
                                                     editing-forbidden-person-info-field-ids
                                                     editing-allowed-person-info-field-ids]]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [ataru.tarjonta-service.hakukohde :refer [populate-hakukohde-answer-options]]
            [taoensso.timbre :refer [warn]]
            [clj-time.core :as time]
            [clj-time.coerce :as t]
            [schema.core :as s]
            [ataru.hakija.form-role :as form-role]
            [ataru.component-data.component :as component]
            [medley.core :refer [find-first]]
            [ataru.util :as util]))

(defn- set-can-submit-multiple-applications-and-yhteishaku
  [multiple? yhteishaku? haku-oid field]
  (-> field
      (assoc-in [:params :can-submit-multiple-applications] multiple?)
      (assoc-in [:params :yhteishaku] yhteishaku?)
      (cond-> (not multiple?) (assoc-in [:params :haku-oid] haku-oid))))

(defn- map-if-ssn-or-email
  [f field]
  (if (or (= "ssn" (:id field))
          (= "email" (:id field)))
    (f field)
    field))

(defn set-submit-multiple-and-yhteishaku-if-ssn-or-email-field
  [multiple? yhteishaku? haku-oid]
  (partial map-if-ssn-or-email
           (partial set-can-submit-multiple-applications-and-yhteishaku
                    multiple? yhteishaku? haku-oid)))

(defn- update-ssn-and-email-fields-in-person-module
  [multiple? yhteishaku? haku-oid form]
  (when-let [person-module-idx (util/first-index-of #(= (:module %) "person-info") (:content form))]
    (let [person-module     (nth (:content form) person-module-idx)
          new-person-module (update person-module :children (set-submit-multiple-and-yhteishaku-if-ssn-or-email-field multiple? yhteishaku? haku-oid))]
      (assoc-in form [:content person-module-idx] new-person-module))))

(defn populate-can-submit-multiple-applications
  [form tarjonta-info]
  (let [multiple?   (get-in tarjonta-info [:tarjonta :can-submit-multiple-applications] true)
        yhteishaku? (get-in tarjonta-info [:tarjonta :yhteishaku] false)
        haku-oid    (get-in tarjonta-info [:tarjonta :haku-oid])]
    (or
      (update-ssn-and-email-fields-in-person-module multiple? yhteishaku? haku-oid form)
      (update form :content
              (fn [content]
                (clojure.walk/prewalk (set-submit-multiple-and-yhteishaku-if-ssn-or-email-field multiple? yhteishaku? haku-oid) content))))))

(defn- attachment-modify-grace-period
  [hakuaika]
  (or (:attachment-modify-grace-period-days hakuaika)
      (-> config
        :public-config
        (get :attachment-modify-grace-period-days 14))))

(defn- select-first-ongoing-hakuaika-or-hakuaika-with-last-ending [hakuajat]
  (if-let [ongoing-hakuaika (first (filter :on hakuajat))]
    ongoing-hakuaika
    (first (sort-by :end > (filter :end hakuajat)))))

(defn- select-hakuaika-for-field [field hakukohteet]
  (let [field-hakukohde-and-group-oids (set (concat (:belongs-to-hakukohteet field)
                                                    (:belongs-to-hakukohderyhma field)))
        relevant-hakukohteet (cond->> hakukohteet
                               (not-empty field-hakukohde-and-group-oids)
                               (filter #(not-empty (clojure.set/intersection field-hakukohde-and-group-oids
                                                                  (set (cons (:oid %) (:hakukohderyhmat %)))))))]
    (select-first-ongoing-hakuaika-or-hakuaika-with-last-ending
     (map :hakuaika relevant-hakukohteet))))

(defn- editing-allowed-by-hakuaika?
  [field hakukohteet application-in-processing-state?]
  (let [hakuaika            (select-hakuaika-for-field field hakukohteet)
        hakuaika-start      (some-> hakuaika :start t/from-long)
        hakuaika-end        (some-> hakuaika :end t/from-long)
        attachment-edit-end (some-> hakuaika-end (time/plus (time/days (attachment-modify-grace-period hakuaika))))
        hakukierros-end     (some-> hakuaika :hakukierros-end t/from-long)
        after?              (fn [t] (or (nil? t)
                                        (time/after? (time/now) t)))
        before?             (fn [t] (and (some? t)
                                         (time/before? (time/now) t)))]
    (or (nil? hakuaika)
        (and (not (and application-in-processing-state? (:jatkuva-haku? hakuaika)))
             (after? hakuaika-start)
             (or (before? hakuaika-end)
                 (and (before? attachment-edit-end)
                      (= "attachment" (:fieldType field)))
                 (and (before? hakukierros-end)
                      (contains? editing-allowed-person-info-field-ids
                        (keyword (:id field)))))))))

(defn- uneditable?
  [field hakukohteet roles application-in-processing-state?]
  (not (and (or (and (form-role/virkailija? roles)
                     (not (form-role/with-henkilo? roles)))
                (not (contains? editing-forbidden-person-info-field-ids (keyword (:id field)))))
            (or (form-role/virkailija? roles)
                (editing-allowed-by-hakuaika? field hakukohteet application-in-processing-state?))
            (or (form-role/virkailija? roles)
                (not (and (nil? hakukohteet)
                          application-in-processing-state?))))))

(s/defn ^:always-validate flag-uneditable-and-unviewable-fields :- s/Any
  [form :- s/Any
   hakukohteet :- s/Any
   roles :- [form-role/FormRole]
   application-in-processing-state? :- s/Bool]
  (update form :content
          (fn [content]
            (clojure.walk/prewalk
             (fn [field]
               (if (= "formField" (:fieldClass field))
                 (let [cannot-view? (and (contains? viewing-forbidden-person-info-field-ids
                                                    (keyword (:id field)))
                                         (not (form-role/virkailija? roles)))
                       cannot-edit? (or cannot-view?
                                        (uneditable? field hakukohteet roles application-in-processing-state?))]
                   (assoc field
                          :cannot-view cannot-view?
                          :cannot-edit cannot-edit?))
                 field))
             content))))

(s/defn ^:always-validate remove-required-hakija-validator-if-virkailija :- s/Any
  [form :- s/Any
   roles :- [form-role/FormRole]]
  (if (form-role/virkailija? roles)
    (update form :content
            (fn [content]
              (clojure.walk/prewalk
                (fn [field]
                  (if (= "formField" (:fieldClass field))
                    (update field :validators (partial remove #{"required-hakija"}))
                    field))
                content)))
    form))

(s/defn ^:always-validate fetch-form-by-key :- s/Any
  [key :- s/Any
   roles :- [form-role/FormRole]]
  (when-let [form (form-store/fetch-by-key key)]
    (when (not (:deleted form))
      (-> form
          (remove-required-hakija-validator-if-virkailija roles)
          koodisto/populate-form-koodisto-fields))))

(s/defn ^:always-validate fetch-form-by-key-with-flagged-fields :- s/Any
  [key :- s/Any
   roles :- [form-role/FormRole]
   hakukohteet :- s/Any
   application-in-processing-state? :- s/Bool]
  (some-> (fetch-form-by-key key roles)
          (flag-uneditable-and-unviewable-fields hakukohteet roles application-in-processing-state?)))

(s/defn ^:always-validate fetch-form-by-haku-oid :- s/Any
  [tarjonta-service :- s/Any
   organization-service :- s/Any
   ohjausparametrit-service :- s/Any
   haku-oid :- s/Any
   application-in-processing-state? :- s/Bool
   roles :- [form-role/FormRole]]
  (let [tarjonta-info (tarjonta-parser/parse-tarjonta-info-by-haku tarjonta-service organization-service ohjausparametrit-service haku-oid)
        form-keys     (->> (-> tarjonta-info :tarjonta :hakukohteet)
                        (map :form-key)
                        (distinct)
                        (remove nil?))
        hakukohteet   (get-in tarjonta-info [:tarjonta :hakukohteet])
        form          (when (= 1 (count form-keys))
                        (fetch-form-by-key-with-flagged-fields (first form-keys) roles hakukohteet application-in-processing-state?))]
    (when (not tarjonta-info)
      (throw (Exception. (str "No haku found for haku " haku-oid " and keys " (pr-str form-keys)))))
    (if form
      (-> form
          (merge tarjonta-info)
          (assoc :load-time (System/currentTimeMillis))
          (populate-hakukohde-answer-options tarjonta-info)
          (populate-can-submit-multiple-applications tarjonta-info))
      (warn "could not find local form for haku" haku-oid "with keys" (pr-str form-keys)))))

(s/defn ^:always-validate fetch-form-by-hakukohde-oid :- s/Any
  [tarjonta-service :- s/Any
   organization-service :- s/Any
   ohjausparametrit-service :- s/Any
   hakukohde-oid :- s/Any
   application-in-processing-state? :- s/Bool
   roles :- [form-role/FormRole]]
  (let [hakukohde (.get-hakukohde tarjonta-service hakukohde-oid)
        form      (fetch-form-by-haku-oid tarjonta-service
                                          organization-service
                                          ohjausparametrit-service
                                          (:hakuOid hakukohde)
                                          false
                                          roles)]
    (when form
      (-> form
          (assoc :load-time (System/currentTimeMillis))
          (assoc-in [:tarjonta :default-hakukohde]
                    (some #(when (= hakukohde-oid (:oid %)) %)
                      (:hakukohteet (:tarjonta form))))))))
