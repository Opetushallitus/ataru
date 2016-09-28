(ns ataru.koodisto.koodisto
  (:require [oph.soresu.common.koodisto :as koodisto]))

(defn get-koodisto-options
  [uri version]
  (:content
    (koodisto/get-cached-koodi-options :db uri version)))

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
                                                     koodis)]
                    (assoc % :options (if (= (:fieldType %) "dropdown")
                                        (into empty-option koodis-with-default-option)
                                        koodis-with-default-option)))
                  %)
                (:content form))))

(defn get-postal-office-by-postal-code
  [postal-code]
  (->> (get-koodisto-options "posti" 1)
       (filter #(= postal-code (:value %)))
       (first)))

(defn all-koodisto-labels
  [uri version]
  (let [koodisto-values (get-koodisto-options uri version)]
    (set (mapcat #(vals (:label %)) koodisto-values))))

(defn list-all-koodistos
  []
  (koodisto/list-koodistos))