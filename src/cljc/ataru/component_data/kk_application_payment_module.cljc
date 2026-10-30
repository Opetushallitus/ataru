(ns ataru.component-data.kk-application-payment-module
  (:require [ataru.component-data.component :as component]
            [ataru.translations.texts :refer [kk-application-payment-module-texts]]
            [clojure.set :as cs]
            [ataru.constants :refer [system-metadata]]))

(def payment-module-keyword :kk-application-payment-module)
(def payment-module-name (name payment-module-keyword))
(def kk-application-payment-wrapper-key "kk-application-payment-wrapper")
(def kk-application-payment-choice-key "kk-application-payment-option")
(def asiakasnumero-migri-key "asiakasnumero-migri")

(def kk-application-payment-document-options
  {:passport-option-value "0"
   :eu-blue-option-value "1"
   :eu-family-member-option-value "2"
   :continuous-residence-option-value "3"
   :longterm-residence-option-value "4"
   :brexit-option-value "5"
   :permanent-residence-option-value "6"
   :temporary-protection-option-value "7"
   :no-document-option-value "8"})

(def kk-application-payment-document-exempt-options
  (dissoc kk-application-payment-document-options :no-document-option-value))

; A-luvan takapuolta ei enää kysytä, mutta avain säilyy, jotta sen sisältävät hakemukset käsitellään kuten ennenkin
(def kk-application-payment-exempt-attachment-keys
  #{"passport-attachment"
    "eu-blue-card-attachment"
    "eu-family-member-permit"
    "continuous-residence-permit-front"
    "continuous-residence-permit-back"
    "longterm-permit-attachment"
    "brexit-permit-attachment"
    "permanent-residence-permit"
    "temporary-protection-permit"})

(def sync-triggering-kk-application-payment-attachment-keys
  (cs/union kk-application-payment-exempt-attachment-keys
            #{"eu-passport-attachment"
              "eu-family-passport-attachment"
              "continuous-residence-passport-attachment"
              "longterm-passport-attachment"
              "brexit-passport-attachment"
              "permanent-residence-passport-attachment"
              "temporary-protection-passport-attachment"
              "none-passport-attachment"}))

(defn- kk-option-attachment [metadata id label-key info-key]
  (assoc (component/attachment metadata)
    :id id
    :label (label-key kk-application-payment-module-texts)
    :params {
             :info-text
              {:value (info-key kk-application-payment-module-texts)}}))

(defn- permit-attachment [metadata id label-key]
  (kk-option-attachment metadata id label-key :residence-permit-attachment-info))

(defn- passport-attachment [metadata id]
  (kk-option-attachment metadata id :passport-attachment :passport-attachment-info))

(defn- deadline-field [metadata]
  (assoc (component/text-field metadata)
    :label (:attachment-deadline kk-application-payment-module-texts)
    :validators ["required"]
    :params {:size "S"}))

(defn- asiakasnumero-migri [metadata]
  (assoc (component/text-field metadata)
    :id asiakasnumero-migri-key
    :label (:asiakasnumero-migri kk-application-payment-module-texts)
    :validators ["required" "numeric"]
    :params {
             :numeric true
             :decimals nil
             :info-text
              {:label (:asiakasnumero-migri-info kk-application-payment-module-texts)}}))

(defn- passport-option [metadata]
  {:label (:passport-option kk-application-payment-module-texts)
   :value (:passport-option-value kk-application-payment-document-options)
   :followups [(merge (component/info-element metadata)
                      {:text (:passport-info kk-application-payment-module-texts)})
               (passport-attachment metadata "passport-attachment")]})

(defn- eu-blue-card-option [metadata]
  {:label (:eu-blue-card-option kk-application-payment-module-texts)
   :value (:eu-blue-option-value kk-application-payment-document-options)
   :followups [(deadline-field metadata)
               (permit-attachment metadata "eu-blue-card-attachment" :eu-blue-card-attachment)
               (passport-attachment metadata "eu-passport-attachment")]})

(defn- eu-family-member-residence-option [metadata]
  {:label (:eu-family-member-option kk-application-payment-module-texts)
   :value (:eu-family-member-option-value kk-application-payment-document-options)
   :followups [(deadline-field metadata)
               (permit-attachment metadata "eu-family-member-permit" :eu-family-member-attachment)
               (passport-attachment metadata "eu-family-passport-attachment")]})

(defn- continuous-residence-permit-option [metadata]
  {:label (:continuous-residence-option kk-application-payment-module-texts)
   :value (:continuous-residence-option-value kk-application-payment-document-options)
   :followups [(merge (component/info-element metadata)
                 {:text (:continuous-residence-info kk-application-payment-module-texts)})
               (asiakasnumero-migri metadata)
               (permit-attachment metadata "continuous-residence-permit-front" :continuous-permit-front-attachment)
               (passport-attachment metadata "continuous-residence-passport-attachment")]})

(defn- longterm-residence-permit-option [metadata]
  {:label (:longterm-residence-option kk-application-payment-module-texts)
   :value (:longterm-residence-option-value kk-application-payment-document-options)
   :followups [(permit-attachment metadata "longterm-permit-attachment" :longterm-permit-attachment)
               (passport-attachment metadata "longterm-passport-attachment")]})

(defn- brexit-permit-option [metadata]
  {:label (:brexit-option kk-application-payment-module-texts)
   :value (:brexit-option-value kk-application-payment-document-options)
   :followups [(permit-attachment metadata "brexit-permit-attachment" :brexit-permit-attachment)
               (passport-attachment metadata "brexit-passport-attachment")]})

(defn- permanent-residence-permit-option [metadata]
  {:label (:permanent-residence-option kk-application-payment-module-texts)
   :value (:permanent-residence-option-value kk-application-payment-document-options)
   :followups [(permit-attachment metadata "permanent-residence-permit" :permanent-permit-attachment)
               (passport-attachment metadata "permanent-residence-passport-attachment")]})

(defn- temporary-protection-option [metadata]
  {:label (:temporary-protection-option kk-application-payment-module-texts)
   :value (:temporary-protection-option-value kk-application-payment-document-options)
   :followups [(merge (component/info-element metadata)
                      {:text (:protection-info kk-application-payment-module-texts)})
               (permit-attachment metadata "temporary-protection-permit" :temporary-protection-attachment)
               (passport-attachment metadata "temporary-protection-passport-attachment")]})

(defn- none-option [metadata]
  {:label (:no-document-option kk-application-payment-module-texts)
   :value (:no-document-option-value kk-application-payment-document-options)
   :followups [(merge (component/info-element metadata)
                      {:text (:none-passport-info kk-application-payment-module-texts)})
               (passport-attachment metadata "none-passport-attachment")]})

(defn- document-choice [metadata]
  (assoc (component/single-choice-button metadata)
    :id kk-application-payment-choice-key
    :label (:document-option-title kk-application-payment-module-texts)
    :options [(none-option metadata)
              (passport-option metadata)
              (permanent-residence-permit-option metadata)
              (continuous-residence-permit-option metadata)
              (temporary-protection-option metadata)
              (eu-blue-card-option metadata)
              (eu-family-member-residence-option metadata)
              (longterm-residence-permit-option metadata)
              (brexit-permit-option metadata)]
    :validators ["required"]
    :params {
             :info-text
              {:label (:document-option-info kk-application-payment-module-texts)}}))

(defn kk-application-payment-module []
  (assoc (component/form-section system-metadata)
    :id kk-application-payment-wrapper-key
    :module payment-module-keyword
    :label (:section-title kk-application-payment-module-texts)
    :children [(document-choice system-metadata)]))