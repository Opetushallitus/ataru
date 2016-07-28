(ns ataru.hakija.validator
  (:require [ataru.forms.form-store :as form-store]
            [ataru.hakija.application-validators :as validator]
            [ataru.util :as util]
            [yesql.core :as sql]))

(defn valid-application
  "Verifies that given application is valid by validating each answer
   against their associated validators."
  [application]
  (when-let [form (form-store/fetch-form (:form application))]
    (let [validators   (reduce (fn [m field]
                                 (assoc m (:id field) (:validators field)))
                               {}
                               (util/flatten-form-fields (:content form)))
          valid-field? (fn [answer]
                         (when-let [validators (get validators (:key answer))]
                           (every? true? (map #(validator/validate % (:value answer)) validators))))
          answers      (:answers application)]
      (every? valid-field? answers))))
