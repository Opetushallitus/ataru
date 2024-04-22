(ns ataru.forms.form-payment-info
  (:require [ataru.middleware.user-feedback :refer [user-feedback-exception]]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]))

; TODO: should these come from eg. koodisto as discussed? And if so, why?
(def form-payment-types
    #{:payment-type-tutu
      :payment-type-kk
      :payment-type-astu
      nil})

; TODO: move to configuration, placeholder amount
(def kk-processing-fee 100)

(defn- requires-higher-education-application-fee? [haku]
  ; TODO: "kohdejoukon tarkenne on joko tyhjä tai "siirtohaku"
  ; TODO: "kyseessä tutkintoon johtava koulutus"
  (clojure.string/starts-with? (:kohdejoukko-uri haku) "haunkohdejoukko_12#"))

(defn- valid-fees?
  [payment-type processing-fee decision-fee]
  (cond
    ; (or (and (some? processing-fee) (< 0 processing-fee))
    ;    (and (some? decision-fee) (< 0 decision-fee))) false
    ; (and (= :payment-type-kk payment-type)
    ; (or (not= processing-fee kk-processing-fee)
    ;    (some? decision-fee))) false
    :else true))

(defn- set-payment-type
  "Adds the payment type info to a form"
  [form payment-type]
  (if (contains? form-payment-types payment-type)
    (assoc-in form [:properties :payment-type] payment-type)
    (throw (user-feedback-exception (str "Maksutyyppi ei tuettu: " payment-type)))))

(defn- set-fees
  "Sets fees amount for form"
  [form payment-type processing-fee decision-fee]
  (let [final-processing-fee (if (= :payment-type-kk payment-type)
                               kk-processing-fee
                               processing-fee)]
    (if (valid-fees? payment-type final-processing-fee decision-fee)
      (-> form
        (assoc-in [:properties :processing-fee] final-processing-fee)
        (assoc-in [:properties :decision-fee] decision-fee))
      (throw (user-feedback-exception
               (str "Maksutiedot virheelliset: " [payment-type processing-fee decision-fee]))))))

(defn set-payment-info
  "Sets the payment amount for the form"
  [form payment-type processing-fee decision-fee]
  (let [payment-type-kw (keyword payment-type)]
    (-> form
        (set-payment-type payment-type-kw)
        (set-fees payment-type-kw processing-fee decision-fee))))

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

