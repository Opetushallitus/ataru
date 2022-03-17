(ns ataru.applications.question-util
  (:require [ataru.translations.texts :refer [base-education-2nd-module-texts]]
            [clojure.walk :refer [keywordize-keys]]
            [ataru.component-data.base-education-module-2nd :refer [base-education-choice-key base-education-wrapper-key]]))

(defn get-hakurekisteri-toinenaste-specific-questions
  [form]
  (let [form (keywordize-keys form)
        base-education-options-followups (->> (:content form)
                                    (filter #(= base-education-wrapper-key (:id %)))
                                    first
                                    :children
                                    (filter #(= base-education-choice-key (:id %)))
                                    first
                                    :options
                                    (mapcat :followups))
        tutkintovuosi-keys (->> base-education-options-followups
                                (filter #(= (:year-of-graduation-question base-education-2nd-module-texts) (:label %)))
                                (map #(keyword (:id %))))
        tutkintokieli-keys (->> base-education-options-followups
                                (filter #(= (:study-language base-education-2nd-module-texts) (:label %)))
                                (map #(keyword (:id %))))]
    {:tutkintovuosi-keys tutkintovuosi-keys
     :tutkintokieli-keys tutkintokieli-keys}))