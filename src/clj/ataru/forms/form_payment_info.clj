(ns ataru.forms.form-payment-info
  (:require [ataru.middleware.user-feedback :refer [user-feedback-exception]]
            [taoensso.timbre :as log]
            [ataru.config.core :refer [config]]
            [ataru.kk-application-payment.utils :as utils]))

(def form-payment-types
    #{:payment-type-tutu
      :payment-type-kk
      :payment-type-astu
      nil})

(def kk-processing-fee
  (bigdec (get-in config [:kk-application-payments :processing-fee])))

(defn- valid-fees?
  [payment-type processing-fee decision-fee]
  (let [fee-nonpositive? (fn [amount] (and (some? amount) (<= amount 0.00M)))
        incorrect-kk-fee? (fn [payment-type processing decision]
                            (and (= :payment-type-kk payment-type)
                                 (or (and processing (not= processing kk-processing-fee))
                                     (some? decision))))
        incorrect-tutu-fee? (fn [payment-type processing decision]
                              (and (= :payment-type-tutu payment-type)
                                   (or (nil? processing)
                                       (some? decision))))
        incorrect-astu-fee? (fn [payment-type processing decision]
                              (and (= :payment-type-astu payment-type)
                                   (or (some? processing)
                                       (some? decision))))]
    (cond
      (fee-nonpositive? processing-fee)
      (do (log/warn "Nonpositive processing fee: " processing-fee)
          false)

      (fee-nonpositive? decision-fee)
      (do (log/warn "Nonpositive decision fee: " decision-fee)
          false)

      (incorrect-kk-fee? payment-type processing-fee decision-fee)
      (do (log/warn "Incorrect kk fees: " [payment-type processing-fee decision-fee])
          false)

      (incorrect-tutu-fee? payment-type processing-fee decision-fee)
      (do (log/warn "Incorrect TUTU fees: " [payment-type processing-fee decision-fee])
          false)

      (incorrect-astu-fee? payment-type processing-fee decision-fee)
      (do (log/warn "Incorrect ASTU fees: " [payment-type processing-fee decision-fee])
          false)

      :else true)))

(defn- add-payment-type
  "Adds the payment type info to a form if valid"
  [form payment-type]
  (if (contains? form-payment-types payment-type)
    (assoc-in form [:properties :payment :type] (name payment-type))
    (throw (user-feedback-exception (str "Maksutyyppi ei tuettu: " payment-type)))))

(defn- add-fees
  "Adds fee amounts for form if valid. Forces hardcoded values for specific payment types."
  [form payment-type processing-fee decision-fee]
  (if (valid-fees? payment-type processing-fee decision-fee)
    (-> form
        (assoc-in [:properties :payment :processing-fee] (if processing-fee
                                                           (str processing-fee)
                                                           nil))
        (assoc-in [:properties :payment :decision-fee] (if decision-fee
                                                         (str decision-fee)
                                                         nil)))
    (throw (user-feedback-exception
             (str "Maksutiedot virheelliset: " [payment-type processing-fee decision-fee])))))

(defn- add-vat
  "Adds vat to form if valid."
  [form vat]
  (if (and (some? vat) (<= vat 0.00M))
    (user-feedback-exception
      (str "ALV virheellinen: " [vat]))
    (assoc-in form [:properties :payment :vat] vat)))

(defn- add-payment-info-to-form
  [form payment-type processing-fee decision-fee vat]
  (let [payment-type-kw    (keyword payment-type)
        coerce-bigdec-fn   (fn [fee]
                             (when fee
                               (try (bigdec fee)
                                    (catch Exception _
                                      (throw (user-feedback-exception (str "Maksusumma ei numero: " fee)))))))
        processing-fee-num (coerce-bigdec-fn processing-fee)
        decision-fee-num   (coerce-bigdec-fn decision-fee)
        vat-num (when (not-empty vat) (coerce-bigdec-fn vat))]
    (-> form
        (add-payment-type payment-type-kw)
        (add-fees payment-type-kw processing-fee-num decision-fee-num)
        (add-vat vat-num))))

(defn- add-payment-info-if-higher-education
  "Set payment info if form is attached to one or more matching higher education admissions."
  [form tarjonta-service haku hakukohde-oids]
    (if (utils/requires-higher-education-application-fee? tarjonta-service haku hakukohde-oids)
      (add-payment-info-to-form form :payment-type-kk kk-processing-fee nil nil)
      form))

(defn set-payment-info
  "Sets the payment amount for the form. Input and output fees are decimal strings in form \"1234.56\"."
  [form payment-properties]
  (if (empty? payment-properties)
    (assoc-in form [:properties :payment] payment-properties)
    (let [payment-type (:type payment-properties)
          processing-fee (:processing-fee payment-properties)
          decision-fee (:decision-fee payment-properties)
          vat (:vat payment-properties)]
      (if (= :payment-type-kk (keyword payment-type))
        (throw (user-feedback-exception
                 (str "Hakemusmaksua ei voi asettaa manuaalisesti: " [payment-type processing-fee decision-fee])))
        (add-payment-info-to-form form payment-type processing-fee decision-fee vat)))))

(defn populate-form-with-payment-info
  "Adds payment info for form. Should be always used to get payment info rather than querying
   properties directly, because type and fees may be set and overridden dynamically."
  [form tarjonta-service haku]
  ; This should work with both full tarjonta data with extended hakukohde data and normal hakus with just OIDs so...
  (let [hakukohde-oids (or (->> (:hakukohteet haku)
                                (map #(:oid %))
                                (filter some?)
                                (not-empty))
                           (:hakukohteet haku))
        form-with-possible-kk-fees (add-payment-info-if-higher-education
                                     form tarjonta-service haku hakukohde-oids)]
    form-with-possible-kk-fees))

(defn add-admission-payment-info-for-haku
  "Adds info about admission payment requirement to a haku object"
  [tarjonta-service haku]
  (let [admission-payment-required? (utils/requires-higher-education-application-fee?
                                      tarjonta-service
                                      haku
                                      (:hakukohteet haku))]
    (assoc haku :admission-payment-required? admission-payment-required?)))
