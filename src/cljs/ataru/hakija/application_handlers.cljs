(ns ataru.hakija.application-handlers
  (:require [re-frame.core :refer [reg-event-db reg-fx reg-event-fx dispatch]]
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

(defn- handle-get-application [{:keys [db]} [_ secret {:keys [answers form-key lang]}]]
  {:db       (-> db
                 (assoc-in [:application :secret] secret)
                 (assoc-in [:form :selected-language] (keyword lang)))
   :dispatch [:application/get-latest-form-by-key form-key answers]})

(reg-event-fx
  :application/handle-get-application
  handle-get-application)

(defn- get-application-by-secret
  [{:keys [db]} [_ secret]]
  {:db   db
   :http {:method  :get
          :url     (str "/hakemus/api/application?secret=" secret)
          :handler [:application/handle-get-application secret]}})

(reg-event-fx
  :application/get-application-by-secret
  get-application-by-secret)

(reg-event-fx
  :application/get-latest-form-by-key
  (fn [{:keys [db]} [_ form-key answers]]
    {:db   db
     :http {:method  :get
            :url     (str "/hakemus/api/form/" form-key)
            :handler [:application/handle-form answers]}}))

(defn- get-latest-form-by-hakukohde [{:keys [db]} [_ hakukohde-oid]]
  {:db   db
   :http {:method  :get
          :url     (str "/hakemus/api/hakukohde/" hakukohde-oid)
          :handler [:application/handle-form nil]}})

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

(defn- get-lang-from-path [supported-langs]
  (when-let [lang (-> (util/extract-query-params)
                      :lang
                      keyword)]
    (when (some (partial = lang) supported-langs)
      lang)))

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

(defn- toggle-multiple-choice-option [answer option-value validators]
  (let [answer (update-in answer [:options option-value] not)
        value  (->> (:options answer)
                    (filter (comp true? second))
                    (map first)
                    (clojure.string/join ", "))
        valid  (if (not-empty validators)
                 (every? true? (map #(validator/validate % value) validators))
                 true)]
    (merge answer {:value value :valid valid})))

(defn- merge-multiple-choice-option-values [value answer]
  (let [options (clojure.string/split value #"\s*,\s*")]
    (reduce (fn [answer option-value]
              (toggle-multiple-choice-option answer option-value nil))
            answer
            options)))

(defn- set-ssn-field-visibility [db]
  (rules/run-rule {:toggle-ssn-based-fields-for-existing-application "ssn"} db))

(defn- merge-submitted-answers [db [_ submitted-answers]]
  (-> db
      (update-in [:application :answers]
        (fn [answers]
          (reduce (fn [answers {:keys [key value] :as answer}]
                    (let [answer-key (keyword key)
                          value      (cond-> value
                                       (vector? value)
                                       (first))]
                      (if (contains? answers answer-key)
                        (match answer
                          {:fieldType "multipleChoice"}
                          (update answers answer-key (partial merge-multiple-choice-option-values value))

                          {:fieldType "dropdown"}
                          (update answers answer-key merge {:valid true :value value})

                          {:fieldType "textField" :value (_ :guard vector?)}
                          (update answers answer-key merge
                            {:valid true :values (map (fn [value]
                                                        {:valid true :value value})
                                                      (:value answer))})

                          :else
                          (update answers answer-key merge {:valid true :value value}))
                        answers)))
                  answers
                  submitted-answers)))
      (set-ssn-field-visibility)))

(reg-event-db
  :application/merge-submitted-answers
  merge-submitted-answers)

(defn- p [x]
  (println (str x))
  x)

(defn handle-form [{:keys [db]} [_ answers form]]
  (let [form (-> (languages->kwd form)
                 (set-form-language))
        db   (-> db
                 (update :form (fn [{:keys [selected-language]}]
                                 (cond-> form
                                   (some? selected-language)
                                   (assoc :selected-language selected-language))))
                 (assoc-in [:application :answers] (create-initial-answers form))
                 (assoc :wrapper-sections (extract-wrapper-sections form)))]
    {:db               db
     ;; Previously submitted answers must currently be merged to the app db
     ;; after a delay or rules will ruin them and the application will not
     ;; look completely as valid (eg. SSN field will be blank)
     :dispatch-later [{:ms 200 :dispatch [:application/merge-submitted-answers answers]}]}))

(reg-event-db
  :flasher
  (fn [db [_ flash]]
    (assoc db :flasher flash)))

(reg-event-fx
  :application/handle-form
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
  (fn [db [_ multiple-choice-id option-value validators]]
    (update-in db [:application :answers multiple-choice-id]
      (fn [answer]
        (toggle-multiple-choice-option answer option-value validators)))))
