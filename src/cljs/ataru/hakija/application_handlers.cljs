(ns ataru.hakija.application-handlers
  (:require [re-frame.core :refer [reg-event-db reg-fx reg-event-fx dispatch]]
            [ataru.hakija.application-validators :as validator]
            [ataru.cljs-util :as util]
            [ataru.util :as autil]
            [ataru.hakija.rules :as rules]
            [cljs.core.match :refer-macros [match]]
            [ataru.hakija.application :refer [create-initial-answers
                                              create-application-to-submit
                                              extract-wrapper-sections]]
            [taoensso.timbre :refer-macros [spy debug]]
            [clojure.data :as d]
            [ataru.component-data.value-transformers :as value-transformers]))

(defn initialize-db [_ _]
  {:form        nil
   :application {:answers {}}})

(defn- required? [field-descriptor]
  (some (partial = "required")
        (:validators field-descriptor)))

(defn- handle-get-application [{:keys [db]}
                               [_
                                {:keys [secret virkailija-secret]}
                                {:keys [answers
                                        person
                                        form-key
                                        lang
                                        haku
                                        hakukohde
                                        hakukohde-name
                                        state]}]]
  (let [[secret-kwd secret-val] (if-not (clojure.string/blank? secret)
                                  [:secret secret]
                                  [:virkailija-secret virkailija-secret])]
    {:db       (-> db
                   (assoc-in [:application :editing?] true)
                   (assoc-in [:application secret-kwd] secret-val)
                   (assoc-in [:application :state] state)
                   (assoc-in [:application :hakukohde] hakukohde)
                   (assoc-in [:application :person] person)
                   (assoc-in [:form :selected-language] (or (keyword lang) :fi))
                   (assoc-in [:form :hakukohde-name] hakukohde-name))
     :dispatch (if haku
                 [:application/get-latest-form-by-haku
                  haku
                  answers
                  (when (= :virkailija-secret secret-kwd) secret-val)]
                 [:application/get-latest-form-by-key
                  form-key
                  answers
                  (when (= :virkailija-secret secret-kwd) secret-val)])}))

(reg-event-fx
  :application/handle-get-application
  handle-get-application)

(defn- get-application-by-hakija-secret
  [{:keys [db]} [_ secret]]
  {:db   db
   :http {:method  :get
          :url     (str "/hakemus/api/application?secret=" secret)
          :handler [:application/handle-get-application {:secret secret}]}})

(reg-event-fx
  :application/get-application-by-hakija-secret
  get-application-by-hakija-secret)

(defn- get-application-by-virkailija-secret
  [{:keys [db]} [_ virkailija-secret]]
  {:db   db
   :http {:method  :get
          :url     (str "/hakemus/api/application?virkailija-secret=" virkailija-secret)
          :handler [:application/handle-get-application {:virkailija-secret virkailija-secret}]}})

(reg-event-fx
  :application/get-application-by-virkailija-secret
  get-application-by-virkailija-secret)

(reg-event-fx
  :application/get-latest-form-by-key
  (fn [{:keys [db]} [_ form-key answers virkailija-secret]]
    {:db   db
     :http {:method  :get
            :url     (str "/hakemus/api/form/"
                          form-key
                          (when virkailija-secret
                            (str "?virkailija-secret=" virkailija-secret)))
            :handler [:application/handle-form answers]}}))

(defn- get-latest-form-by-hakukohde [{:keys [db]} [_ hakukohde-oid answers virkailija-secret]]
  {:db   (assoc-in db [:application :preselected-hakukohde] hakukohde-oid)
   :http {:method  :get
          :url     (str "/hakemus/api/hakukohde/"
                        hakukohde-oid
                        (when virkailija-secret
                          (str "?virkailija-secret=" virkailija-secret)))
          :handler [:application/handle-form answers]}})

(reg-event-fx
  :application/get-latest-form-by-hakukohde
  get-latest-form-by-hakukohde)

(reg-event-fx
  :application/get-latest-form-by-haku
  (fn [{:keys [db]} [_ haku-oid answers virkailija-secret]]
    {:db db
     :http {:method  :get
            :url     (str "/hakemus/api/haku/"
                          haku-oid
                          (when virkailija-secret
                            (str "?virkailija-secret=" virkailija-secret)))
            :handler [:application/handle-form answers]}}))

(defn handle-submit [db _]
  (assoc-in db [:application :submit-status] :submitted))

(defn send-application [db method]
  (when-not (-> db :application :submit-status)
    {:db   (-> db (assoc-in [:application :submit-status] :submitting) (dissoc :error))
     :http {:method        method
            :url           "/hakemus/api/application"
            :post-data     (create-application-to-submit (:application db) (:form db) (get-in db [:form :selected-language]))
            :handler       :application/handle-submit-response
            :error-handler :application/handle-submit-error}}))

(reg-event-db
  :application/handle-submit-response
  handle-submit)

(reg-event-fx
  :application/handle-submit-error
  (fn [cofx [_ response]]
    {:db (-> (update (:db cofx) :application dissoc :submit-status)
             (assoc :error {:message "Tapahtui virhe " :detail response}))}))

(reg-event-db
  :application/show-attachment-too-big-error
  (fn [db [_ component-id question-group-idx]]
    (let [error-path (if question-group-idx
                       [:application :answers (keyword component-id) :errors question-group-idx :too-big]
                       [:application :answers (keyword component-id) :errors :too-big])
          db         (cond-> db
                       question-group-idx
                       (update-in [:application :answers (keyword component-id) :errors] (util/vector-of-length question-group-idx)))]
      (assoc-in db error-path true))))

(reg-event-fx
  :application/submit
  (fn [{:keys [db]} _]
    (send-application db :post)))

(reg-event-fx
  :application/edit
  (fn [{:keys [db]} _]
    (send-application db :put)))

(reg-event-db
  :application/hide-hakukohteet-if-no-tarjonta
  (fn [db _]
    (assoc-in db [:application :ui :hakukohteet :visible?] (boolean (-> db :form :tarjonta)))))

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

(defn- set-field-visibility
  [visible? db field-descriptor]
  (let [id (keyword (:id field-descriptor))
        db (assoc-in db [:application :ui id :visible?] visible?)]
    (if (or (= (:fieldType field-descriptor) "adjacentfieldset")
            (= (:fieldClass field-descriptor) "questionGroup"))
      (reduce (partial set-field-visibility visible?)
              db
              (:children field-descriptor))
      db)))

(defn- set-single-choice-followup-visibility
  [db field-descriptor value]
  (reduce (fn [db option]
            (reduce (partial set-field-visibility (= value (:value option)))
                    db
                    (:followups option)))
          db
          (:options field-descriptor)))

(defn- set-multiple-choice-followup-visibility
  [db field-descriptor option]
  (let [id (keyword (:id field-descriptor))
        selected? (get-in db [:application :answers id :options (:value option)])]
    (reduce (partial set-field-visibility selected?)
            db
            (:followups option))))

(defn- set-multi-value-changed [db id value-key]
  (let [answer (-> db :application :answers id)
        [new-diff original-diff _] (d/diff (get answer value-key) (:original-value answer))]
    (update-in db [:application :values-changed?] (fn [values]
                                                    (let [values (or values #{})]
                                                      (if (and (empty? new-diff)
                                                               (empty? original-diff))
                                                        (disj values id)
                                                        (conj values id)))))))

(defn- toggle-multiple-choice-option [answer option-value question-group-idx]
  (let [option-path            (if question-group-idx
                                 [:options question-group-idx option-value]
                                 [:options option-value])
        answer                 (cond-> answer
                                 question-group-idx (update :options (util/vector-of-length (inc question-group-idx)))
                                 true (update-in option-path not))
        parse-option-values    (fn [options]
                                 (->> options
                                      (filter (comp true? second))
                                      (mapv first)))
        value                  (if question-group-idx
                                 (mapv parse-option-values (:options answer))
                                 (parse-option-values (:options answer)))]
    (assoc answer :value value)))

(defn- toggle-values
  [answer options]
  (let [question-group-answer? (and (vector? options)
                                    (every? vector? options))]
    (->> options
         (map-indexed (fn [question-group-idx option-or-options]
                        [(when question-group-answer? question-group-idx)
                         option-or-options]))
         (reduce (fn [answer [question-group-idx option-or-options]]
                   (if question-group-idx
                     (reduce #(toggle-multiple-choice-option %1 %2 question-group-idx)
                             answer
                             option-or-options)
                     (toggle-multiple-choice-option answer
                                                    option-or-options
                                                    nil)))
                 answer))))

(defn- merge-multiple-choice-option-values [value answer]
  (if (string? value)
    (toggle-values answer (clojure.string/split value #"\s*,\s*"))
    (toggle-values answer value)))

(defn- set-ssn-field-visibility [db]
  (rules/run-rule {:toggle-ssn-based-fields "ssn"} db))

(defn- set-country-specific-fields-visibility
  [db]
  (rules/run-rule {:change-country-of-residence nil} db))

(defonce multi-value-field-types #{"multipleChoice" "singleChoice" "textField" "attachment" "hakukohteet" "dropdown" "textArea"})

(defn- supports-multiple-values [field-type]
  (contains? multi-value-field-types field-type))

(defn- set-have-finnish-ssn
  [db]
  (let [ssn (get-in db [:application :answers :ssn])]
    (update-in db [:application :answers :have-finnish-ssn]
               merge {:valid true
                      :value (str (or (and (clojure.string/blank? (:value ssn))
                                           (:cannot-view ssn))
                                      (not (clojure.string/blank? (:value ssn)))))})))

(defn- populate-hakukohde-answers-if-necessary
  "Populate hakukohde answers for legacy applications where only top-level hakukohde array exists"
  [db]
  (let [hakukohteet (-> db :application :hakukohde)
        hakukohde-answers (-> db :application :answers :hakukohteet :value)]
    (if (and (not-empty hakukohteet)
             (empty? hakukohde-answers))
      (-> db
          (assoc-in [:application :answers :hakukohteet :values] (map (fn [oid] {:valid true :value oid}) hakukohteet))
          (assoc-in [:application :answers :hakukohteet :valid] true))
      db)))

(defn- >0? [x]
  (when (> x 0)
    x))

(defn set-question-group-row-amounts [db]
  (reduce-kv (fn [db answer-key {:keys [value values]}]
               (let [field-descriptor  (->> (:flat-form-content db)
                                            (filter (comp (partial = answer-key) keyword :id))
                                            (first))
                     question-group-id (-> field-descriptor :params :question-group-id)]
                 (cond-> db
                   question-group-id
                   (update-in [:application :ui question-group-id :count] #(let [provided-val ((some-fn >0?)
                                                                                               (-> values count)
                                                                                               (-> value count)
                                                                                               1)]
                                                                             (if (> % provided-val)
                                                                               %
                                                                               provided-val))))))
             db
             (-> db :application :answers)))

(defn- merge-single-choice-values [value answer]
  (if (and (vector? value)
           (every? vector? value))
    (merge answer {:valid true
                   :value value
                   :values (mapv (partial mapv (fn [value]
                                                 {:valid true :value value}))
                                 value)})
    (merge answer {:valid true :value value})))

(defn- merge-dropdown-values [value answer]
  (if (and (vector? value)
           (every? vector? value))
    (let [values (mapv (partial mapv (fn [value] {:valid true :value value})) value)]
      (merge answer {:valid true :values values}))
    (merge answer {:valid true :value value})))

(defn- merge-submitted-answers [db submitted-answers]
  (-> db
      (update-in [:application :answers]
                 (fn [answers]
                   (reduce (fn [answers {:keys [key value cannot-view] :as answer}]
                             (let [answer-key (keyword key)
                                   value      (cond-> value
                                                (and (vector? value)
                                                     (not (supports-multiple-values (:fieldType answer))))
                                                (first))]
                               (if (contains? answers answer-key)
                                 (let [answer (match answer
                                                     {:fieldType "multipleChoice"}
                                                     (-> answers
                                                         (update answer-key (partial merge-multiple-choice-option-values value))
                                                         (assoc-in [answer-key :valid] true))

                                                     {:fieldType "singleChoice"}
                                                     (update answers answer-key (partial merge-single-choice-values value))

                                                     {:fieldType "dropdown"}
                                                     (update answers answer-key (partial merge-dropdown-values value))

                                                     {:fieldType (field-type :guard supports-multiple-values) :value (_ :guard vector?)}
                                                     (letfn [(parse-values [value-or-values]
                                                               (if (vector? value-or-values)
                                                                 (mapv parse-values value-or-values)
                                                                 (cond-> {:valid true :value value-or-values}
                                                                         (= field-type "attachment")
                                                                         (assoc :status :ready))))]
                                                       (update answers answer-key merge
                                                               {:valid  true
                                                                :values (parse-values (:value answer))}))

                                                     :else
                                                     (update answers answer-key merge {:valid true :value value}))]
                                   (assoc-in answer [answer-key :cannot-view] cannot-view))
                                 answers)))
                           answers
                           submitted-answers)))
      (populate-hakukohde-answers-if-necessary)
      (set-have-finnish-ssn)
      (set-ssn-field-visibility)
      (set-country-specific-fields-visibility)
      (set-question-group-row-amounts)))

(defn- set-followup-visibility
  [db]
  (autil/reduce-form-fields
   (fn [db field]
     (let [id (keyword (:id field))
           value (get-in db [:application :answers id :value])
           options (:options field)
           field-type (:fieldType field)]
       (if (and options
                (not-empty (mapcat :followups options)))
         (if (or (= "dropdown" field-type)
                 (= "singleChoice" field-type))
           (set-single-choice-followup-visibility db field value)
           (reduce (fn [db option]
                     (set-multiple-choice-followup-visibility db field option))
                   db
                   options))
         db)))
   db
   (get-in db [:form :content])))

(defn- original-values->answers [db]
  (cond-> db
    (or (-> db :application :secret)
        (-> db :application :virkailija-secret))
    (update-in [:application :answers]
               (partial reduce-kv
                        (fn [answers answer-key {:keys [value values] :as answer}]
                          (let [answer (assoc answer :original-value
                                              (or value (if (every? sequential? values)
                                                          (mapv (partial mapv :value) values)
                                                          (mapv :value values))))]
                            (assoc answers answer-key answer)))
                        {}))))

(defn- set-question-group-id
  ([field]
   (let [update-group-child (partial set-question-group-id (keyword (:id field)))
         update-followups (fn [option] (update option :followups (partial map set-question-group-id)))]
     (if (= "questionGroup" (:fieldClass field))
       (update field :children (partial map update-group-child))
       (cond-> field
         (contains? field :children)
         (update :children (partial map set-question-group-id))
         (contains? field :options)
         (update :options (partial map update-followups))))))
  ([question-group-id field]
   (let [update-child (partial set-question-group-id question-group-id)
         update-followups (fn [option] (update option :followups (partial map update-child)))]
     (cond-> (assoc-in field [:params :question-group-id] question-group-id)
       (contains? field :children)
       (update :children (partial map update-child))
       (contains? field :options)
       (update :options (partial map update-followups))))))

(defn handle-form [{:keys [db]} [_ answers form]]
  (let [form (-> (languages->kwd form)
                 (set-form-language)
                 (update :content (partial map set-question-group-id)))
        preselected-hakukohde (-> db :application :preselected-hakukohde)]
    {:db         (-> db
                     (update :form (fn [{:keys [selected-language]}]
                                     (cond-> form
                                             (some? selected-language)
                                             (assoc :selected-language selected-language))))
                     (assoc :flat-form-content (autil/flatten-form-fields (:content form)))
                     (assoc-in [:application :answers] (create-initial-answers form preselected-hakukohde))
                     (assoc-in [:application :show-hakukohde-search] false)
                     (assoc :wrapper-sections (extract-wrapper-sections form))
                     (merge-submitted-answers answers)
                     (original-values->answers)
                     (set-followup-visibility))
     :dispatch-n [[:application/hide-hakukohteet-if-no-tarjonta]
                  [:application/show-answers-belonging-to-hakukohteet]
                  [:application/hakukohde-query-change "" 0]
                  [:application/set-page-title]]}))

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

(reg-event-fx
  :application/textual-field-blur
  (fn [{db :db} [_ field]]
    (let [id (keyword (:id field))
          answer (get-in db [:application :answers id])]
      {:dispatch-n (if (or (empty? (:blur-rules field))
                           (not (:valid answer)))
                     []
                     [[:application/run-rule (:blur-rules field)]])})))

(reg-event-fx
  :application/set-application-field-valid
  (fn [{db :db} [_ field-descriptor valid? errors]]
    (let [id (keyword (:id field-descriptor))
          rules (:rules field-descriptor)]
      (cond-> {:db (-> db
                       (assoc-in [:application :answers id :valid] valid?)
                       (assoc-in [:application :answers id :errors] errors))}
        (not (empty? rules))
        (assoc :dispatch [:application/run-rule rules])))))

(defn- transform-value [value field-descriptor]
  (let [t (case (:id field-descriptor)
            "birth-date" value-transformers/birth-date
            identity)]
    (or (t value) value)))

(reg-event-fx
  :application/set-application-field
  (fn [{db :db} [_ field value]]
    (let [value    (transform-value value field)
          id       (keyword (:id field))
          answers  (get-in db [:application :answers])]
      {:db (-> db
               (assoc-in [:application :answers id :value] value)
               (set-multi-value-changed id :value))
       :validate {:value value
                  :answers answers
                  :field-descriptor field
                  :editing? (get-in db [:application :editing?])
                  :on-validated (fn [[valid? errors]]
                                  (dispatch [:application/set-application-field-valid
                                             field valid? errors]))}})))

(defn- set-repeatable-field-values
  [db field-descriptor value data-idx question-group-idx]
  (let [id         (keyword (:id field-descriptor))
        value-path (cond-> [:application :answers id :values]
                     question-group-idx (conj question-group-idx))]
    (-> db
        (update-in [:application :answers id :values] (util/vector-of-length (inc question-group-idx)))
        (update-in value-path (fnil assoc []) data-idx {:value value}))))

(defn- set-repeatable-field-value
  [db field-descriptor group-idx]
  (let [id                   (keyword (:id field-descriptor))
        values               (get-in db [:application :answers id :values])
        multi-value-answers? (some? group-idx)
        value                (if multi-value-answers?
                               (mapv (partial mapv :value) values)
                               (mapv :value values))]
    (-> db
        (assoc-in [:application :answers id :value] value)
        (set-multi-value-changed id :value))))

(defn- set-repeatable-application-repeated-field-valid
  [db id group-idx data-idx valid?]
  (let [path (cond-> [:application :answers id :values]
               (some? group-idx) (conj group-idx))]
    (assoc-in db (conj path data-idx :valid) valid?)))

(defn- set-repeatable-application-field-top-level-valid
  [db id group-idx required? valid?]
  (let [values (get-in db [:application :answers id :values])
        multi-value-answers? (some? group-idx)
        is-empty? (if multi-value-answers?
                    (some empty? values)
                    (empty? values))
        all-valid? (and (every? :valid (flatten values)) valid?)]
    (assoc-in db [:application :answers id :valid] (if is-empty?
                                                     (not required?)
                                                     all-valid?))))

(reg-event-db
  :application/set-repeatable-application-field-valid
  (fn [db [_ id group-idx data-idx required? valid?]]
    (-> db
        (set-repeatable-application-repeated-field-valid id group-idx data-idx valid?)
        (set-repeatable-application-field-top-level-valid id group-idx required? valid?))))

(reg-event-fx
  :application/set-repeatable-application-field
  (fn [{db :db} [_ field-descriptor value data-idx question-group-idx]]
    {:db (-> db
             (set-repeatable-field-values field-descriptor value data-idx question-group-idx)
             (set-repeatable-field-value field-descriptor question-group-idx))
     :validate {:value value
                :answers (get-in db [:application :answers])
                :field-descriptor field-descriptor
                :editing? (get-in db [:application :editing?])
                :on-validated (fn [[valid? errors]]
                                (dispatch [:application/set-repeatable-application-field-valid
                                           (keyword (:id field-descriptor))
                                           question-group-idx
                                           data-idx
                                           (required? field-descriptor)
                                           valid?]))}}))

(defn- remove-repeatable-field-value
  [db field-descriptor data-idx question-group-idx]
  (let [id              (keyword (:id field-descriptor))
        raw-value-path  (cond-> [:application :answers id :values]
                          question-group-idx (conj question-group-idx))
        disp-value-path (cond-> [:application :answers id :value]
                          question-group-idx (conj question-group-idx))]
    (cond-> db
      (seq (get-in db raw-value-path))
      (update-in raw-value-path
                 #(autil/remove-nth % data-idx))

      ;; when creating application, we have the value below (and it's important). when editing, we do not.
      ;; consider this a temporary, terrible bandaid solution
      (seq (get-in db disp-value-path))
      (update-in disp-value-path
                 #(autil/remove-nth (vec %) data-idx))

      true
      (set-repeatable-field-value field-descriptor question-group-idx))))

(reg-event-db
  :application/remove-repeatable-application-field-value
  (fn [db [_ field-descriptor data-idx question-group-idx]]
    (remove-repeatable-field-value db field-descriptor data-idx question-group-idx)))

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
    (update-in db [:application :answers :postal-office]
               merge {:value (:fi postal-office-name) :valid true})))

(reg-event-db
  :application/handle-postal-code-error
  (fn [db _]
    (-> db
        (update-in [:application :answers :postal-code]
                   merge {:valid false})
        (update-in [:application :answers :postal-office]
                   merge {:value "" :valid false}))))

(reg-event-db
  :application/set-multiple-choice-valid
  (fn [db [_ field-descriptor valid?]]
    (assoc-in db [:application :answers (keyword (:id field-descriptor)) :valid] valid?)))

(reg-event-fx
  :application/toggle-multiple-choice-option
  (fn [{db :db} [_ field-descriptor option question-group-idx]]
    (let [id (keyword (:id field-descriptor))
          db (-> db
                 (update-in [:application :answers id]
                            (fn [answer]
                              (toggle-multiple-choice-option answer
                                                             (:value option)
                                                             question-group-idx)))
                 (set-multi-value-changed id :value))]
      (if question-group-idx
        {:db db
         :validate-every {:values (get-in db [:application :answers id :value])
                          :answers (get-in db [:application :answers])
                          :field-descriptor field-descriptor
                          :editing? (get-in db [:application :editing?])
                          :on-validated (fn [[valid? errors]]
                                          (dispatch [:application/set-multiple-choice-valid
                                                     field-descriptor
                                                     valid?]))}}
        {:db (set-multiple-choice-followup-visibility db field-descriptor option)
         :validate {:value (get-in db [:application :answers id :value])
                    :answers (get-in db [:application :answers])
                    :field-descriptor field-descriptor
                    :editing? (get-in db [:application :editing?])
                    :on-validated (fn [[valid? errors]]
                                    (dispatch [:application/set-multiple-choice-valid
                                               field-descriptor
                                               valid?]))}}))))

(reg-event-fx
  :application/select-single-choice-button
  (fn [{db :db} [_ value field-descriptor question-group-idx]]
    (let [id (keyword (:id field-descriptor))
          button-path [:application :answers id]
          value-path (cond-> button-path
                       (some? question-group-idx)
                       (conj :values question-group-idx 0)
                       true
                       (conj :value))
          current-value (get-in db value-path)
          new-value (when (not= value current-value) value)]
      {:db (if (some? question-group-idx)
             (-> db
                 (update-in (conj button-path :values) (util/vector-of-length (inc question-group-idx)))
                 (update-in (conj button-path :values question-group-idx) (fnil identity []))
                 (assoc-in value-path new-value)
                 (update-in button-path (fn [answer]
                                          (assoc answer :value (mapv (partial mapv :value)
                                                                     (:values answer)))))
                 (set-multi-value-changed id :value)
                 (set-single-choice-followup-visibility field-descriptor value))
             (-> db
                 (assoc-in value-path new-value)
                 (set-multi-value-changed id :value)
                 (set-single-choice-followup-visibility field-descriptor value)))
       :validate {:value new-value
                  :answers (get-in db [:application :answers])
                  :field-descriptor field-descriptor
                  :editing? (get-in db [:application :editing?])
                  :on-validated (fn [[valid? errors]]
                                  (dispatch [:application/set-repeatable-application-field-valid
                                             id
                                             question-group-idx
                                             0
                                             (required? field-descriptor)
                                             valid?]))}})))

(reg-event-fx
  :application/set-adjacent-field-answer
  (fn [{db :db} [_ field-descriptor idx value question-group-idx]]
    {:db (-> db
             (set-repeatable-field-values field-descriptor value idx question-group-idx)
             (set-repeatable-field-value field-descriptor question-group-idx))
     :validate {:value value
                :answers (get-in db [:application :answers])
                :field-descriptor field-descriptor
                :editing? (get-in db [:application :editing?])
                :on-validated (fn [[valid? errors]]
                                (dispatch [:application/set-repeatable-application-field-valid
                                           (keyword (:id field-descriptor))
                                           question-group-idx
                                           idx
                                           (required? field-descriptor)
                                           valid?]))}}))

(reg-event-fx
  :application/add-adjacent-fields
  (fn [{db :db} [_ field-descriptor question-group-idx]]
    {:dispatch-n
     (reduce (fn [dispatch child]
               (let [id (keyword (:id child))
                     new-idx (count (if question-group-idx
                                      (get-in db [:application :answers id :values question-group-idx])
                                      (get-in db [:application :answers id :values])))]
                 (conj dispatch [:application/set-adjacent-field-answer
                                 child new-idx "" question-group-idx])))
             []
             (:children field-descriptor))}))

(reg-event-db
  :application/remove-adjacent-field
  (fn [db [_ field-descriptor index]]
    (reduce #(remove-repeatable-field-value %1 %2 index nil)
            db
            (:children field-descriptor))))

(reg-event-fx
  :application/add-single-attachment
  (fn [{:keys [db]} [_ field-descriptor component-id attachment-idx file retries question-group-idx]]
    (let [name      (.-name file)
          form-data (doto (js/FormData.)
                      (.append "file" file name))]
      {:db   db
       :http {:method    :post
              :url       "/hakemus/api/files"
              :handler   [:application/handle-attachment-upload field-descriptor component-id attachment-idx question-group-idx]
              :error-handler [:application/handle-attachment-upload-error field-descriptor component-id attachment-idx name file (inc retries) question-group-idx]
              :body      form-data}})))

(reg-event-fx
  :application/add-attachments
  (fn [{:keys [db]} [_ field-descriptor component-id attachment-count files question-group-idx]]
    (let [row-count     (when (some? question-group-idx)
                          (get-in db [:application :ui (get-in field-descriptor [:params :question-group-id]) :count] 1))
          files         (filter (fn [file]
                                  (let [prev-files (get-in db [:application :answers (keyword component-id) :values])
                                        new-file   {:filename (.-name file)
                                                    :size     (.-size file)}]
                                    (not (some (partial = new-file)
                                               (eduction (map :value)
                                                         (map #(select-keys % [:filename :size]))
                                                         prev-files)))))
                                files)
          dispatch-list (map-indexed (fn file->dispatch-vec [idx file]
                                       [:application/add-single-attachment field-descriptor component-id (+ attachment-count idx) file 0 question-group-idx])
                                     files)
          db            (if (not-empty files)
                          (as-> db db'
                                (update-in db' [:application :answers (keyword component-id) :values] (util/vector-of-length (or row-count 0)))
                                (cond-> db'
                                  question-group-idx
                                  (update-in [:application :answers (keyword component-id) :values question-group-idx] (fnil identity [])))
                                (->> files
                                     (map-indexed (fn attachment-idx->file [idx file]
                                                    {:idx (+ attachment-count idx) :file file}))
                                     (reduce (fn attachment-spec->db [db {:keys [idx file]}]
                                               (assoc-in db (if question-group-idx
                                                              [:application :answers (keyword component-id) :values question-group-idx idx]
                                                              [:application :answers (keyword component-id) :values idx])
                                                 {:value   {:filename     (.-name file)
                                                            :content-type (.-type file)
                                                            :size         (.-size file)}
                                                  :valid   false
                                                  :too-big false
                                                  :status  :uploading}))
                                             db'))
                                (assoc-in db' [:application :answers (keyword component-id) :valid] false))
                          db)]
      {:db         db
       :dispatch-n dispatch-list})))

(reg-event-db
  :application/set-attachment-valid
  (fn [db [_ id required? valid?]]
    (let [answer (get-in db [:application :answers id])
          question-group-answer? (and (vector? (:values answer))
                                      (not (empty? (:values answer)))
                                      (every? vector? (:values answer)))]
      (assoc-in db [:application :answers id :valid]
                (and (if question-group-answer?
                       (every? (partial every? :valid) (:values answer))
                       (every? :valid (:values answer)))
                     (not (and required?
                               (if question-group-answer?
                                 (some empty? (:values answer))
                                 (empty? (:values answer)))))
                     valid?)))))

(reg-event-fx
  :application/handle-attachment-upload
  (fn [{db :db} [_ field-descriptor component-id attachment-idx question-group-idx response]]
    (let [path (if question-group-idx
                  [:application :answers (keyword component-id) :values question-group-idx attachment-idx]
                  [:application :answers (keyword component-id) :values attachment-idx])]
      {:db (-> db
               (update-in path
                          merge
                          {:value response :valid true :status :ready})
               (set-multi-value-changed (keyword component-id) :values))
       :validate {:value (get-in db path)
                  :answers (get-in db [:application :answers])
                  :field-descriptor field-descriptor
                  :editing? (get-in db [:application :editing?])
                  :on-validated (fn [[valid? errors]]
                                  (dispatch [:application/set-attachment-valid
                                             (keyword component-id)
                                             (required? field-descriptor)
                                             valid?]))}})))

(defn- rate-limit-error? [response]
  (= (:status response) 429))

(reg-event-fx
  :application/handle-attachment-upload-error
  (fn [{:keys [db]} [_ field-descriptor component-id attachment-idx filename file retries question-group-idx response]]
    (let [rate-limited? (rate-limit-error? response)
          current-error (if rate-limited?
                          (util/get-translation :file-upload-failed)
                          (util/get-translation :file-type-forbidden))]
      (if (and rate-limited? (< retries 3))
        {:db db
         :delayed-dispatch {:dispatch-vec [:application/add-single-attachment field-descriptor component-id attachment-idx file retries question-group-idx]
                            :timeout (+ 2000 (rand-int 2000))}}
        {:db (-> db
                 (update-in (if question-group-idx
                              [:application :answers (keyword component-id) :values question-group-idx attachment-idx]
                              [:application :answers (keyword component-id) :values attachment-idx])
                            merge
                            {:value {:filename filename} :valid false :status :error :error current-error})
                 (assoc-in [:applicatin :answers (keyword component-id) :valid] false))}))))

(reg-event-fx
  :application/handle-attachment-delete
  (fn [{db :db} [_ field-descriptor component-id question-group-idx attachment-key _]]
    {:db (-> db
             (update-in (if (some? question-group-idx)
                          [:application :answers (keyword component-id) :values question-group-idx]
                          [:application :answers (keyword component-id) :values])
                        (comp vec
                              (partial remove (comp (partial = attachment-key) :key :value))))
             (set-multi-value-changed (keyword component-id) :values))
     :dispatch [:application/set-attachment-valid
                (keyword component-id)
                (required? field-descriptor)
                true]}))

(reg-event-fx
  :application/remove-attachment
  (fn [{:keys [db]} [_ field-descriptor component-id attachment-idx question-group-idx]]
    (let [key       (get-in db (if question-group-idx
                                 [:application :answers (keyword component-id) :values question-group-idx attachment-idx :value :key]
                                 [:application :answers (keyword component-id) :values attachment-idx :value :key]))
          db        (-> db
                        (assoc-in [:application :answers (keyword component-id) :valid] false)
                        (update-in (if question-group-idx
                                     [:application :answers (keyword component-id) :values question-group-idx attachment-idx]
                                     [:application :answers (keyword component-id) :values attachment-idx])
                                   merge
                                   {:status :deleting
                                    :valid  false}))
          db-and-fx {:db db}]
      (if (get-in db [:application :editing?])
        (assoc db-and-fx :dispatch [:application/handle-attachment-delete field-descriptor component-id question-group-idx key])
        (assoc db-and-fx :http {:method  :delete
                                :url     (str "/hakemus/api/files/" key)
                                :handler [:application/handle-attachment-delete field-descriptor component-id question-group-idx key]})))))

(reg-event-fx
  :application/remove-attachment-error
  (fn [{db :db} [_ field-descriptor component-id attachment-idx question-group-idx]]
    (let [id (keyword component-id)]
      {:db (update-in db (cond-> [:application :answers id :values]
                           (some? question-group-idx)
                           (conj question-group-idx))
                      autil/remove-nth attachment-idx)
       :dispatch [:application/set-attachment-valid
                  id
                  (required? field-descriptor)
                  true]})))

(reg-event-db
  :application/rating-hover
  (fn [db [_ star-number]]
    (assoc-in db [:application :feedback :star-hovered] star-number)))

(reg-event-db
  :application/rating-submit
  (fn [db [_ star-number]]
    (-> db
        (assoc-in [:application :feedback :stars] star-number)
        (assoc-in [:application :feedback :status] :rating-given))))

(reg-event-db
  :application/rating-update-feedback
  (fn [db [_ feedback-text]]
    (assoc-in db [:application :feedback :text] feedback-text)))

(reg-event-fx
  :application/rating-feedback-submit
  (fn [{:keys [db]}]
    (let [lang-kw   (keyword (-> db :form :selected-language))
          new-db    (assoc-in db [:application :feedback :status] :feedback-submitted)
          feedback  (-> db :application :feedback)
          text      (:text feedback)
          post-data {:form-key   (-> db :form :key)
                     :form-id    (-> db :form :id)
                     :form-name  (-> db :form :name lang-kw)
                     :user-agent (.-userAgent js/navigator)
                     :rating     (:stars feedback)
                     :feedback   (when text
                                   (subs text 0 2000))}]
      {:db   new-db
       :http {:method    :post
              :post-data post-data
              :url       "/hakemus/api/feedback"}})))

(reg-event-db
  :application/rating-form-toggle
  (fn [db _]
    (update-in db [:application :feedback :hidden?] not)))

(reg-event-fx
  :application/set-page-title
  (fn [{:keys [db]}]
    (let [lang-kw       (keyword (-> db :form :selected-language))
          title-prefix  (util/get-translation :page-title)
          title-suffix  (or
                          (lang-kw (-> db :form :tarjonta :haku-name))
                          (-> db :form :name lang-kw))]
      {:db db
       :set-page-title (str title-prefix " – " title-suffix)})))

(defn- set-empty-value-dispatch
  [group-idx field-descriptor]
  (let [id (keyword (:id field-descriptor))]
    (match field-descriptor
      {:fieldType (:or "dropdown" "textField" "textArea")}
      [[:application/set-repeatable-application-field
        field-descriptor
        ""
        0
        group-idx]]
      {:fieldType "singleChoice"}
      (let [d [:application/select-single-choice-button
               (:value (first (:options field-descriptor)))
               field-descriptor
               group-idx]]
        [d d])
      {:fieldType "multipleChoice"}
      (let [d [:application/toggle-multiple-choice-option
               field-descriptor
               (first (:options field-descriptor))
               group-idx]]
        [d d])
      {:fieldType "adjacentfieldset"}
      (mapv (fn [child]
              [:application/set-adjacent-field-answer child 0 "" group-idx])
            (:children field-descriptor))
      {:fieldType "attachment"}
      []
      {:fieldClass "infoElement"}
      [])))

(defn- set-empty-value-dispatches
  [db id group-idx]
  (autil/reduce-form-fields (fn [dispatches field]
                              (if (= id (:id field))
                                (mapcat (partial set-empty-value-dispatch group-idx)
                                        (:children field))
                                dispatches))
                            []
                            (get-in db [:form :content])))

(reg-event-fx
  :application/add-question-group-row
  (fn add-question-group-row [{db :db} [_ field-descriptor-id]]
    (let [id (keyword field-descriptor-id)
          repeat-count (get-in db [:application :ui id :count] 1)]
      {:db (-> db
               (assoc-in [:application :ui id :count] (inc repeat-count))
               (update-in [:application :ui id] dissoc :mouse-over-remove-button))
       :dispatch-n (set-empty-value-dispatches db field-descriptor-id repeat-count)})))

(reg-event-db
  :application/remove-question-group-row
  (fn [db [_ field-descriptor idx]]
    (let [id (keyword (:id field-descriptor))
          with-decremented-count (-> db
                                     (update-in [:application :ui id :count] dec)
                                     (update-in [:application :ui id] dissoc :mouse-over-remove-button))]
      (autil/reduce-form-fields
       (fn [db child]
         (let [id (keyword (:id child))
               answer (get-in db [:application :answers id])]
           (cond-> db
             (contains? answer :values)
             (update-in [:application :answers id :values]
                        autil/remove-nth idx)
             (contains? answer :value)
             (update-in [:application :answers id :value]
                        autil/remove-nth idx)
             (and (contains? answer :values)
                  (contains? answer :valid))
             (update-in [:application :answers id]
                        #(assoc % :valid (->> (:values %)
                                              flatten
                                              (every? :valid))))
             (or (contains? answer :values)
                 (contains? answer :value))
             (update-in [:application :values-changed?] conj id))))
       with-decremented-count
       (:children field-descriptor)))))

(reg-event-fx
  :application/dropdown-change
  (fn [{db :db} [_ field-descriptor value group-idx]]
    {:db (set-single-choice-followup-visibility db field-descriptor value)
     :dispatch (if (some? group-idx)
                 [:application/set-repeatable-application-field field-descriptor value 0 group-idx]
                 [:application/set-application-field field-descriptor value])}))

(reg-event-db
  :application/remove-question-group-mouse-over
  (fn [db [_ field-descriptor idx]]
    (assoc-in db [:application :ui (keyword (:id field-descriptor)) :mouse-over-remove-button idx] true)))

(reg-event-db
  :application/remove-question-group-mouse-out
  (fn [db [_ field-descriptor idx]]
    (assoc-in db [:application :ui (keyword (:id field-descriptor)) :mouse-over-remove-button idx] false)))
