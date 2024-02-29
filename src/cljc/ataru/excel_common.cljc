(ns ataru.excel-common
  (:require [ataru.translations.texts :refer [excel-texts virkailija-texts]]
            [ataru.util :refer [to-vec]]
            [clojure.set :as set]))

(def hakemuksen-yleiset-tiedot-fields
  [{:id        "application-number"
    :label     (:application-number excel-texts)}
   {:id        "application-created-time"
    :label     (:sent-at excel-texts)}
   {:id        "application-state"
    :label     (:application-state excel-texts)}
   {:id        "student-number"
    :label     (:student-number excel-texts)}
   {:id        "applicant-oid"
    :label     (:applicant-oid excel-texts)}
   {:id        "turvakielto"
    :label     (:turvakielto excel-texts)}])

(def kasittelymerkinnat-fields
  [{:id        "hakukohde-handling-state"
    :label     (:hakukohde-handling-state excel-texts)}
   {:id        "kielitaitovaatimus"
    :label     (:kielitaitovaatimus excel-texts)}
   {:id        "tutkinnon-kelpoisuus"
    :label     (:tutkinnon-kelpoisuus excel-texts)}
   {:id        "hakukelpoisuus"
    :label     (:hakukelpoisuus excel-texts)}
   {:id        "eligibility-set-automatically"
    :label     (:eligibility-set-automatically virkailija-texts)}
   {:id        "ineligibility-reason"
    :label     (:ineligibility-reason virkailija-texts)}
   {:id        "maksuvelvollisuus"
    :label     (:maksuvelvollisuus excel-texts)}
   {:id        "valinnan-tila"
    :label     (:valinnan-tila excel-texts)}
   {:id        "ehdollinen"
    :label     (:ehdollinen excel-texts)}
   {:id        "pisteet"
    :label     (:pisteet excel-texts)}
   {:id        "application-review-notes"
    :label     (:notes excel-texts)}])

(defn hakukohde-to-hakukohderyhma-oids [all-hakukohteet selected-hakukohde]
  (some->> (first (filter #(= selected-hakukohde (:oid %)) @all-hakukohteet))
           :hakukohderyhmat))

(defn hakukohderyhma-to-hakukohde-oids [all-hakukohteet selected-hakukohderyhma]
  (->> @all-hakukohteet
       (filter #(contains? (set (:hakukohderyhmat %)) selected-hakukohderyhma))
       (map :oid)))

(defn form-field-belongs-to-hakukohde [form-field selected-hakukohde selected-hakukohderyhma all-hakukohteet]
  (let [belongs-not-specified? (and (empty? (:belongs-to-hakukohderyhma form-field))
                                    (empty? (:belongs-to-hakukohteet form-field)))]
    (cond
      (some? selected-hakukohde) (or
                                  belongs-not-specified?
                                  (contains?
                                   (set (:belongs-to-hakukohteet form-field))
                                   selected-hakukohde)
                                  (let [hakukohderyhmas (hakukohde-to-hakukohderyhma-oids all-hakukohteet selected-hakukohde)]
                                    (-> (set/intersection
                                         (set (:belongs-to-hakukohderyhma form-field))
                                         (set hakukohderyhmas))
                                        empty?
                                        not)))
      (some? selected-hakukohderyhma) (or
                                       belongs-not-specified?
                                       (contains?
                                        (set (:belongs-to-hakukohderyhma form-field))
                                        selected-hakukohderyhma)
                                       (let [hakukohderyhmas (->>
                                                              (:belongs-to-hakukohteet form-field)
                                                              (mapcat #(hakukohde-to-hakukohderyhma-oids all-hakukohteet %)))]
                                         (contains? (set hakukohderyhmas) selected-hakukohderyhma)))
      :else true)))

(defn assoc-in-excel [db k v]
  (assoc-in db (concat [:application :excel-request] (to-vec k)) v))

(defn get-in-excel [db k]
  (get-in db (concat [:application :excel-request] (to-vec k))))
