(ns ataru.excel-common
  (:require [ataru.translations.texts :refer [excel-texts virkailija-texts]]
            [clojure.set :as set]))

(def hakemuksen-yleiset-tiedot-field-labels
  [{:id        "application-number"
    :label     (:application-number excel-texts)}
   {:id        "application-submitted-time"
    :label     (:application-submitted excel-texts)}
   {:id        "application-modified-time"
    :label     (:application-modified excel-texts)}
   {:id        "application-state"
    :label     (:application-state excel-texts)}
   {:id        "student-number"
    :label     (:student-number excel-texts)}
   {:id        "applicant-oid"
    :label     (:applicant-oid excel-texts)}
   {:id        "turvakielto"
    :label     (:turvakielto excel-texts)}])

(def kasittelymerkinnat-field-labels
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
    :label     (:notes excel-texts)}
   {:id        "hakemusmaksuvelvollisuus"
    :label     (:hakemusmaksuvelvollisuus excel-texts)}
   {:id        "kk-payment-state"
    :label     (:kk-payment-state excel-texts)}])

(def common-field-labels
  [{:id "hakemuksen-yleiset-tiedot"
    :label (:excel-hakemuksen-yleiset-tiedot virkailija-texts)
    :children (map #(select-keys % [:id :label]) hakemuksen-yleiset-tiedot-field-labels)}
   {:id "kasittelymerkinnat"
    :label (:excel-kasittelymerkinnat virkailija-texts)
    :children (map #(select-keys % [:id :label]) kasittelymerkinnat-field-labels)}])

(defn- vals-if-map [x] (if (map? x) (vals x) x))

(defn hakukohde-to-hakukohderyhma-oids [all-hakukohteet selected-hakukohde]
  (some->> (vals-if-map @all-hakukohteet)
           (filter #(= selected-hakukohde (:oid %)))
           (first)
           :ryhmaliitokset))

(defn hakukohderyhma-to-hakukohde-oids [all-hakukohteet selected-hakukohderyhma]
  (->> (vals-if-map @all-hakukohteet)
       (filter #(contains? (set (:ryhmaliitokset %)) selected-hakukohderyhma))
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

