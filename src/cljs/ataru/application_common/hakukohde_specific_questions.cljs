(ns ataru.application-common.hakukohde-specific-questions)

(declare change-followups-for-question)

(defn- change-followup-id
  [followup hakukohde-oid created-during-form-load]
  (-> followup
    (assoc :id (str (:id followup) "_" hakukohde-oid)
           :duplikoitu-followup-hakukohde-oid hakukohde-oid
           :original-followup (:id followup)
           :created-during-form-load created-during-form-load)
    (change-followups-for-question hakukohde-oid)))

(defn- change-followups-for-option
  [option hakukohde-oid created-during-form-load]
  (if-let [followups (seq (:followups option))]
    (assoc option :followups (map #(change-followup-id % hakukohde-oid created-during-form-load) followups))
    option))

(defn change-followups-for-question
  ([question hakukohde-oid]
   (change-followups-for-question question hakukohde-oid false))
  ([question hakukohde-oid created-during-form-load]
  (if-let [options (seq (:options question))]
    (assoc question :options (map #(change-followups-for-option % hakukohde-oid created-during-form-load) options))
    question)))
