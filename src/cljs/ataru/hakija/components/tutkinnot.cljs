(ns ataru.hakija.components.tutkinnot)

(defn is-tutkinto-configuration-component? [field-descriptor]
  (= "tutkinto-properties" (:category field-descriptor)))

(defn itse-syotetty-tutkinnot-content [conf-field-descriptor]
  (get-in (some #(when (= "itse-syotetty" (:id %)) %) (:options conf-field-descriptor)) [:followups] []))
