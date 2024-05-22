(ns ataru.forms.form-payment-info
  (:require [ataru.middleware.user-feedback :refer [user-feedback-exception]]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [ataru.config.core :refer [config]]))

(def form-payment-types
    #{:payment-type-tutu
      :payment-type-kk
      :payment-type-astu
      nil})

(def kk-processing-fee
  (bigdec (get-in config [:form-payment-info :kk-processing-fee])))

(def tutu-processing-fee
  (bigdec (get-in config [:tutkintojen-tunnustaminen :maksut :decision-amount])))

(defn- requires-higher-education-application-fee?
  [tarjonta-service haku]
  (let [hakukohteet (tarjonta/get-hakukohteet tarjonta-service (:hakukohteet haku))]
    (and
      haku
      hakukohteet
      ; Kohdejoukko must be korkeakoulutus
      (str/starts-with? (:kohdejoukko-uri haku) "haunkohdejoukko_12#")
      ; "Kohdejoukon tarkenne must be empty or siirtohaku
      (or (nil? (:kohdejoukon-tarkenne-uri haku))
          (str/starts-with? (:kohdejoukon-tarkenne-uri haku) "haunkohdejoukontarkenne_1#"))
      ; Must be tutkintoon johtava
      (some true? (map #(:tutkintoon-johtava? %) hakukohteet)))))

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
    (assoc-in form [:properties :payment-type] payment-type)
    (throw (user-feedback-exception (str "Maksutyyppi ei tuettu: " payment-type)))))

(defn- add-fees
  "Adds fee amounts for form if valid. Forces hardcoded values for specific payment types."
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

(defn- add-payment-info-to-form
  [form payment-type processing-fee decision-fee]
  (let [payment-type-kw    (keyword payment-type)
        coerce-bigdec-fn   (fn [fee]
                             (when fee
                               (try (bigdec fee)
                                    (catch Exception _
                                      (throw (user-feedback-exception (str "Maksusumma ei numero: " fee)))))))
        processing-fee-num (coerce-bigdec-fn processing-fee)
        decision-fee-num   (coerce-bigdec-fn decision-fee)]
    (-> form
        (add-payment-type payment-type-kw)
        (add-fees payment-type-kw processing-fee-num decision-fee-num))))

(defn- add-payment-info-if-higher-education
  "Set payment info if form is attached to one or more matching higher education admissions."
  [tarjonta-service form haku]
    (if (requires-higher-education-application-fee? tarjonta-service haku)
      (add-payment-info-to-form form :payment-type-kk kk-processing-fee nil)
      form))

(defn set-payment-info
  "Sets the payment amount for the form. Input and output fees are decimal strings in form \"1234.56\"."
  [form payment-type processing-fee decision-fee]
  (if (= :payment-type-kk (keyword payment-type))
    (throw (user-feedback-exception
             (str "Hakemusmaksua ei voi asettaa manuaalisesti: " [payment-type processing-fee decision-fee])))
    (add-payment-info-to-form form payment-type processing-fee decision-fee)))

(defn get-payment-info
  "Gets payment info for form. Should be always used to get payment info rather than querying
   properties directly, because type and fees may be set and overridden dynamically."
  [tarjonta-service form haku]
  (let [form-with-possible-kk-fees (add-payment-info-if-higher-education tarjonta-service form haku)
        properties (:properties form-with-possible-kk-fees)]
    (select-keys properties [:payment-type :processing-fee :decision-fee])))
