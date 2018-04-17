(ns ataru.koodisto.koodisto
  (:require [ataru.koodisto.koodisto-db-cache :as koodisto-cache]
            [ataru.component-data.value-transformers :refer [update-options-while-keeping-existing-followups]]))

(defn get-koodisto-options
  [uri version]
  (:content
    (koodisto-cache/get-cached-koodi-options :db uri version)))

(defn populate-form-koodisto-fields
  [form]
  (assoc form :content
              (clojure.walk/prewalk
                #(if (and (:koodisto-source %)
                          (= (:fieldClass %) "formField")
                          (some (fn [type] (= type (:fieldType %))) ["dropdown" "multipleChoice"]))
                  (let [{:keys [uri version default-option]} (:koodisto-source %)
                        empty-option               [{:value "" :label {:fi "" :sv "" :en ""}}]
                        koodis                     (get-koodisto-options uri version)
                        koodis-with-default-option (if default-option
                                                     (map (fn [option] (if (=
                                                                             default-option
                                                                             (-> option :label :fi))
                                                                         (merge option {:default-value true})
                                                                         option))
                                                          koodis)
                                                     koodis)
                        koodis-with-followups (update-options-while-keeping-existing-followups koodis-with-default-option (:options %))]
                    (assoc % :options (if (= (:fieldType %) "dropdown")
                                        (into empty-option koodis-with-followups)
                                        koodis-with-followups)))
                  %)
                (:content form))))

(defn get-postal-office-by-postal-code
  [postal-code]
  (->> (get-koodisto-options "posti" 1)
       (filter #(= postal-code (:value %)))
       (first)))

(defn all-koodisto-values
  [uri version]
  (->> (get-koodisto-options uri version)
       (map :value)
       (into #{})))

(defn list-all-koodistos
  []
  (koodisto-cache/list-koodistos))