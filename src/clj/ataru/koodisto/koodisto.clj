(ns ataru.koodisto.koodisto
  (:require [ataru.util :as util]
            [ataru.koodisto.koodisto-db-cache :as koodisto-cache]
            [ataru.component-data.value-transformers :refer [update-options-while-keeping-existing-followups]]
            [clojure.core.cache :as cache]
            [ataru.cache.cache-service :as cache-service]))

(def populate-form-koodisto-fields-cache (atom (cache/lru-cache-factory {})))

(defn encode-koodisto-key [{:keys [uri version]}]
  (str uri "#" version))

(defn get-koodisto-options
  [koodisto-cache uri version]
  (cache-service/get-from koodisto-cache (encode-koodisto-key {:uri uri :version version})))

(defn- populate-form-koodisto-field
  [koodisto-cache field]
  (if (and (:koodisto-source field)
           (or (= "multipleChoice" (:fieldType field))
               (= "dropdown" (:fieldType field))))
    (let [{:keys [uri version default-option]} (:koodisto-source field)
          empty-option                         {:value "" :label {:fi "" :sv "" :en ""}}
          koodis                               (map (fn [koodi] (select-keys koodi [:value :label]))
                                                    (get-koodisto-options koodisto-cache uri version))
          koodis-with-default-option           (cond->> koodis
                                                        (some? default-option)
                                                        (map (fn [option]
                                                               (cond-> option
                                                                       (= default-option (-> option :label :fi))
                                                                       (assoc :default-value true)))))
          koodis-with-followups                (update-options-while-keeping-existing-followups koodis-with-default-option (:options field))]
      (assoc field :options (if (= (:fieldType field) "dropdown")
                              (->> koodis-with-followups
                                   (remove (comp (partial = "") :value))
                                   (cons empty-option))
                              koodis-with-followups)))
    field))

(defn populate-form-koodisto-fields
  [koodisto-cache form]
  (update form :content (partial util/map-form-fields
                                 (partial populate-form-koodisto-field
                                          koodisto-cache))))

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
