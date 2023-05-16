(ns ataru.component-data.base-education-continuous-admissions-module
  (:require [ataru.translations.texts :refer [base-education-cotinuous-admissions-module-texts]]
            [ataru.component-data.component :as component]
            [ataru.component-data.base-education-module-2nd :refer [base-education-choice-key]]))


(def base-education-continuous-admissions-wrapper-key "pohjakoulutus-continuous-admissions-wrapper")

(defn- base-education-question
  [metadata]
  (assoc (component/single-choice-button metadata)
    :id base-education-choice-key
    :label (:choose-base-education base-education-cotinuous-admissions-module-texts)
    :koodisto-source {
                      :uri "pohjakoulutusjatkuvahaku2023"
                      :version 1
                      :title "jatkuvan haun pohjakoulutus (2023)"
                      :allow-invalid? false}
    :koodisto-ordered-by-user true
    :validators ["required"]
    :params
     {:info-text
      {:label (:choose-base-education-info base-education-cotinuous-admissions-module-texts)}}))

(defn base-education-continuous-admissions-module [metadata]
  (assoc (component/form-section metadata)
         :id base-education-continuous-admissions-wrapper-key
         :label (:section-title base-education-cotinuous-admissions-module-texts)
         :children [(base-education-question metadata)]))

