(ns ataru.hakija.application-tutkinto-handlers
  (:require [ataru.util :as util]
            [re-frame.core :refer [reg-event-db reg-event-fx]]
            [ataru.tutkinto.tutkinto-util :as tutkinto-util]
            [ataru.hakija.application :refer [create-initial-answers]]
            [ataru.hakija.application-handlers :refer [check-schema-interceptor set-empty-value-dispatch
                                                       set-repeatable-field-values set-repeatable-field-value]]
            [ataru.hakija.application.field-visibility :as field-visibility]))

(reg-event-db
  :application/set-itse-syotetyt-visibility
  [check-schema-interceptor]
  (fn set-itse-syotetyt-visibility [db [_ show?]]
    (let [itse-syotetty-content (tutkinto-util/find-itse-syotetty-tutkinto-content (:form db))
          do-show? (boolean
                     (or show? (util/any-answered? (get-in db [:application :answers]) itse-syotetty-content)))]
      (reduce (fn [db field] (field-visibility/set-field-visibility db field do-show?))
              (assoc-in db [:application :ui :show-itse-syotetyt-tutkinnot?] do-show?)
              itse-syotetty-content))))

(reg-event-fx
  :application/handle-fetch-tutkinnot
  [check-schema-interceptor]
  (fn [{db :db} [_ {tutkinnot-response-body :body}]]
    {:db (-> db
                     (assoc-in [:application :koski-tutkinnot]
                               (tutkinto-util/sort-koski-tutkinnot tutkinnot-response-body))
                     (assoc-in [:oppija-session :tutkinto-fetch-handled] true))
     :dispatch [:application/set-itse-syotetyt-visibility (not (seq tutkinnot-response-body))]}))

(reg-event-fx
  :application/fetch-tutkinnot
  [check-schema-interceptor]
  (fn [_ [_ requested-koski-levels]]
    {:http {:method         :get
            :url            (str "/hakemus/api/omat-tutkinnot?tutkinto-levels=" requested-koski-levels)
            :handler        [:application/handle-fetch-tutkinnot]}}))

(reg-event-db
  :application/add-tutkinto-selection
  [check-schema-interceptor]
  (fn add-tutkinto-selection [db [_ field-descriptor question-group-idx selection-field-id tutkinto-id]]
    (-> db
        (set-repeatable-field-values selection-field-id question-group-idx nil tutkinto-id)
        (set-repeatable-field-value selection-field-id)
        (field-visibility/set-field-visibility field-descriptor))))

(reg-event-fx
  :application/add-tutkinto-row
  [check-schema-interceptor]
  (fn add-tutkinto-row [{db :db} [_ field-descriptor id-field-descriptor tutkinto-id]]
    (let [id                              (keyword (:id field-descriptor))
          id-field-id                     (keyword (:id id-field-descriptor))
          repeat-count                    (get-in db [:application :ui id :count] 1)
          new-question-group-row-needed   (util/answered-in-group-idx
                                            (get-in db [:application :answers id-field-id])
                                            (dec repeat-count))]
      (if new-question-group-row-needed
        {:db         (assoc-in db [:application :ui id :count] (inc repeat-count))
         :dispatch-n (concat (mapcat (partial set-empty-value-dispatch repeat-count)
                                     (:children field-descriptor))
                             [[:application/add-tutkinto-selection field-descriptor repeat-count id-field-id tutkinto-id]])}
        {:db         (assoc-in db [:application :ui id :count] repeat-count)
         :dispatch   [:application/add-tutkinto-selection field-descriptor (dec repeat-count) id-field-id tutkinto-id]}))))

(reg-event-fx
  :application/remove-tutkinto-row
  [check-schema-interceptor]
  (fn remove-tutkinto-row [{db :db} [_ field-descriptor answer-idx]]
    (let [id (keyword (:id field-descriptor))]
      (if (= 1 (get-in db [:application :ui id :count] 1))
        {:dispatch [:application/reset-tutkinto-answers [field-descriptor] false]}
        {:dispatch [:application/remove-question-group-row field-descriptor answer-idx]}))))

(reg-event-fx
  :application/clear-and-hide-itse-syotetyt
  [check-schema-interceptor]
  (fn clear-and-hide-itse-syotetyt [{db :db}]
    (let [itse-syotetty-content (tutkinto-util/find-itse-syotetty-tutkinto-content (:form db))]
      {:db (assoc-in db [:application :ui :show-itse-syotetyt-tutkinnot?] false)
       :dispatch [:application/reset-tutkinto-answers itse-syotetty-content true]})))

(reg-event-db
  :application/reset-tutkinto-answers
  [check-schema-interceptor]
  (fn [db [_ fields hide?]]
    (let [fields-and-descendants    (util/flatten-form-fields fields)
          question-groups           (map :id (filter #(= "questionGroup" (:fieldClass %)) fields-and-descendants))
          initial-answers-of-fields (create-initial-answers fields-and-descendants nil nil)
          merged-answers            (merge (get-in db [:application :answers]) initial-answers-of-fields)
          current-ui-values         (get-in db [:application :ui])
          merged-ui-values          (merge current-ui-values
                                           (into {}
                                                 (map
                                                   (fn [id] [(keyword id)
                                                             (merge ((keyword id) current-ui-values) {:count 1})])
                                                   question-groups)))]
      (reduce (fn [db field] (field-visibility/set-field-visibility db field (not hide?)))
              (-> db
                  (assoc-in [:application :answers] merged-answers)
                  (assoc-in [:application :ui] merged-ui-values))
              fields))))
