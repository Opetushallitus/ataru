(ns ataru.applications.harkinnanvaraisuus.harkinnanvaraisuus-util
  (:require [ataru.application.harkinnanvaraisuus-types :refer [harkinnanvaraisuus-reasons]]
            [clojure.walk :refer [keywordize-keys]]
            [ataru.component-data.base-education-module-2nd :refer [base-education-option-values-affecting-harkinnanvaraisuus
                                                                    yksilollistetty-key-values-affecting-harkinnanvaraisuus
                                                                    base-education-choice-key]]))

(defn get-common-harkinnanvaraisuus-reason
  [answers]
  (let [base-education-value ((keyword base-education-choice-key) answers)
        key-affecting-harkinnanvaraisuus-value (->> yksilollistetty-key-values-affecting-harkinnanvaraisuus
                                                    keys
                                                    (filter #(seq (% answers)))
                                                    first)]
    (cond
      (and base-education-value
           (= (:ulkomailla-suoritettu-value base-education-option-values-affecting-harkinnanvaraisuus)
              base-education-value))
      (:ataru-ulkomailla-opiskelu harkinnanvaraisuus-reasons)

      (and base-education-value
           (= (:ei-paattotodistusta-value base-education-option-values-affecting-harkinnanvaraisuus)
              base-education-value))
      (:ataru-ei-paattotodistusta harkinnanvaraisuus-reasons)

      (and key-affecting-harkinnanvaraisuus-value
           (= (key-affecting-harkinnanvaraisuus-value yksilollistetty-key-values-affecting-harkinnanvaraisuus)
              (key-affecting-harkinnanvaraisuus-value answers)))
      (:ataru-yks-mat-ai harkinnanvaraisuus-reasons))))

(defn get-harkinnanvaraisuus-reason-for-hakukohde
  [answers hakukohde-oid]
  (let [harkinnanvaraisuus-reason-key (keyword (str "harkinnanvaraisuus-reason_" hakukohde-oid))
        harkinnanvaraisuus-answer (harkinnanvaraisuus-reason-key answers)]
    (cond
      (= "0" harkinnanvaraisuus-answer)
      (:ataru-oppimisvaikeudet harkinnanvaraisuus-reasons)

      (= "1" harkinnanvaraisuus-answer)
      (:ataru-sosiaaliset-syyt harkinnanvaraisuus-reasons)

      (= "2" harkinnanvaraisuus-answer)
      (:ataru-koulutodistusten-vertailuvaikeudet harkinnanvaraisuus-reasons)

      (= "3" harkinnanvaraisuus-answer)
      (:ataru-riittamaton-tutkintokielen-taito harkinnanvaraisuus-reasons)

      :else
      (:none harkinnanvaraisuus-reasons))))

(defn assoc-harkinnanvaraisuustieto
  [tarjonta-application]
  (let [answers     (keywordize-keys (:keyValues tarjonta-application))
        hakukohteet (:hakutoiveet tarjonta-application)
        common-harkinnanvaraisuus (get-common-harkinnanvaraisuus-reason answers)
        assoc-harkinnanvaraisuus-fn (fn [hakukohde]
                                        (assoc hakukohde
                                          :harkinnanvaraisuus
                                          (get-harkinnanvaraisuus-reason-for-hakukohde answers (:hakukohdeOid hakukohde))))]
    (if common-harkinnanvaraisuus
      (assoc tarjonta-application :hakutoiveet (map #(assoc % :harkinnanvaraisuus common-harkinnanvaraisuus) hakukohteet))
      (assoc tarjonta-application :hakutoiveet (map assoc-harkinnanvaraisuus-fn hakukohteet)))))