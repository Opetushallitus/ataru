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
    (let [allowed-values (partial reduce
                                  (fn [values option]
                                    (when-not (clojure.string/blank? (:value option))
                                      (concat values (vals (:label option)))))
                           [])
          validators     (reduce (fn [m field]
                                   (assoc m (:id field) (cond-> (select-keys field [:validators])
                                                          (= (:fieldType field) "dropdown") (assoc :allowed-values (allowed-values (:options field))))))
                                 {}
                                 (util/flatten-form-fields (:content form)))
          valid-field?   (fn [answer]
                           (let [validators    (or
                                                 (get validators (:key answer))
                                                 [])
                                 valid-answer? (fn [validator]
                                                 (validator/validate validator (:value answer)))]
                             (and
                               (every? true? (map valid-answer? (:validators validators)))
                               (or
                                 (nil? (:allowed-values validators))
                                 (some #(= (:value answer) %) (:allowed-values validators))))))
          answers        (:answers application)]
      (every? valid-field? answers))))
