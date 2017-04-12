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
                                                 hakukohde
                                                 hakukohde-name]}]]
  {:db       (-> db
                 (assoc-in [:application :editing?] true)
                 (assoc-in [:application :secret] secret)
                 (assoc-in [:form :selected-language] (keyword lang))
                 (assoc-in [:form :hakukohde-name] hakukohde-name))
   :dispatch (if hakukohde
               [:application/get-latest-form-by-hakukohde hakukohde answers]
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
  {:db   db
   :http {:method  :get
          :url     (str "/hakemus/api/hakukohde/" hakukohde-oid)
          :handler [:application/handle-form answers]}})

(reg-event-fx
  :application/get-latest-form-by-hakukohde
  get-latest-form-by-hakukohde)

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
                    (map first))
        valid  (if (not-empty validators)
                 (every? true? (map #(validator/validate % value) validators))
                 true)]
    (merge answer {:value value :valid valid})))

(defn- select-single-choice-button [db [_ button-id value validators]]
  (let [button-path   [:application :answers button-id]
        current-value (:value (get-in db button-path))
        new-value     (when (not= value current-value) value)]
    (update-in db button-path
               (fn [answer]
                 (let [valid? (if (not-empty validators)
                                (every? true? (map #(validator/validate % new-value) validators))
                                true)]
                   (merge answer {:value new-value
                                  :valid valid?}))))))

(defn- toggle-values
  [answer options]
  (reduce (fn [answer option-value]
            (toggle-multiple-choice-option answer option-value nil))
          answer
          options))

(defn- merge-multiple-choice-option-values [value answer]
  (if (string? value)
    (toggle-values answer (clojure.string/split value #"\s*,\s*"))
    (toggle-values answer value)))

(defn- set-ssn-field-visibility [db]
  (rules/run-rule {:toggle-ssn-based-fields-for-existing-application "ssn"} db))

(defonce multi-value-field-types #{"textField" "attachment"})

(defn- supports-multiple-values [field-type]
  (contains? multi-value-field-types field-type))

(defn- merge-submitted-answers [db [_ submitted-answers]]
  (-> db
      (update-in [:application :answers]
        (fn [answers]
          (reduce (fn [answers {:keys [key value cannot-edit] :as answer}]
                    (let [answer-key (keyword key)
                          value      (cond-> value
                                             (and (vector? value)
                                                  (not (supports-multiple-values (:fieldType answer))))
                                             (first))]
                      (if (contains? answers answer-key)
                        (update
                          (match answer
                                 {:fieldType "multipleChoice"}
                                 (update answers answer-key (partial merge-multiple-choice-option-values value))

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
                          answer-key merge {:cannot-edit cannot-edit})
                        answers)))
                  answers
                  submitted-answers)))
      (set-ssn-field-visibility)))

(reg-event-db
  :application/merge-submitted-answers
  merge-submitted-answers)

(defn handle-form [{:keys [db]} [_ answers form]]
  (let [form (-> (languages->kwd form)
                 (set-form-language))
        db   (-> db
                 (update :form (fn [{:keys [selected-language hakukohde-name]}]
                                 (cond-> form
                                   (some? selected-language)
                                   (assoc :selected-language selected-language)

                                   (some? hakukohde-name)
                                   (assoc :hakukohde-name hakukohde-name))))
                 (assoc-in [:application :answers] (create-initial-answers form))
                 (assoc :wrapper-sections (extract-wrapper-sections form)))]
    {:db               db
     ;; Previously submitted answers must currently be merged to the app db
     ;; after a delay or rules will ruin them and the application will not
     ;; look completely as valid (eg. SSN field will be blank)
     :dispatch-later [{:ms 200 :dispatch [:application/merge-submitted-answers answers]}]
     :dispatch [:application/set-followup-visibility-to-false]}))

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
    (assoc-in db path
      (when values
        (merge current-answer-data values)))))

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
    (cond-> db
            (get-in db [:application :answers key :values])
            (update-in [:application :answers key :values]
                       #(autil/remove-nth % idx))

            ; when creating application, we have the value below (and it's important). when editing, we do not.
            ; consider this a temporary, terrible bandaid solution
            (get-in db [:application :answers key :value])
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

(reg-event-fx
  :state-update-fx
  (fn [cofx [_ f]]
    (or (f cofx)
        (dissoc cofx :event))))

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

(reg-event-db
  :application/select-single-choice-button
  select-single-choice-button)

(reg-event-db
  :application/set-followup-visibility-to-false
  (fn [db _]
    (assoc-in db [:application :ui]
      (->> (autil/flatten-form-fields (:content (:form db)))
        (filter :followup?)
        (map (fn [field] {(keyword (:id field))
                          ; prevent hiding followups with children
                          {:visible? (not (empty? (:children field)))}}))
        (reduce merge)))))

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
                     validated? (every? true? (map #(validator/validate % values) validators))]
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
                     :valid  false}))]
      {:db   db
       :http {:method  :delete
              :url     (str "/hakemus/api/files/" key)
              :handler [:application/handle-attachment-delete field-descriptor component-id key]}})))

(reg-event-db
  :application/remove-attachment-error
  (fn [db [_ field-descriptor component-id attachment-idx]]
    (-> db
        (update-in [:application :answers (keyword component-id) :values] autil/remove-nth attachment-idx)
        (update-attachment-answer-validity field-descriptor component-id))))
