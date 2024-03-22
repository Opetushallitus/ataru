(ns ataru.virkailija.application.application-selectors
  (:require [clojure.string :as string]))

(defn application-list-selected-by
  [db]
  (let [db-application (:application db)]
    (cond
      (:selected-form-key db-application)       :selected-form-key
      (:selected-haku db-application)           :selected-haku
      (:selected-hakukohde db-application)      :selected-hakukohde
      (:selected-hakukohderyhma db-application) :selected-hakukohderyhma)))

(defn selected-hakukohderyhma-hakukohteet
  [db]
  (when-let [[_ hakukohderyhma-oid] (get-in db [:application :selected-hakukohderyhma])]
    (->> (:hakukohteet db)
         vals
         (filter (fn [hakukohde]
                   (some #(= hakukohderyhma-oid %)
                         (:ryhmaliitokset hakukohde)))))))

(defn hakukohde-oids-from-selected-hakukohde-or-hakukohderyhma
  [db]
  (case (application-list-selected-by db)
    :selected-hakukohde      #{(get-in db [:application :selected-hakukohde])}
    :selected-hakukohderyhma (if-let [h (get-in db [:application :rajaus-hakukohteella])]
                               #{h}
                               (set (map :oid (selected-hakukohderyhma-hakukohteet db))))
    nil))

(defn selected-hakukohde-oid-set
  [db]
  (let [hakukohde-oids-from-hakukohde-or-ryhma (hakukohde-oids-from-selected-hakukohde-or-hakukohderyhma db)]
    (cond
      (some? hakukohde-oids-from-hakukohde-or-ryhma)
      hakukohde-oids-from-hakukohde-or-ryhma
      (some? (-> db :application :selected-form-key))
      #{"form"}
      :else
      nil)))

(defn selected-application-answers [db]
  (get-in db [:application :selected-application-and-form :application :answers]))

(defn get-tutu-payment-note-input [db application-key]
  (or
   (get-in db [:tutu-payment :inputs application-key :note])
   ""))

(defn get-tutu-payment-amount-input [db application-key]
  (or
   (get-in db [:tutu-payment :inputs application-key :amount])
   (get-in db [:tutu-payment :applications application-key :decision :amount])
   ""))

(defn get-tutu-form? [tutu-key]
  (let [tutu-forms (string/split (aget js/config "tutu-payment-form-keys") #",")]
    (boolean
     (and
      (not-empty tutu-forms)
      (some #(= tutu-key %) tutu-forms)))))