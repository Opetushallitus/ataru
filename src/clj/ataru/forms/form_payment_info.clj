(ns ataru.forms.form-payment-info
  (:require [ataru.middleware.user-feedback :refer [user-feedback-exception]]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [ataru.config.core :refer [config]]))

; TODO: should these come from eg. koodisto as discussed before? And if so, why?
(def form-payment-types
    #{:payment-type-tutu
      :payment-type-kk
      :payment-type-astu
      nil})

(def kk-processing-fee
  (bigdec (get-in config [:form-payment-info :kk-processing-fee])))

(def tutu-processing-fee
  (bigdec (get-in config [:tutkintojen-tunnustaminen :maksut :decision-amount])))

(defn- requires-higher-education-application-fee? [haku]
  ; TODO: "kohdejoukon tarkenne on joko tyhjä tai "siirtohaku"
  ; TODO: "kyseessä tutkintoon johtava koulutus"
  (str/starts-with? (:kohdejoukko-uri haku) "haunkohdejoukko_12#"))

(defn- valid-fees?
  [payment-type processing-fee decision-fee]
  (let [fee-nonpositive? (fn [amount] (and (some? amount) (<= amount 0.00M)))
        incorrect-kk-fee? (fn [payment-type processing-fee decision-fee]
                            (and (= :payment-type-kk payment-type)
                                 (or (not= processing-fee kk-processing-fee)
                                     (some? decision-fee))))
        incorrect-tutu-fee? (fn [payment-type processing-fee decision-fee]
                              (and (= :payment-type-tutu payment-type)
                                   (or (not= processing-fee tutu-processing-fee)
                                       (some? decision-fee))))
        incorrect-astu-fee? (fn [payment-type processing-fee _]
                              (and (= :payment-type-astu payment-type)
                                   (some? processing-fee)))]
    (cond
      (fee-nonpositive? processing-fee)
      (do (log/warn "Nonpositive processing fee: " processing-fee)
          false)

      (fee-nonpositive? decision-fee)
      (do (log/warn "Nonpositive decision fee: " processing-fee)
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

(defn- set-payment-type
  "Adds the payment type info to a form"
  [form payment-type]
  (if (contains? form-payment-types payment-type)
    (assoc-in form [:properties :payment-type] payment-type)
    (throw (user-feedback-exception (str "Maksutyyppi ei tuettu: " payment-type)))))

(defn- set-fees
  "Sets fees amount for form"
  [form payment-type processing-fee decision-fee]
  (let [final-processing-fee (cond
                               (= :payment-type-kk payment-type) kk-processing-fee
                               (= :payment-type-tutu payment-type) tutu-processing-fee
                               :else processing-fee)]
    (if (valid-fees? payment-type final-processing-fee decision-fee)
      (-> form
        (assoc-in [:properties :processing-fee] (when final-processing-fee (str final-processing-fee)))
        (assoc-in [:properties :decision-fee] (when decision-fee (str decision-fee))))
      (throw (user-feedback-exception
               (str "Maksutiedot virheelliset: " [payment-type processing-fee decision-fee]))))))

(defn set-payment-info
  "Sets the payment amount for the form. Input and output fees are decimal strings."
  [form payment-type processing-fee decision-fee]
  (let [payment-type-kw    (keyword payment-type)
        coerce-bigdec-fn   (fn [fee]
                             (when fee
                               (try (bigdec fee)
                                    (catch Exception _
                                      (throw (user-feedback-exception (str "Maksuarvo ei numero: " fee)))))))
        processing-fee-num (coerce-bigdec-fn processing-fee)
        decision-fee-num   (coerce-bigdec-fn decision-fee)]
    (-> form
        (set-payment-type payment-type-kw)
        (set-fees payment-type-kw processing-fee-num decision-fee-num))))

; TODO: this should be triggered as part of form updates / whenever a form is attached to haku?
(defn set-payment-info-if-higher-education
  "Set payment type and amount based on if form is
   attached to one or more matching higher education admissions"
  [tarjonta-service form]
  (let [haut (tarjonta/hakus-by-form-key tarjonta-service (:key form))
        application-fee-required? (some true? (map requires-higher-education-application-fee? haut))]
    (if application-fee-required?
      (set-payment-info form :payment-type-kk kk-processing-fee nil)
      form)))

