(ns ataru.hakija.application-handlers
  (:require [re-frame.core :refer [reg-event-db reg-fx reg-event-fx dispatch inject-cofx]]
            [ataru.hakija.application-validators :as validator]
            [ataru.cljs-util :as util]
            [ataru.hakija.hakija-ajax :as ajax]
            [ataru.hakija.rules :as rules]
            [cljs.core.match :refer-macros [match]]
            [ataru.hakija.application :refer [create-initial-answers
                                              create-application-to-submit
                                              extract-wrapper-sections]]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn initialize-db [_ _]
  {:form        nil
   :application {:answers {}}})

(reg-event-fx
  :application/get-latest-form-by-key
  (fn [{:keys [db]} [_ form-key]]
    {:db   db
     :http {:method  :get
            :url     (str "/hakemus/api/form/" form-key)
            :handler :application/handle-form}}))

(defn- get-latest-form-by-hakukohde [{:keys [db]} [_ hakukohde-oid]]
  {:db   db
   :http {:method  :get
          :url     (str "/hakemus/api/hakukohde/" hakukohde-oid)
          :handler :application/handle-form}})

(reg-event-fx
  :application/get-latest-form-by-hakukohde
  get-latest-form-by-hakukohde)

(defn handle-submit [db _]
  (assoc-in db [:application :submit-status] :submitted))

(reg-event-db
  :application/handle-submit-response
  handle-submit)

(reg-event-fx
  :application/handle-submit-error
  (fn [cofx [_ response]]
    {:db (-> (update (:db cofx) :application dissoc :submit-status)
             (assoc :error {:message "Tapahtui virhe " :detail response}))
     :dispatch-later [{:ms 30000 :dispatch [:application/clear-error]}]}))

(reg-event-db
  :application/clear-error
  (fn [db _]
    (dissoc db :error)))

(reg-event-fx
  :application/submit-form
  (fn [{:keys [db]} _]
    {:db       (assoc-in db [:application :submit-status] :submitting)
     :http     {:method        :post
                :url           "/hakemus/api/application"
                :post-data     (create-application-to-submit (:application db) (:form db) (get-in db [:form :selected-language]))
                :handler       :application/handle-submit-response
                :error-handler :application/handle-submit-error}
     :dispatch  [:application/clear-error]}))

(def ^:private lang-pattern #"/(\w{2})$")

(defn- get-lang-from-path [supported-langs]
  ((set supported-langs)
   (some->> (util/get-path)
     (re-find lang-pattern)
     second
     keyword)))

(defn- set-form-language [form & [lang]]
  (let [supported-langs (:languages form)
        lang            (or lang
                          (get-lang-from-path supported-langs)
                          (first supported-langs))]
    (assoc form :selected-language lang)))

(defn- languages->kwd [form]
  (update form :languages
    (fn [languages]
      (mapv keyword languages))))

(defn- handle-get-application [db [_ application]]
  (->> (:answers application)
       (reduce (fn [result {:keys [key value]}]
                 (assoc result (keyword key) value))
               {})
       (reduce-kv (fn [db answer-key value]
                    (update-in db [:application :answers answer-key]
                      merge {:value value :valid true}))
                  db)))

(reg-event-db :application/handle-get-application
  handle-get-application)

(defn handle-form [{:keys [db query-params]} [_ form]]
  (let [form               (-> (languages->kwd form)
                               (set-form-language))
        db                 (-> db
                               (assoc :form form)
                               (assoc :application {:answers (create-initial-answers form)})
                               (assoc :wrapper-sections (extract-wrapper-sections form)))
        application-secret (:modify query-params)]
    (cond-> {:db db}
      (some? application-secret)
      (assoc :http {:method  :get
                    :url     (str "/hakemus/api/application?secret=" application-secret)
                    :handler :application/handle-get-application}))))

(reg-event-db
  :flasher
  (fn [db [_ flash]]
    (assoc db :flasher flash)))

(reg-event-fx
  :application/handle-form
  [(inject-cofx :query-params)]
  handle-form)

(reg-event-db
  :application/initialize-db
  initialize-db)

(defn set-application-field [db [_ key values]]
  (let [path                [:application :answers key]
        current-answer-data (get-in db path)]
    (assoc-in db path (merge current-answer-data values))))

(reg-event-db
  :application/set-application-field
  set-application-field)

(reg-event-db
  :application/set-repeatable-application-field
  (fn [db [_ field-descriptor key idx {:keys [value valid] :as values}]]
    (let [path                      [:application :answers key :values]
          required?                 (some? ((set (:validators field-descriptor)) "required"))
          with-answer               (if (and
                                          (zero? idx)
                                          (empty? value)
                                          (= 1 (count (get-in db path))))
                                      (assoc-in db path [])
                                      (update-in db path (fnil assoc []) idx values))
          all-values                (get-in with-answer path)
          validity-for-validation   (boolean
                                      (some->>
                                        (map :valid (or
                                                      (when (= 1 (count all-values))
                                                        [values])
                                                      (when (and (not required?) (empty? all-values))
                                                        [{:valid true}])
                                                      (butlast all-values)))
                                        not-empty
                                        (every? true?)))
          value-for-readonly-fields-and-db (filter not-empty (mapv :value all-values))]
      (update-in
        with-answer
        (butlast path)
        assoc
        :valid validity-for-validation
        :value value-for-readonly-fields-and-db))))

(reg-event-db
  :application/remove-repeatable-application-field-value
  (fn [db [_ key idx]]
    (update-in db [:application :answers key :values]
      (fn [values]
        (vec
          (filter identity (map-indexed #(if (not= %1 idx) %2) values)))))))

(defn default-error-handler [db [_ response]]
  (assoc db :error {:message "Tapahtui virhe " :detail (str response)}))

(defn application-run-rule [db rule]
  (if (not-empty rule)
    (rules/run-rule rule db)
    (rules/run-all-rules db)))

(reg-event-db
  :application/run-rule
  (fn [db [_ rule]]
    (if (#{:submitting :submitted} (-> db :application :submit-status))
      db
      (application-run-rule db rule))))

(reg-event-db
  :application/default-handle-error
  default-error-handler)

(reg-event-db
 :application/default-http-ok-handler
 (fn [db _] db))

(reg-event-db
  :state-update
  (fn [db [_ f]]
    (or (f db)
        db)))

(reg-event-db
  :application/handle-postal-code-input
  (fn [db [_ postal-office-name]]
    (-> db
        (update-in [:application :ui :postal-office] assoc :disabled? true)
        (update-in [:application :answers :postal-office] merge {:value (:fi postal-office-name) :valid true}))))

(reg-event-db
  :application/handle-postal-code-error
  (fn [db _]
    (-> db
        (update-in [:application :answers :postal-office] merge {:value "" :valid false}))))

(reg-event-db
  :application/toggle-multiple-choice-option
  (fn [db [_ multiple-choice-id idx option-value validators]]
    (let [db    (-> db
                    (assoc-in [:application :answers multiple-choice-id :options idx :value] option-value)
                    (update-in [:application :answers multiple-choice-id :options idx :selected] not))
          value (->> (get-in db [:application :answers multiple-choice-id :options])
                     (vals)
                     (filter :selected)
                     (map :value)
                     (clojure.string/join ", "))
          valid (if (not-empty validators)
                  (every? true? (map #(validator/validate % value) validators))
                  true)]
      (-> db
          (assoc-in [:application :answers multiple-choice-id :value] value)
          (assoc-in [:application :answers multiple-choice-id :valid] valid)))))
