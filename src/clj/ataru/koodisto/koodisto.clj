(ns ataru.koodisto.koodisto
  (:require [ataru.util :as util]
            [ataru.component-data.value-transformers :refer [update-options-while-keeping-existing-followups]]
            [ataru.cache.cache-service :as cache-service]
            [ataru.middleware.user-feedback :refer [user-feedback-exception]]
            [taoensso.timbre :as log])
  (:import java.time.ZonedDateTime))

(defn encode-koodisto-key [{:keys [uri version]}]
  (str uri "#" version))

(defn- valid-koodi
  [valid-at koodi]
  (when-not (or (and (contains? (:valid koodi) :start)
                     (.isBefore valid-at (:start (:valid koodi))))
                (and (contains? (:valid koodi) :end)
                     (.isAfter valid-at (:end (:valid koodi)))))
    (cond-> koodi
            (contains? koodi :within)
            (update :within (partial keep (partial valid-koodi valid-at))))))

(defn get-koodisto-options
  [koodisto-cache uri version allow-invalid?]
  (cond->> (cache-service/get-from koodisto-cache (encode-koodisto-key {:uri uri :version version}))
           (not allow-invalid?)
           (keep (partial valid-koodi (ZonedDateTime/now)))))

(defn- choice-field-from-koodisto?
  [field]
  (and (:koodisto-source field)
    (or (= "multipleChoice" (:fieldType field))
        (= "singleChoice" (:fieldType field))
        (= "dropdown" (:fieldType field)))))

(defn- attachment-from-kouta?
  [field]
  (and
    (= "attachment" (:fieldType field))
    (-> field :params :fetch-info-from-kouta?)))

(defn- removed-followup-values
  [newest-options existing-options remove-existing]
  (if remove-existing
    (let [new-values (set (map :value newest-options))
          options-to-be-removed (filter  #(not (contains? new-values (:value %))) existing-options)]
      (filter #(not-empty (:followups %)) options-to-be-removed))
    []))

(defn- populate-choice-field-from-koodisto
  [koodisto-cache field remove-existing]
  (let [{:keys [uri version default-option]} (:koodisto-source field)
        empty-option               {:value "" :label {:fi "" :sv "" :en ""}}
        unsorted-koodis            (map (fn [koodi] (select-keys koodi [:value :label]))
                                        (get-koodisto-options koodisto-cache uri version (:allow-invalid? (:koodisto-source field))))
        koodis                     (sort-by (comp :fi :label) unsorted-koodis)
        koodis-with-default-option (cond->> koodis
                                     (some? default-option)
                                     (map (fn [option]
                                            (cond-> option
                                              (= default-option (-> option :label :fi))
                                              (assoc :default-value true)))))
        removed-followup-values     (removed-followup-values koodis-with-default-option
                                                             (:options field)
                                                             remove-existing)
        koodis-with-followups      (update-options-while-keeping-existing-followups koodis-with-default-option
                                                                                    (:options field)
                                                                                    remove-existing)]
    (when (not-empty removed-followup-values)
      (log/warn "Followup questions would be silently removed, field" (:id field) "values" removed-followup-values)
      (throw (user-feedback-exception
              (str "Kysymyksellä " (:id field) " on jatkokysymyksiä jotka poistettaisiin: " removed-followup-values))))
    (assoc field :options (if (= (:fieldType field) "dropdown")
                            (->> koodis-with-followups
                                 (remove (comp (partial = "") :value))
                                 (cons empty-option))
                            koodis-with-followups))))

(def attachment-type-koodisto-uri "liitetyypitamm")
(def attachment-type-koodisto-version 1)

(defn get-attachment-type-label
  [koodisto-cache attachment-type]
  (->> (get-koodisto-options koodisto-cache attachment-type-koodisto-uri attachment-type-koodisto-version true)
    (filter #(= attachment-type (:uri %)))
    first
    :label))

(defn- populate-attachment-field-from-koodisto
  [koodisto-cache field]
  (let [attachment-type (-> field :params :attachment-type)
        label (get-attachment-type-label koodisto-cache attachment-type)]
    (assoc field :label label)))

(defn- populate-form-koodisto-field
  [koodisto-cache remove-existing field]
  (cond
    (choice-field-from-koodisto? field)
    (populate-choice-field-from-koodisto koodisto-cache field remove-existing)

    (attachment-from-kouta? field)
    (populate-attachment-field-from-koodisto koodisto-cache field)

    :else field))

(defn populate-form-koodisto-fields
  ([koodisto-cache form remove-existing]
  (update form :content (partial util/map-form-fields
                                 (partial populate-form-koodisto-field
                                          koodisto-cache
                                          remove-existing))))
  ([koodisto-cache form]
   (populate-form-koodisto-fields koodisto-cache form false)))

(defn get-postal-office-by-postal-code
  [koodisto-cache postal-code]
  (->> (get-koodisto-options koodisto-cache "posti" 1 false)
       (filter #(= postal-code (:value %)))
       (first)))

(defn get-koulutustyypit
  [koodisto-cache]
  (get-koodisto-options koodisto-cache "koulutustyyppi" 2 false))

(defn all-koodisto-values
  [koodisto-cache uri version allow-invalid?]
  (->> (get-koodisto-options koodisto-cache uri version allow-invalid?)
       (map :value)
       (into #{})))
