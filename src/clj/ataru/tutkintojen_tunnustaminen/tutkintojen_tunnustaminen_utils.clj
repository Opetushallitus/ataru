(ns ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-utils
  (:require [ataru.hakija.hakija-form-service :as hakija-form-service]
            [ataru.config.core :refer [config]]
            [clojure.string :as string]))

(def apply-reason-tutkinnon-tason-rinnastaminen   "0")
(def apply-reason-kelpoisuus-ammattiin            "1")
(def apply-reason-tutkinto-suoritus-rinnastaminen "2")
(def apply-reason-riittavat-opinnot               "3")
(def apply-reason-lopullinen-paatos               "5")

(defn get-configuration
  []
  (let [cfg (:tutkintojen-tunnustaminen config)]
    (when (string/blank? (:form-key cfg))
      (throw (new RuntimeException
                  "Tutkintojen tunnustaminen form key not set")))
    (when (string/blank? (:country-question-id cfg))
      (throw (new RuntimeException
                  "Tutkintojen tunnustaminen country question id not set")))
    (when (not (integer? (:attachment-total-size-limit cfg)))
      (throw (new RuntimeException
                  "Tutkintojen tunnustaminen attachment size limit not set")))
    cfg))

(defn get-form
  [form-by-id-cache koodisto-cache attachment-deadline-service application]
  (let [form (hakija-form-service/fetch-form-by-id
               (:form-id application)
               [:hakija]
               form-by-id-cache
               koodisto-cache
               nil
               false
               {}
               attachment-deadline-service
               nil
               nil)]
    (when (nil? form)
      (throw (new RuntimeException (str "Form " (:form-id application)
                                        " not found"))))
    form))

(defn- form-key-matches?
  [cfg-form-key test]
  (let [keys (string/split cfg-form-key #",")]
    (some #(= test %) keys)))

(defn tutu-form? [form]
  (let [cfg-form-key (:form-key (get-configuration))]
    (or (= "payment-type-tutu" (get-in form [:properties :payment :type]))
        (form-key-matches? cfg-form-key (:key form)))))
