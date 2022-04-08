(ns ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-util
  (:require [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-types :refer [harkinnanvaraisuus-reasons]]
            [clojure.walk :refer [keywordize-keys]]
            [ataru.component-data.base-education-module-2nd :refer [base-education-option-values-affecting-harkinnanvaraisuus
                                                                    yksilollistetty-key-values-affecting-harkinnanvaraisuus
                                                                    base-education-choice-key]]))

(defn get-common-harkinnanvaraisuus-reason
  [answers pick-value-fn]
  (let [base-education-value (pick-value-fn answers (keyword base-education-choice-key) )
        key-affecting-harkinnanvaraisuus-value (->> yksilollistetty-key-values-affecting-harkinnanvaraisuus
                                                    keys
                                                    (filter #(seq (pick-value-fn answers %)))
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
              (pick-value-fn answers key-affecting-harkinnanvaraisuus-value)))
      (:ataru-yks-mat-ai harkinnanvaraisuus-reasons))))

(defn get-targeted-harkinnanvaraisuus-reason-for-hakukohde
  [answers hakukohde pick-value-fn]
  (let [hakukohde-oid (:oid hakukohde)
        harkinnanvaraisuus-reason-key (keyword (str "harkinnanvaraisuus-reason_" hakukohde-oid))
        harkinnanvaraisuus-answer (pick-value-fn answers harkinnanvaraisuus-reason-key)]
    (cond
      (not (:voiko-hakukohteessa-olla-harkinnanvaraisesti-hakeneita? hakukohde))
      (:ei-harkinnanvarainen-hakukohde harkinnanvaraisuus-reasons)

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
  ;todo handle hakukode :voiko-hakukohteessa-olla-harkinnanvaraisesti-hakeneita?
  ; pass hakukohdes fetched from cache as parameter
  (let [answers     (keywordize-keys (:keyValues tarjonta-application))
        hakukohteet (:hakutoiveet tarjonta-application)
        pick-value-fn (fn [answers question]
                        (question answers))
        common-harkinnanvaraisuus (get-common-harkinnanvaraisuus-reason answers pick-value-fn)
        assoc-harkinnanvaraisuus-fn (fn [hakukohde]
                                        (assoc hakukohde
                                          :harkinnanvaraisuus
                                          (get-targeted-harkinnanvaraisuus-reason-for-hakukohde answers
                                                                                                (:hakukohdeOid hakukohde)
                                                                                                pick-value-fn)))]
    (if common-harkinnanvaraisuus
      (assoc tarjonta-application :hakutoiveet (map #(assoc % :harkinnanvaraisuus common-harkinnanvaraisuus) hakukohteet))
      (assoc tarjonta-application :hakutoiveet (map assoc-harkinnanvaraisuus-fn hakukohteet)))))

(defn get-harkinnanvaraisuus-reason-for-hakukohde
  [answers hakukohde]
  (let [answers (keywordize-keys answers)
        pick-value-fn (fn [answers question]
                        (:value (question answers)))]
    (or (get-common-harkinnanvaraisuus-reason answers pick-value-fn)
        (get-targeted-harkinnanvaraisuus-reason-for-hakukohde answers hakukohde pick-value-fn))))

(defn assoc-harkinnanvaraisuustieto-to-hakukohde
  [answers hakukohde]
  {:hakukohdeOid (:oid hakukohde)
   :harkinnanvaraisuudenSyy (get-harkinnanvaraisuus-reason-for-hakukohde answers hakukohde)})
