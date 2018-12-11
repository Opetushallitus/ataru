(ns ataru.koodisto.koodisto
  (:require [ataru.koodisto.koodisto-db-cache :as koodisto-cache]
            [ataru.component-data.value-transformers :refer [update-options-while-keeping-existing-followups]]
            [clojure.core.cache :as cache]
            [ataru.cache.cache-service :as cache-service]))

(def populate-form-koodisto-fields-cache (atom (cache/lru-cache-factory {})))

(defn get-koodisto-options
  [koodisto-cache uri version]
  (let [koodisto-uri (str uri "#" version)]
    (cache-service/get-from koodisto-cache koodisto-uri)))

(defn populate-form-koodisto-fields
  [koodisto-cache form]
  (assoc form :content
              (clojure.walk/prewalk
                #(if (and (:koodisto-source %)
                          (= (:fieldClass %) "formField")
                          (some (fn [type] (= type (:fieldType %))) ["dropdown" "multipleChoice"]))
                  (let [{:keys [uri version default-option]} (:koodisto-source %)
                        empty-option               [{:value "" :label {:fi "" :sv "" :en ""}}]
                        koodis                     (map (fn [koodi] (select-keys koodi [:value :label]))
                                                        (get-koodisto-options koodisto-cache uri version))
                        koodis-with-default-option (cond->> koodis
                                                            (some? default-option)
                                                            (map (fn [option]
                                                                   (cond-> option
                                                                           (= default-option (-> option :label :fi))
                                                                           (assoc :default-value true)))))
                        koodis-with-followups (update-options-while-keeping-existing-followups koodis-with-default-option (:options %))]
                    (assoc % :options (if (= (:fieldType %) "dropdown")
                                        (->> koodis-with-followups
                                             (remove (comp (partial = "") :value))
                                             (into empty-option))
                                        koodis-with-followups)))
                  %)
                (:content form))))

(defn populate-form-koodisto-fields-cached
  [koodisto-cache form]
  (let [cached-data (swap! populate-form-koodisto-fields-cache cache/through-cache form
                      (partial populate-form-koodisto-fields koodisto-cache))]
    (get cached-data form)))

(defn get-postal-office-by-postal-code
  [koodisto-cache postal-code]
  (->> (get-koodisto-options koodisto-cache "posti" 1)
       (filter #(= postal-code (:value %)))
       (first)))

(defn all-koodisto-values
  [koodisto-cache uri version]
  (->> (get-koodisto-options koodisto-cache uri version)
       (map :value)
       (into #{})))

(defn list-all-koodistos
  []
  (koodisto-cache/list-koodistos))
