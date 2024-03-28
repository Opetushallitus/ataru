(ns ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-util
  (:require [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-types :refer [harkinnanvaraisuus-reasons]]
            [clojure.walk :refer [keywordize-keys]]
            [ataru.number :as number]
            [ataru.component-data.base-education-module-2nd :refer [base-education-option-values-affecting-harkinnanvaraisuus
                                                                    yksilollistetty-key-values-affecting-harkinnanvaraisuus
                                                                    base-education-option-where-harkinnanvaraisuus-do-not-need-to-be-checked
                                                                    base-education-choice-key
                                                                    suoritusvuosi-keys]]
            [taoensso.timbre :as log]))

;Kaikki 2017 tai myöhemmin valmistuneet perusopetukset pitäisi löytyä suoritusrekisteristä.
(defn perusopetus-should-be-in-sure [answers]
  (let [tutkinto-vuosi-key (->> suoritusvuosi-keys
                                (map keyword)
                                (filter #(not (nil? (% answers))))
                                first)
        tutkinto-vuosi (when tutkinto-vuosi-key
                         (-> answers tutkinto-vuosi-key :value))
        parsed-vuosi (if (int? tutkinto-vuosi)
                       tutkinto-vuosi
                       (when (not-empty tutkinto-vuosi)
                         (number/->int tutkinto-vuosi)))
        result (boolean (when (some? parsed-vuosi)
                          (>= parsed-vuosi 2017)))]
    (log/info (str "Should be in sure? " answers ", vuosi " parsed-vuosi ", result " result))
    result))

(defn can-skip-recheck-for-yks-ma-ai
  [application]
  (let [answers (:answers application)
        base-education-value (->> answers
                                  (filter #(= (:key %) base-education-choice-key))
                                  first
                                  :value)]
    (-> (vals base-education-option-where-harkinnanvaraisuus-do-not-need-to-be-checked)
        (set)
        (contains? base-education-value))))

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

; following returns true only if there are common harkinnanvaraisuus reasons
(defn does-application-belong-to-only-harkinnanvarainen-valinta?
  [application]
  (let [answers (:answers application)
        pick-value-fn (fn [answers question]
                        (->> answers
                             (filter #(= question (keyword (:key %))))
                             first
                             :value))
        common-reason (get-common-harkinnanvaraisuus-reason answers pick-value-fn)]
    (not (nil? common-reason))))

(defn decide-reason
  [common-reason targeted-reason perusopetus-should-be-in-sure]
  (let [result (cond
                 (= (:ei-harkinnanvarainen-hakukohde harkinnanvaraisuus-reasons) targeted-reason)
                 targeted-reason

                 (not (nil? common-reason))
                 common-reason

                 ;Tässä nojataan siihen, että Valintalaskentakoostepalvelun HarkinnanvaraisuusResourcen päättely yliajaa
                 ;tämän tiedon jos suresta löytyy suoritus
                 (and perusopetus-should-be-in-sure
                      (= targeted-reason (:none harkinnanvaraisuus-reasons)))
                 (:ataru-ei-paattotodistusta harkinnanvaraisuus-reasons)

                 :else
                 targeted-reason)]
    (log/info (str "Decide reason - common " common-reason ", targeted " targeted-reason ", sure? " perusopetus-should-be-in-sure ", result " result))
    result))

(defn assoc-harkinnanvaraisuustieto
  [hakukohteet tarjonta-application]
  (let [answers                       (keywordize-keys (:keyValues tarjonta-application))
        pick-value-fn                 (fn [answers question]
                                        (question answers))
        should-be-in-sure             (perusopetus-should-be-in-sure answers)
        common-reason                 (get-common-harkinnanvaraisuus-reason answers pick-value-fn)]
    (letfn [(get-hakukohde-for-hakutoive
              [hakutoive]
              (let [hakukohde-oid (:hakukohdeOid hakutoive)]
                (->> hakukohteet
                     (filter #(= hakukohde-oid (:oid %)))
                     first)))
            (get-targeted-reason
              [hakutoive]
              (let [hakukohde (get-hakukohde-for-hakutoive hakutoive)]
                (get-targeted-harkinnanvaraisuus-reason-for-hakukohde answers hakukohde pick-value-fn)))
            (assoc-harkinnanvaraisuustieto
              [hakutoive]
              (let [targeted-reason (get-targeted-reason hakutoive)
                    reason          (decide-reason common-reason targeted-reason should-be-in-sure)]
                (assoc hakutoive :harkinnanvaraisuus reason)))]
      (update tarjonta-application :hakutoiveet #(map assoc-harkinnanvaraisuustieto %)))))

(defn get-harkinnanvaraisuus-reason-for-hakukohde
  [answers hakukohde]
  (let [answers         (keywordize-keys answers)
        pick-value-fn   (fn [answers question]
                          (:value (question answers)))
        should-be-in-sure (perusopetus-should-be-in-sure answers)
        targeted-reason (get-targeted-harkinnanvaraisuus-reason-for-hakukohde answers hakukohde pick-value-fn)
        common-reason   (get-common-harkinnanvaraisuus-reason answers pick-value-fn)]
    (decide-reason common-reason targeted-reason should-be-in-sure)))

(defn assoc-harkinnanvaraisuustieto-to-hakukohde
  [answers hakukohde]
  {:hakukohdeOid (:oid hakukohde)
   :harkinnanvaraisuudenSyy (get-harkinnanvaraisuus-reason-for-hakukohde answers hakukohde)})
