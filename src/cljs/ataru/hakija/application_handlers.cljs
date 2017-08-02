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
            [taoensso.timbre :refer-macros [spy debug]]))

(defn initialize-db [_ _]
  {:form        nil
   :application {:answers {}}})

(defn- handle-get-application [{:keys [db]}
                               [_ secret {:keys [answers
                                                 form-key
                                                 lang
                                                 haku
                                                 hakukohde
                                                 hakukohde-name
                                                 state]}]]
  {:db       (-> db
                 (assoc-in [:application :editing?] true)
                 (assoc-in [:application :secret] secret)
                 (assoc-in [:application :state] state)
                 (assoc-in [:form :selected-language] (or (keyword lang) :fi))
                 (assoc-in [:form :hakukohde-name] hakukohde-name))
   :dispatch (if haku
               [:application/get-latest-form-by-haku haku answers]
               [:application/get-latest-form-by-key form-key answers])})

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

(defn- get-latest-form-by-hakukohde [{:keys [db]} [_ hakukohde-oid answers]]
  {:db   (assoc-in db [:application :preselected-hakukohde] hakukohde-oid)
   :http {:method  :get
          :url     (str "/hakemus/api/hakukohde/" hakukohde-oid)
          :handler [:application/handle-form answers]}})

(reg-event-fx
  :application/get-latest-form-by-hakukohde
  get-latest-form-by-hakukohde)

(reg-event-fx
  :application/get-latest-form-by-haku
  (fn [{:keys [db]} [_ haku-oid answers]]
    {:db db
     :http {:method  :get
            :url     (str "/hakemus/api/haku/" haku-oid)
            :handler [:application/handle-form answers]}}))

(defn handle-submit [db _]
  (assoc-in db [:application :submit-status] :submitted))

(defn send-application [db method]
  {:db       (-> db (assoc-in [:application :submit-status] :submitting) (dissoc :error))
   :http     {:method        method
              :url           "/hakemus/api/application"
              :post-data     (create-application-to-submit (:application db) (:form db) (get-in db [:form :selected-language]))
              :handler       :application/handle-submit-response
              :error-handler :application/handle-submit-error}})

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
  (fn [db [_ component-id]]
    (assoc-in db [:application :answers (keyword component-id) :too-big] true)))

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

(defn- toggle-multiple-choice-option [answer option-value validators answers-by-key]
  (let [answer (update-in answer [:options option-value] not)
        value  (->> (:options answer)
                    (filter (comp true? second))
                    (map first))
        valid  (if (not-empty validators)
                 (every? true? (map #(validator/validate % value answers-by-key nil) validators))
                 true)]
    (merge answer {:value value :valid valid})))

(defn- select-single-choice-button [db [_ button-id value validators]]
  (let [button-path   [:application :answers button-id]
        current-value (:value (get-in db button-path))
        new-value     (when (not= value current-value) value)]
    (update-in db button-path
               (fn [answer]
                 (let [valid? (if (not-empty validators)
                                (every? true? (map #(validator/validate % new-value (-> db :application :answers) nil) validators))
                                true)]
                   (merge answer {:value new-value
                                  :valid valid?}))))))

(defn- toggle-values
  [answer options answers-by-key]
  (reduce (fn [answer option-value]
            (toggle-multiple-choice-option answer option-value nil answers-by-key))
          answer
          options))

(defn- merge-multiple-choice-option-values [value answers-by-key answer]
  (if (string? value)
    (toggle-values answer (clojure.string/split value #"\s*,\s*") answers-by-key)
    (toggle-values answer value answers-by-key)))

(defn- set-ssn-field-visibility [db]
  (rules/run-rule {:toggle-ssn-based-fields "ssn"} db))

(defn- set-country-specific-fields-visibility
  [db]
  (rules/run-rule {:change-country-of-residence nil} db))

(defonce multi-value-field-types #{"textField" "attachment" "hakukohteet"})

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

(defn- merge-submitted-answers [db submitted-answers]
  (-> db
      (update-in [:application :answers]
        (fn [answers]
          (reduce (fn [answers {:keys [key value cannot-edit cannot-view] :as answer}]
                    (let [answer-key (keyword key)
                          value      (cond-> value
                                             (and (vector? value)
                                                  (not (supports-multiple-values (:fieldType answer))))
                                             (first))]
                      (if (contains? answers answer-key)
                        (update
                          (match answer
                                 {:fieldType "multipleChoice"}
                                 (update answers answer-key (partial merge-multiple-choice-option-values value (-> db :application :answers)))

                                 {:fieldType "dropdown"}
                                 (update answers answer-key merge {:valid true :value value})

                                 {:fieldType (field-type :guard supports-multiple-values) :value (_ :guard vector?)}
                                 (update answers answer-key merge
                                   {:valid  true
                                    :values (mapv (fn [value]
                                                    (cond-> {:valid true :value value}
                                                      (= field-type "attachment")
                                                      (assoc :status :ready)))
                                                  (:value answer))})

                                 :else
                                 (update answers answer-key merge {:valid true :value value}))
                          answer-key merge {:cannot-edit cannot-edit :cannot-view cannot-view})
                        answers)))
                  answers
                  submitted-answers)))
      set-have-finnish-ssn
      (set-ssn-field-visibility)
      (set-country-specific-fields-visibility)))

(defn- set-followup-visibility-to-false
  [db]
  (assoc-in db [:application :ui]
            (->> (autil/flatten-form-fields (:content (:form db)))
                 (filter :followup?)
                 (map (fn [field]
                        (let [id (keyword (:id field))
                              has-value? (or (some? (get-in db [:application :answers id :value]))
                                             (some #(some? (:value %))
                                                   (get-in db [:application :answers id :values] [])))
                              has-children? (not (empty? (:children field)))]
                          {id {:visible? (or has-value? has-children?)}})))
                 (reduce merge))))

(defn handle-form [{:keys [db]} [_ answers form]]
  (let [form (-> (languages->kwd form)
                 (set-form-language))]
    {:db (-> db
             (update :form (fn [{:keys [selected-language]}]
                             (cond-> form
                               (some? selected-language)
                               (assoc :selected-language selected-language))))
             (assoc-in [:application :answers] (create-initial-answers form (-> db :application :preselected-hakukohde)))
             (assoc-in [:application :show-hakukohde-search] true)
             (assoc :wrapper-sections (extract-wrapper-sections form))
             (merge-submitted-answers answers)
             set-followup-visibility-to-false)
     :dispatch [:application/hide-hakukohteet-if-no-tarjonta]}))

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
  :application/set-application-field
  (fn [{db :db} [_ field value]]
    (let [id (keyword (:id field))
          answers (get-in db [:application :answers])
          answer (get answers id)
          valid? (or (:cannot-view answer)
                     (:cannot-edit answer)
                     (every? #(validator/validate % value answers)
                             (:validators field)))]
      {:db (update-in db [:application :answers id]
                      merge {:valid valid? :value value})
       :dispatch-n (if (empty? (:rules field))
                     []
                     [[:application/run-rule (:rules field)]])})))

(defn- set-repeatable-field-values
  [db field-descriptor idx value]
  (let [id (keyword (:id field-descriptor))
        answers (get-in db [:application :answers])
        answer (get answers id)
        valid? (or (:cannot-view answer)
                   (:cannot-edit answer)
                   (every? #(validator/validate % value answers)
                           (:validators field-descriptor)))]
    (update-in db [:application :answers id :values]
               (fnil assoc []) idx {:valid valid? :value value})))

(defn- set-repeatable-field-value
  [db field-descriptor]
  (let [id (keyword (:id field-descriptor))
        values (get-in db [:application :answers id :values])
        required? (some (partial = "required")
                        (:validators field-descriptor))
        valid? (if (empty? values)
                 (not required?)
                 (every? :valid values))]
    (update-in db [:application :answers id]
               merge {:valid valid? :value (mapv :value values)})))

(reg-event-db
  :application/set-repeatable-application-field
  (fn [db [_ field-descriptor idx value]]
    (-> db
        (set-repeatable-field-values field-descriptor idx value)
        (set-repeatable-field-value field-descriptor))))

(reg-event-db
  :application/remove-repeatable-application-field-value
  (fn [db [_ key idx]]
    (cond-> db
            (seq (get-in db [:application :answers key :values]))
            (update-in [:application :answers key :values]
                       #(autil/remove-nth % idx))

            ; when creating application, we have the value below (and it's important). when editing, we do not.
            ; consider this a temporary, terrible bandaid solution
            (seq (get-in db [:application :answers key :value]))
            (update-in [:application :answers key :value]
                       #(autil/remove-nth (vec %) idx)))))

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
  :application/toggle-multiple-choice-option
  (fn [db [_ multiple-choice-id option-value validators]]
    (update-in db [:application :answers multiple-choice-id]
      (fn [answer]
        (toggle-multiple-choice-option answer option-value validators (-> db :application :answers))))))

(reg-event-db
  :application/select-single-choice-button
  select-single-choice-button)

(defn- required? [field-descriptor]
  (some? ((set (:validators field-descriptor)) "required")))

(defn- set-adjacent-field-validity
  [field-descriptor {:keys [values] :as answer}]
    (assoc answer
      :valid
      (boolean
        (some->>
          (map :valid (or
                        (not-empty values)
                        [{:valid (not (required? field-descriptor))}]))
          not-empty
          (every? true?)))))

(reg-event-db
  :application/set-adjacent-field-answer
  (fn [db [_ field-descriptor id idx value]]
    (-> (update-in db [:application :answers id :values]
                   (fn [answers]
                     (let [[init last] (split-at
                                         idx
                                         (or
                                           (not-empty answers)
                                           []))]
                       (vec (concat init [value] (rest last))))))
        (update-in [:application :answers id] (partial set-adjacent-field-validity field-descriptor)))))

(reg-event-db
  :application/add-adjacent-fields
  (fn [db [_ field-descriptor]]
    (let [children (map #(update % :id keyword) (:children field-descriptor))]
      (reduce (fn [db {:keys [id] :as child-descriptor}]
                (let [required? (required? child-descriptor)]
                  (-> db
                      (update-in [:application :answers id :values]
                        (fn [values]
                          (conj (or values [{:value nil :valid (not required?)}])
                                {:value nil :valid (not required?)})))
                      (update-in [:application :answers id]
                        (partial set-adjacent-field-validity child-descriptor)))))
              db
              children))))

(reg-event-db
  :application/remove-adjacent-field
  (fn [db [_ field-descriptor index]]
    (let [children (map #(update % :id keyword) (:children field-descriptor))]
      (reduce (fn [db {:keys [id] :as child-descriptor}]
                (-> db
                    (update-in [:application :answers id :values]
                      (fn [answers]
                        (vec (concat
                               (subvec answers 0 index)
                               (subvec answers (inc index))))))
                    (update-in [:application :answers id]
                      (partial set-adjacent-field-validity child-descriptor))))
              db
              children))))

(reg-event-fx
  :application/add-single-attachment
  (fn [{:keys [db]} [_ field-descriptor component-id attachment-idx file retries]]
    (let [name      (.-name file)
          form-data (doto (js/FormData.)
                      (.append "file" file name))]
      {:db   db
       :http {:method    :post
              :url       "/hakemus/api/files"
              :handler   [:application/handle-attachment-upload field-descriptor component-id attachment-idx]
              :error-handler [:application/handle-attachment-upload-error field-descriptor component-id attachment-idx name file (inc retries)]
              :body      form-data}})))

(reg-event-fx
  :application/add-attachments
  (fn [{:keys [db]} [_ field-descriptor component-id attachment-count files]]
    (let [dispatch-list (map-indexed (fn file->dispatch-vec [idx file]
                                       [:application/add-single-attachment field-descriptor component-id (+ attachment-count idx) file 0])
                                     files)
          db            (as-> db db'
                              (update-in db' [:application :answers (keyword component-id) :values]
                                (fn [values]
                                  (or values [])))
                              (->> files
                                   (map-indexed (fn attachment-idx->file [idx file]
                                                  {:idx (+ attachment-count idx) :file file}))
                                   (reduce (fn attachment-spec->db [db {:keys [idx file]}]
                                             (assoc-in db [:application :answers (keyword component-id) :values idx]
                                               {:value  {:filename     (.-name file)
                                                         :content-type (.-type file)
                                                         :size         (.-size file)}
                                                :valid  false
                                                :status :uploading}))
                                           db'))
                              (assoc-in db' [:application :answers (keyword component-id) :valid] false)
                              (assoc-in db' [:application :answers (keyword component-id) :too-big] false))]
      {:db         db
       :dispatch-n dispatch-list})))

(defn- update-attachment-answer-validity [db field-descriptor component-id]
  (update-in db [:application :answers (keyword component-id)]
             (fn [{:keys [values] :as component}]
               (let [validators (:validators field-descriptor)
                     validated? (every? true? (map #(validator/validate % values (-> db :application :answers) nil) validators))]
                 (assoc component
                   :valid
                   (and validated?
                        (every? (comp true? :valid) values)))))))

(reg-event-db
  :application/handle-attachment-upload
  (fn [db [_ field-descriptor component-id attachment-idx response]]
    (-> db
        (update-in [:application :answers (keyword component-id) :values attachment-idx] merge
                   {:value response :valid true :status :ready})
        (update-attachment-answer-validity field-descriptor component-id))))

(defn- rate-limit-error? [response]
  (= (:status response) 429))

(reg-event-fx
  :application/handle-attachment-upload-error
  (fn [{:keys [db]} [_ field-descriptor component-id attachment-idx filename file retries response]]
    (let [rate-limited? (rate-limit-error? response)
          current-error (if rate-limited?
                          {:fi "Tiedostoa ei ladattu, yritä uudelleen"
                           :en "File failed to upload, try again"
                           :sv "Fil inte laddat, försök igen"}
                          {:fi "Kielletty tiedostomuoto"
                           :en "File type forbidden"
                           :sv "Förbjudet filformat"})]
      (if (and rate-limited? (< retries 3))
        {:db db
         :delayed-dispatch {:dispatch-vec [:application/add-single-attachment field-descriptor component-id attachment-idx file retries]
                            :timeout (+ 2000 (rand-int 2000))}}
        {:db (-> db
                 (update-in [:application :answers (keyword component-id) :values attachment-idx] merge
                            {:value {:filename filename} :valid false :status :error :error current-error})
                 (update-attachment-answer-validity field-descriptor component-id))}))))

(reg-event-db
  :application/handle-attachment-delete
  (fn [db [_ field-descriptor component-id attachment-key _]]
    (-> db
        (update-in [:application :answers (keyword component-id) :values]
                   (comp vec
                         (partial remove (comp (partial = attachment-key) :key :value))))
        (update-attachment-answer-validity field-descriptor component-id))))

(reg-event-fx
  :application/remove-attachment
  (fn [{:keys [db]} [_ field-descriptor component-id attachment-idx]]
    (let [key (get-in db [:application :answers (keyword component-id) :values attachment-idx :value :key])
          db  (-> db
                  (assoc-in [:application :answers (keyword component-id) :valid] false)
                  (update-in [:application :answers (keyword component-id) :values attachment-idx] merge
                    {:status :deleting
                     :valid  false}))
          db-and-fx {:db db}]
      (if (get-in db [:application :editing?])
        (assoc db-and-fx :dispatch [:application/handle-attachment-delete field-descriptor component-id key])
        (assoc db-and-fx :http {:method  :delete
                                :url     (str "/hakemus/api/files/" key)
                                :handler [:application/handle-attachment-delete field-descriptor component-id key]})))))

(reg-event-db
  :application/remove-attachment-error
  (fn [db [_ field-descriptor component-id attachment-idx]]
    (-> db
        (update-in [:application :answers (keyword component-id) :values] autil/remove-nth attachment-idx)
        (update-attachment-answer-validity field-descriptor component-id))))

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
    (let [new-db    (assoc-in db [:application :feedback :status] :feedback-submitted)
          feedback  (-> db :application :feedback)
          text      (:text feedback)
          post-data {:form-key   (-> db :form :key)
                     :form-id    (-> db :form :id)
                     :form-name  (-> db :form :name)
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

(defn- hakukohteet-field [db]
  (->> (get-in db [:form :content] [])
       (filter #(= "hakukohteet" (:id %)))
       first))

(reg-event-db
  :application/hakukohde-search-toggle
  (fn [db _]
    (update-in db [:application :show-hakukohde-search] not)))

(reg-event-db
  :application/hakukohde-query-process
  (fn [db [_ hakukohde-query]]
    (if (and (= hakukohde-query (get-in db [:application :hakukohde-query]))
             (< 1 (count hakukohde-query)))
      (let [hakukohde-options (:options (hakukohteet-field db))
            pattern (re-pattern (str "(?i)" hakukohde-query))]
        (assoc-in db [:application :hakukohde-hits]
                  (->> hakukohde-options
                       (filter #(re-find pattern (get-in % [:label :fi] "")))
                       (map :value))))
      db)))

(reg-event-fx
  :application/hakukohde-query-change
  (fn [{db :db} [_ hakukohde-query]]
    {:db (-> db
             (assoc-in [:application :hakukohde-query] hakukohde-query)
             (assoc-in [:application :hakukohde-hits] []))
     :dispatch-later [{:ms 1000
                       :dispatch [:application/hakukohde-query-process
                                  hakukohde-query]}]}))

(reg-event-db
  :application/hakukohde-query-clear
  (fn [db _]
    (-> db
        (assoc-in [:application :hakukohde-query] "")
        (assoc-in [:application :hakukohde-hits] []))))

(reg-event-db
  :application/hakukohde-add-selection
  (fn [db [_ hakukohde-oid]]
    (let [selected-hakukohteet (get-in db [:application :answers :hakukohteet :values] [])
          new-hakukohde-values (conj selected-hakukohteet {:valid true :value hakukohde-oid})]
      (-> db
          (assoc-in [:application :answers :hakukohteet :values]
                     new-hakukohde-values)
          (assoc-in [:application :answers :hakukohteet :valid]
                    (validator/validate :hakukohteet new-hakukohde-values nil (hakukohteet-field db)))))))

(reg-event-db
  :application/hakukohde-remove-selection
  (fn [db [_ hakukohde-oid]]
    (let [selected-hakukohteet (get-in db [:application :answers :hakukohteet :values] [])
          new-hakukohde-values (remove #(= hakukohde-oid (:value %)) selected-hakukohteet)]
      (-> db
          (assoc-in [:application :answers :hakukohteet :values]
                    new-hakukohde-values)
          (assoc-in [:application :answers :hakukohteet :valid]
                    (validator/validate :hakukohteet new-hakukohde-values nil (hakukohteet-field db)))))))
