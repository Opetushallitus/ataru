(ns ataru.application-common.hakukohde-specific-questions)

(declare change-followups-for-question)

(defn- change-followup-id
  [followup hakukohde-oid]
  (-> followup
    (assoc :id (str (:id followup) "_" hakukohde-oid)
           :duplikoitu-followup-hakukohde-oid hakukohde-oid
           :original-followup (:id followup))
    (change-followups-for-question hakukohde-oid)))

(defn- change-followups-for-option
  [option hakukohde-oid]
  (if-let [followups (seq (:followups option))]
    (assoc option :followups (map #(change-followup-id % hakukohde-oid) followups))
    option))

(defn change-followups-for-question
  [question hakukohde-oid]
  (if-let [options (seq (:options question))]
    (assoc question :options (map #(change-followups-for-option % hakukohde-oid) options))
    question))
