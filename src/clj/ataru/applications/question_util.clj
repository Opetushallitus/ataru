(ns ataru.applications.question-util
  (:require [ataru.translations.texts :refer [base-education-2nd-module-texts]]
            [clojure.walk :refer [keywordize-keys]]
            [ataru.component-data.base-education-module-2nd :refer [base-education-choice-key base-education-wrapper-key]]))

(def sora-question-wrapper-label {:fi "Terveydelliset tekijät " :sv "Hälsoskäl"})

(defn get-hakurekisteri-toinenaste-specific-questions
  [form]
  (let [content (:content (keywordize-keys form))
        base-education-options-followups (->> content
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
                                (map #(keyword (:id %))))
        sora-wrapper-children (->> content
                                   (filter #(= sora-question-wrapper-label (:label %)))
                                   first
                                   :children)
        sora-terveys-question (get-in sora-wrapper-children [0 :id])
        sora-aiempi-question (get-in sora-wrapper-children [1 :id])]
    (prn sora-terveys-question)
    (prn sora-aiempi-question)
    {:tutkintovuosi-keys tutkintovuosi-keys
     :tutkintokieli-keys tutkintokieli-keys
     :sora-terveys-key sora-terveys-question
     :sora-aiempi-question sora-aiempi-question}))