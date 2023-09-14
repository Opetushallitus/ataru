(ns ataru.selection-limit.selection-limit-service
  (:require [ataru.forms.form-store :as forms]
            [ataru.db.db :as db]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :refer [defqueries]]
            [ataru.util :as util]
            [taoensso.timbre :as log]
            [clojure.set :as set])
  (:import [java.util UUID]))

(declare yesql-permanent-selection-question-ids-for-application)
(declare yesql-get-selections-query)
(declare yesql-remove-existing-initial-selection!)
(declare yesql-get-selections-for-answer-query)
(declare yesql-remove-existing-selection!)
(declare yesql-has-permanent-selection)
(declare yesql-new-selection!)
(defqueries "sql/selections-queries.sql")

(defn- exec-db
  [ds-key query params]
  (db/exec ds-key query params))

(defn fields-in-selection-group [form]
  (->> form
       :content
       (util/flatten-form-fields)
       (filter #(get-in % [:params :selection-group-id]))))

(defn query-permanent-selections [application-key]
  (exec-db :db yesql-permanent-selection-question-ids-for-application {:application_key application-key}))

(defn query-available-selections
  ([form-key]
   (query-available-selections form-key nil))
  ([form-key selection-id]
   (let [form                (or (forms/fetch-by-key form-key)
                                 (throw (RuntimeException. (format "Form %s doesn't exist! Trying to make selection query." form-key))))
         fields              (fields-in-selection-group form)
         selection-group-ids (->> fields
                                  (map #(get-in % [:params :selection-group-id]))
                                  (distinct))
         result              (exec-db :db yesql-get-selections-query {:selection_group_ids selection-group-ids :selection_id selection-id})
         selections          (util/group-by-first (fn [r] [(:question-id r) (:answer-id r)]) result)
         limits              (into {} (mapcat #(map (fn [o] [[(:id %) (:value o)] [% (:selection-limit o)]]) (:options %)) fields))]
     (merge
      {:limit-reached (mapcat (fn [[key [parent limit]]]
                                  (when (and limit
                                             (get-in parent [:params :selection-group-id])
                                             (<= limit (get-in selections [key :n] 0)))
                                        [(zipmap [:question-id :answer-id] key)])) limits)}
      (when selection-id
            {:selection-id selection-id})))))

(defn remove-initial-selection [form-key selection-id question-id selection-group-id]
  (exec-db :db yesql-remove-existing-initial-selection! {:selection_id       selection-id
                                                         :selection_group_id selection-group-id
                                                         :question_id        question-id})
  (query-available-selections form-key selection-id))

(defn limit-exception [message]
  (ex-info message {:cause :limit-reached}))

(defn enforce-limits [limit application-key selection-id selection-group-id question-id answer-id connection]
  (let [n (-> (yesql-get-selections-for-answer-query {:selection_group_id selection-group-id
                                                      :question_id        question-id
                                                      :answer_id          answer-id} connection)
              first
              :n)]
    (when (and limit (not (<= n limit)))
      (if application-key
        (throw (limit-exception
                 (format
                   (str "Unable to save application-key = %s because selection limit reached in"
                     "selection-group-id = %s, question-id = %s, answer-id = %s with limit = %s and n = %s!")
                   application-key
                   selection-group-id
                   question-id
                   answer-id
                   limit
                   n)))
        (throw (limit-exception
                 (format
                   (str "Unable to select with selection-id = %s because limit reached in"
                     "selection-group-id = %s, question-id = %s, answer-id = %s with limit %s and n = %s!")
                   selection-id
                   selection-group-id
                   question-id
                   answer-id
                   limit
                   n)))))))

(defn permanent-select-on-store-application
  ([application-key application selection-id form]
   (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
     (permanent-select-on-store-application application-key application selection-id form {:connection conn})))
  ([application-key {:keys [answers]} selection-id form connection]
   (let [selection-group-fields (group-by :id (fields-in-selection-group form))
         try-to-select          (->> answers
                                     (filter #(selection-group-fields (:key %)))
                                     (seq))
         existing-selections (set (map :question_id (yesql-permanent-selection-question-ids-for-application {:application_key application-key} connection)))
         relevant-question-ids-from-application (set (map :id try-to-select))
         existing-not-in-answers (set/difference existing-selections relevant-question-ids-from-application)]
     (when (not-empty existing-not-in-answers)
         (log/info "Cleaning up existing selections for question id:s" existing-not-in-answers "for application" application-key)
         (doseq [question-id existing-not-in-answers]
           (yesql-remove-existing-selection! {:application_key    application-key
                                              :question_id        question-id} connection)))
     (doseq [{:keys [key value]} try-to-select]
       (let [{:keys [params options]} (first (selection-group-fields key))
             limit              (->> options
                                     (filter #(= (:value %) value))
                                     first
                                     :selection-limit)
             selection-group-id (get params :selection-group-id)
             question-id        key
             answer-id          value]
         (yesql-remove-existing-initial-selection! {:selection_id       selection-id
                                                    :selection_group_id selection-group-id
                                                    :question_id        question-id} connection)
         (when-not (= 1 (-> (yesql-has-permanent-selection {:application_key    application-key
                                                            :question_id        question-id
                                                            :answer_id          answer-id
                                                            :selection_group_id selection-group-id} connection)
                            first
                            :n))
           (yesql-remove-existing-selection! {:application_key    application-key
                                              :question_id        question-id} connection)
           (yesql-new-selection! {:application_key    application-key
                                  :question_id        question-id
                                  :answer_id          answer-id
                                  :selection_group_id selection-group-id} connection)
           (enforce-limits limit application-key nil selection-group-id question-id answer-id connection)))))))

(defn swap-selection [form-key selection-id question-id answer-id selection-group-id]
  (let [{:keys [options]} (->> (fields-in-selection-group (forms/fetch-by-key form-key))
                               (filter #(= (:id %) question-id))
                               first)
        limit (->> options
                   (filter #(= (:value %) answer-id))
                   first
                   :selection-limit)]
    (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
      (let [conn {:connection conn}]
        (yesql-new-initial-selection! {:selection_id       selection-id
                                       :question_id        question-id
                                       :answer_id          answer-id
                                       :selection_group_id selection-group-id} conn)
        (enforce-limits limit nil selection-id selection-group-id question-id answer-id conn)))
    (query-available-selections form-key selection-id)))

(defn new-selection [form-key question-id answer-id selection-group-id]
  (swap-selection form-key (str (UUID/randomUUID)) question-id answer-id selection-group-id))
