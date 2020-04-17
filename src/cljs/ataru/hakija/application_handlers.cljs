(ns ataru.hakija.application-handlers
  (:require [re-frame.core :refer [reg-event-db reg-fx reg-event-fx dispatch subscribe]]
            [ataru.component-data.higher-education-base-education-module :as hebem]
            [ataru.hakija.application-validators :as validator]
            [ataru.cljs-util :as util]
            [ataru.util :as autil]
            [ataru.hakija.person-info-fields :as person-info-fields]
            [ataru.hakija.rules :as rules]
            [ataru.hakija.resumable-upload :as resumable-upload]
            [ataru.hakija.try-selection :refer [try-selection]]
            [ataru.translations.translation-util :as translations]
            [cljs.core.match :refer-macros [match]]
            [ataru.hakija.application :refer [create-initial-answers
                                              create-application-to-submit
                                              extract-wrapper-sections
                                              db->valid-status]]
            [clojure.data :as d]
            [ataru.component-data.value-transformers :as value-transformers]
            [cljs-time.core :as c]
            [cljs-time.format :as f]
            [cljs-time.coerce :refer [from-long to-long]]))

(defn initialize-db [_ _]
  {:form        nil
   :application {:attachments-id (random-uuid)
                 :answers        {}}})

(defn- required? [field-descriptor]
  (some (partial = "required")
        (:validators field-descriptor)))

(reg-event-db
  :application/set-secret-delivery-status
  (fn [db [_ status]]
    (assoc-in db [:application :secret-delivery-status] status)))

(reg-event-fx
  :application/handle-get-application-by-hakija-secret-error
  (fn [{:keys [db]} [_ old-secret response]]
    (if (and (= (:status response) 401)
             (= "secret-expired" (get-in response [:body :code])))
      {:db (-> db
               (assoc-in [:form :selected-language] (or (keyword (get-in response [:body :lang])) :fi))
               (assoc-in [:application :secret-expired?] true)
               (assoc-in [:application :old-secret] old-secret))}
      {:db       db
       :dispatch [:application/default-handle-error response]})))

(reg-event-db
  :application/handle-send-new-secret
  (fn [db _]
    (assoc-in db [:application :secret-delivery-status] :completed)))

(reg-event-fx
  :application/send-new-secret
  (fn [{:keys [db]}]
    (let [old-secret (get-in db [:application :old-secret])]
      {:db       db
       :dispatch [:application/set-secret-delivery-status :ongoing]
       :http     {:method    :post
                  :post-data {:old-secret old-secret}
                  :url       "/hakemus/api/send-application-secret"
                  :handler   [:application/handle-send-new-secret]}})))

(defn- get-application-by-hakija-secret
  [{:keys [db]} [_ secret]]
  {:db   db
   :http {:method        :get
          :url           (str "/hakemus/api/application?secret=" secret)
          :error-handler [:application/handle-get-application-by-hakija-secret-error secret]
          :handler       [:application/handle-get-application {:secret secret}]}})

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
  (fn [{:keys [db]} [_ form-key virkailija-secret]]
    {:db   (cond-> db
                   (some? virkailija-secret)
                   (assoc-in [:application :virkailija-secret] virkailija-secret))
     :http {:method  :get
            :url     (str "/hakemus/api/form/"
                          form-key
                          (if (some? virkailija-secret)
                            "?role=virkailija"
                            "?role=hakija"))
            :handler [:application/handle-form]}}))

(reg-event-fx
  :application/get-latest-form-by-hakukohde
  (fn [{:keys [db]} [_ hakukohde-oid virkailija-secret]]
    {:db   (cond-> (assoc-in db [:application :preselected-hakukohde-oids] [hakukohde-oid])
                   (some? virkailija-secret)
                   (assoc-in [:application :virkailija-secret] virkailija-secret))
     :http {:method  :get
            :url     (str "/hakemus/api/hakukohde/"
                          hakukohde-oid
                          (if (some? virkailija-secret)
                            "?role=virkailija"
                            "?role=hakija"))
            :handler [:application/handle-form]}}))

(reg-event-fx
  :application/get-latest-form-by-haku
  (fn [{:keys [db]} [_ haku-oid hakukohde-oids virkailija-secret]]
    {:db   (cond-> (assoc-in db [:application :preselected-hakukohde-oids] hakukohde-oids)
                   (some? virkailija-secret)
                   (assoc-in [:application :virkailija-secret] virkailija-secret))
     :http {:method  :get
            :url     (str "/hakemus/api/haku/"
                          haku-oid
                          (if (some? virkailija-secret)
                            "?role=virkailija"
                            "?role=hakija"))
            :handler [:application/handle-form]}}))

(defn handle-submit [db _]
  (assoc-in db [:application :submit-status] :submitted))

(defn send-application [db method]
  (when-not (-> db :application :submit-status)
    {:db   (-> db (assoc-in [:application :submit-status] :submitting) (dissoc :error))
     :http {:method        method
            :url           "/hakemus/api/application"
            :post-data     (create-application-to-submit (:application db) (:form db) (get-in db [:form :selected-language]))
            :handler       [:application/handle-submit-response]
            :error-handler [:application/handle-submit-error]}}))

(reg-event-db
  :application/handle-submit-response
  handle-submit)

(defn response->error-message [db response]
  (assoc db :error {:code    (keyword (get-in response [:body :code] "internal-server-error"))
                    :message "Tapahtui virhe"
                    :detail  (str response)}))

(reg-event-fx
  :application/handle-submit-error
  (fn [cofx [_ response]]
    {:db (-> (update (:db cofx) :application dissoc :submit-status)
             (response->error-message response))}))

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

(defn- ylioppilastutkinto? [db]
  (boolean (some #(or (= "pohjakoulutus_yo" %)
                      (= "pohjakoulutus_yo_ammatillinen" %)
                      (= "pohjakoulutus_yo_kansainvalinen_suomessa" %)
                      (= "pohjakoulutus_yo_ulkomainen" %))
                 (get-in db [:application :answers :higher-completed-base-education :value]))))

(defn- selected-hakukohteet [db]
  (map :value (get-in db [:application :answers :hakukohteet :values] [])))

(defn selected-hakukohteet-and-ryhmat [db]
  (let [selected-hakukohteet                   (set (selected-hakukohteet db))
        selected-hakukohteet-tarjonta          (when (not-empty selected-hakukohteet)
                                                 (filter #(contains? selected-hakukohteet (:oid %))
                                                         (get-in db [:form :tarjonta :hakukohteet])))
        selected-hakukohderyhmat               (set (mapcat :hakukohderyhmat selected-hakukohteet-tarjonta))
        selected-ei-jyemp-hakukohteet-tarjonta (set (remove :jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja?
                                                            selected-hakukohteet-tarjonta))
        selected-ei-jyemp-hakukohderyhmat      (set (mapcat :hakukohderyhmat selected-ei-jyemp-hakukohteet-tarjonta))
        selected-ei-jyemp-hakukohteet          (set (map :oid selected-ei-jyemp-hakukohteet-tarjonta))]
    [(clojure.set/union selected-hakukohteet selected-hakukohderyhmat)
     (clojure.set/union selected-ei-jyemp-hakukohteet selected-ei-jyemp-hakukohderyhmat)]))

(declare set-field-visibility)

(defn- set-followups-visibility
  [db field-descriptor visible? ylioppilastutkinto? hakukohteet-and-ryhmat option-selected?]
  (reduce (fn [db option]
            (let [selected? (option-selected? option)]
              (reduce #(set-field-visibility %1 %2 (and visible? selected?) ylioppilastutkinto? hakukohteet-and-ryhmat)
                      db
                      (:followups option))))
          db
          (:options field-descriptor)))

(defn- set-single-choice-followups-visibility
  [db field-descriptor visible? ylioppilastutkinto? hakukohteet-and-ryhmat]
  (let [value (get-in db [:application :answers (keyword (:id field-descriptor)) :value])]
    (set-followups-visibility db field-descriptor visible? ylioppilastutkinto? hakukohteet-and-ryhmat  #(= value (:value %)))))

(defn- set-multi-choice-followups-visibility
  [db field-descriptor visible? ylioppilastutkinto? hakukohteet-and-ryhmat]
  (let [values (get-in db [:application :answers (keyword (:id field-descriptor)) :value])]
    (set-followups-visibility db field-descriptor visible? ylioppilastutkinto? hakukohteet-and-ryhmat (fn [option]
                                                                                                        (let [value (:value option)]
                                                                                                          (some #(= value %) values))))))


(defn- set-option-visibility [db [index option] visible? id selected-hakukohteet-and-ryhmat]
  (let [belongs-to (set (concat (:belongs-to-hakukohderyhma option)
                                (:belongs-to-hakukohteet option)))]
    (assoc-in db [:application :ui id index :visible?]
              (and visible?
                   (or (empty? belongs-to)
                       (not-empty (clojure.set/intersection
                                   belongs-to
                                   selected-hakukohteet-and-ryhmat)))))))

(defn- set-field-visibility
  ([db field-descriptor]
   (set-field-visibility
    db
    field-descriptor
    true
    (ylioppilastutkinto? db)
    (selected-hakukohteet-and-ryhmat db)))
  ([db
    field-descriptor
    visible?
    ylioppilastutkinto?
    [selected-hakukohteet-and-ryhmat selected-ei-jyemp-hakukohteet-and-ryhmat]]
   (let [hakukohteet-and-ryhmat [selected-hakukohteet-and-ryhmat selected-ei-jyemp-hakukohteet-and-ryhmat]
         id                     (keyword (:id field-descriptor))
         belongs-to             (set (concat (:belongs-to-hakukohderyhma field-descriptor)
                                             (:belongs-to-hakukohteet field-descriptor)))
         excluded-attachment-ids-when-yo-and-jyemp (-> db :application :excluded-attachment-ids-when-yo-and-jyemp)
         jyemp?                 (and ylioppilastutkinto?
                                     (contains? excluded-attachment-ids-when-yo-and-jyemp (:id field-descriptor)))
         visible?               (and (not (get-in field-descriptor [:params :hidden] false))
                                     visible?
                                     (or (not jyemp?) (not-empty selected-ei-jyemp-hakukohteet-and-ryhmat))
                                     (or (empty? belongs-to)
                                         (not-empty (clojure.set/intersection
                                                     belongs-to
                                                     (if jyemp?
                                                       selected-ei-jyemp-hakukohteet-and-ryhmat
                                                       selected-hakukohteet-and-ryhmat)))))
         child-visibility       (fn [db]
                                  (reduce #(set-field-visibility %1 %2 visible? ylioppilastutkinto? hakukohteet-and-ryhmat)
                                          db
                                          (:children field-descriptor)))
         option-visibility      (fn [db]
                                  (reduce #(set-option-visibility %1 %2 visible? id selected-hakukohteet-and-ryhmat)
                                          db
                                          (map-indexed vector (:options field-descriptor))))]
     (cond-> (-> (assoc-in db [:application :ui id :visible?] visible?)
                 (child-visibility)
                 (option-visibility))
             (or (= "dropdown" (:fieldType field-descriptor))
                 (= "singleChoice" (:fieldType field-descriptor)))
             (set-single-choice-followups-visibility field-descriptor visible? ylioppilastutkinto? hakukohteet-and-ryhmat)
             (= "multipleChoice" (:fieldType field-descriptor))
             (set-multi-choice-followups-visibility field-descriptor visible? ylioppilastutkinto? hakukohteet-and-ryhmat)))))

(defn set-field-visibilities
  [db]
  (rules/run-all-rules
   (reduce set-field-visibility db (get-in db [:form :content]))
   (:flat-form-content db)))

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
  (let [toggle (fn [value]
                 (let [[before after] (split-with #(not= option-value %) value)]
                   (vec
                    (if (empty? after)
                      (cons option-value before)
                      (concat before (rest after))))))]
    (if (some? question-group-idx)
      (update-in answer [:value question-group-idx] toggle)
      (update answer :value toggle))))

(defn- merge-multiple-choice-option-values [value answer]
  (if (string? value)
    (assoc answer :value (clojure.string/split value #"\s*,\s*"))
    (assoc answer :value value)))

(defonce multi-value-field-types #{"multipleChoice" "singleChoice" "textField" "attachment" "hakukohteet" "dropdown" "textArea"})

(defn- supports-multiple-values [field-type]
  (contains? multi-value-field-types field-type))

(defn application-run-rules [db rule]
  (if (not-empty rule)
    (rules/run-rules db rule)
    (rules/run-all-rules db (:flat-form-content db))))

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
  (reduce (fn [db field-descriptor]
            (let [id                     (keyword (:id field-descriptor))
                  {:keys [value values]} (if (and (:cannot-edit field-descriptor)
                                                  (contains? person-info-fields/editing-forbidden-person-info-field-ids id))
                                           {:value (get-in db [:application :person id])}
                                           (get-in db [:application :answers id]))
                  question-group-id      (-> field-descriptor :params :question-group-id)]
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
          (:flat-form-content db)))

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
  (cond-> (merge answer {:valid true :value value})
          (and (vector? value) (every? vector? value))
          (merge {:values (mapv (partial mapv (fn [value] {:valid true :value value}))
                                value)})))

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

                                                     {:fieldType "attachment"}
                                                     (update answers answer-key merge
                                                             {:values (mapv (fn self [value]
                                                                              (if (vector? value)
                                                                                (mapv self value)
                                                                                {:value    (:key value)
                                                                                 :filename (:filename value)
                                                                                 :size     (:size value)
                                                                                 :status   :ready
                                                                                 :valid    true}))
                                                                            (:value answer))
                                                              :valid  true})

                                                     {:fieldType (field-type :guard supports-multiple-values) :value (_ :guard vector?)}
                                                     (update answers answer-key merge
                                                             {:values (mapv (fn self [value]
                                                                              (if (vector? value)
                                                                                (mapv self value)
                                                                                {:value value
                                                                                 :valid true}))
                                                                            (:value answer))
                                                              :valid  true})

                                                     {:key "email"}
                                                     (update answers answer-key merge {:valid true :value value :verify value})

                                                     :else
                                                     (update answers answer-key merge {:valid true :value value}))]
                                   (assoc-in answer [answer-key :cannot-view] cannot-view))
                                 answers)))
                           answers
                           submitted-answers)))
      (populate-hakukohde-answers-if-necessary)
      (set-have-finnish-ssn)
      (application-run-rules [])
      (set-question-group-row-amounts)))

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

(defn- handle-form [db answers server-date form]
  (let [form                       (-> (languages->kwd form)
                                       (set-form-language)
                                       (update :content (partial map set-question-group-id))
                                       (assoc :hakuaika-end (->> form :tarjonta :hakukohteet
                                                                 (map :hakuaika)
                                                                 (filter :on)
                                                                 (sort-by :end >)
                                                                 first
                                                                 :end))
                                       (assoc :time-delta-from-server
                                              (if (some? server-date)
                                                (- (->> (clojure.string/replace server-date " GMT" "")
                                                        (f/parse (f/formatter "EEE, dd MMM yyyy HH:mm:ss"))
                                                        to-long)
                                                   (.getTime (js/Date.)))
                                                0)))
        valid-hakukohde-oids       (set (->> form :tarjonta :hakukohteet
                                             (filter #(get-in % [:hakuaika :on]))
                                             (map :oid)))
        preselected-hakukohde-oids (->> db :application :preselected-hakukohde-oids
                                        (filter #(contains? valid-hakukohde-oids %)))
        flat-form-content          (autil/flatten-form-fields (:content form))
        excluded-attachment-ids-when-yo-and-jyemp (hebem/non-yo-attachment-ids form)
        initial-answers            (create-initial-answers flat-form-content preselected-hakukohde-oids)]
    (-> db
        (update :form (fn [{:keys [selected-language]}]
                        (cond-> form
                                (some? selected-language)
                                (assoc :selected-language selected-language))))
        (assoc :flat-form-content flat-form-content)
        (assoc-in [:application :excluded-attachment-ids-when-yo-and-jyemp] excluded-attachment-ids-when-yo-and-jyemp)
        (assoc-in [:application :answers] initial-answers)
        (assoc-in [:application :show-hakukohde-search] false)
        (assoc-in [:application :validators-processing] #{})
        (assoc :wrapper-sections (extract-wrapper-sections form))
        (merge-submitted-answers answers)
        (original-values->answers)
        (set-field-visibilities))))

(defn- selection-limits [{:keys [flat-form-content]}]
  (let [limited? (fn [{:keys [validators]}]
                   (some #(= "selection-limit" %) validators))]
    (->> flat-form-content
         (filter limited?)
         (map :id)
         (set)
         (not-empty))))

(defn set-limit-reached [db {:keys [selection-id limit-reached] :as selection}]
  (if (nil? selection)
    db
    (let [new-limits (group-by :question-id limit-reached)]
      (reduce (fn [db question-id]
                (if-let [answers (new-limits (name question-id))]
                  (assoc-in db [:application :answers question-id :limit-reached] (set (map :answer-id answers)))
                  (assoc-in db [:application :answers question-id :limit-reached] nil)))
        (if selection-id
          (assoc-in db [:application :selection-id] selection-id)
          db)
        (map keyword (:selection-limited db))))))

(defn reset-other-selections [db question-id answer-id]
  (reduce (fn [db key]
            (if (= key question-id)
              db
              (assoc-in db [:application :answers key :value] nil)))
      db (map keyword (:selection-limited db))))

(reg-event-fx
  :application/handle-update-selection-limits
  (fn [{:keys [db]} [_ selection valid? question-id answer-id]]
    {:db (cond (false? valid?)
               (-> db
                   (set-limit-reached selection))

               (not (and valid? question-id answer-id))
               (set-limit-reached db selection)

               (and question-id answer-id)
               (-> db
                   (set-limit-reached selection)
                   (reset-other-selections question-id answer-id)))}))

(reg-event-db
  :application/handle-selection-limit
  (fn [db [_ response]]
    (set-limit-reached db (:body response))))

(reg-event-fx
  :application/post-handle-form-dispatches
  (fn [{:keys [db]} _]
    (let [selection-limited (selection-limits db)]
      (merge
        {:db         (assoc db :selection-limited selection-limited)
         :dispatch-n [[:application/hide-hakukohteet-if-no-tarjonta]
                      [:application/hakukohde-query-change (atom "")]
                      [:application/set-page-title]
                      [:application/update-answers-validity]
                      [:application/validate-hakukohteet]]}
        (when selection-limited
          {:http {:method  :put
                  :url     (str "/hakemus/api/selection-limit?form-key=" (-> db :form :key))
                  :handler [:application/handle-selection-limit]}})))))

(defn- handle-get-application [{:keys [db]}
                               [_
                                {:keys [secret virkailija-secret]}
                                response]]
  (let [{:keys [application person form]} (:body response)
        [secret-kwd secret-val]           (if-not (clojure.string/blank? secret)
                                            [:secret secret]
                                            [:virkailija-secret virkailija-secret])]
    (util/set-query-param "application-key" (:key application))
    {:db       (-> db
                   (assoc-in [:application :application-identifier] (:application-identifier application))
                   (assoc-in [:application :editing?] true)
                   (assoc-in [:application secret-kwd] secret-val)
                   (assoc-in [:application :state] (:state application))
                   (assoc-in [:application :hakukohde] (:hakukohde application))
                   (assoc-in [:application :person] person)
                   (assoc-in [:application :cannot-edit-because-in-processing] (:cannot-edit-because-in-processing application))
                   (assoc-in [:form :selected-language] (or (keyword (:lang application)) :fi))
                   (handle-form (:answers application) (get-in response [:headers "date"]) form))
     :dispatch [:application/post-handle-form-dispatches]}))

(reg-event-fx
  :application/handle-get-application
  handle-get-application)

(reg-event-db
  :flasher
  (fn [db [_ flash]]
    (assoc db :flasher flash)))

(reg-event-fx
  :application/handle-form
  (fn [{:keys [db]} [_ response]]
    {:db       (handle-form db nil (get-in response [:headers "date"]) (:body response))
     :dispatch [:application/post-handle-form-dispatches]}))

(reg-event-db
  :application/network-online
  (fn [db [_ flash]]
    (if (= :network-offline (get-in db [:error :code]))
      (dissoc db :error)
      db)))

(reg-event-db
  :application/handle-selection-over-network-uncertain
  (fn [db [_ uncertain?]]
    (if uncertain?
      (assoc-in db [:application :selection-over-network-uncertain?] true)
      (update db :application dissoc :selection-over-network-uncertain?))))

(reg-event-db
  :application/network-offline
  (fn [db [_ flash]]
    (if (get db :error)
      db
      (assoc-in db [:error :code] :network-offline))))

(reg-event-db
  :application/initialize-db
  initialize-db)

(reg-event-fx
  :application/textual-field-blur
  (fn [{db :db} [_ field value idx]]
    (let [id          (keyword (:id field))
          answer      (get-in db [:application :answers id])
          skip-rules? (or (empty? (:blur-rules field))
                          (and
                           (-> answer :valid not)
                           (not (contains? (-> db :application :validators-processing) id))))
          plain-value (or value "")
          value       (clojure.string/trim plain-value)]
      (merge
        (when (string? (:value answer))
          {:db (assoc-in db [:application :answers id :value] value)})
        {:dispatch-n (vec (concat
                           (when-not skip-rules?
                             [[:application/run-rules (:blur-rules field)]])
                           (when (not= plain-value value)
                             (if idx
                               [[:application/set-repeatable-application-field field value 0 idx]]
                               [[:application/set-application-field field value]]))))}))))

(defn set-validator-processing
  [db id]
  (update-in db [:application :validators-processing] conj id))

(reg-event-db
  :application/update-answers-validity
  (fn [db _]
    (assoc-in db [:application :answers-validity] (db->valid-status db))))

(reg-event-fx
  :application/set-validator-processed
  (fn [{:keys [db]} [_ id]]
    {:db       (update-in db [:application :validators-processing] disj id)
     :dispatch [:application/update-answers-validity]}))

(defn- transform-value [value field-descriptor]
  (let [t (case (:id field-descriptor)
            "birth-date" value-transformers/birth-date
            identity)]
    (or (t value) value)))

(reg-event-fx
  :application/set-email-verify-field
  (fn [{:keys [db]} [_ field-descriptor value verify-value]]
    (let [id     (keyword (:id field-descriptor))
          new-db (-> db
                     (assoc-in [:application :answers id :verify] verify-value)
                     (set-validator-processing id)
                     (set-multi-value-changed id :value))]
      {:db new-db
       :dispatch [:application/set-application-field field-descriptor value]})))

(reg-event-fx
  :application/set-application-field
  (fn [{db :db} [_ field value]]
    (let [value  (transform-value value field)
          id     (keyword (:id field))
          new-db (-> db
                     (assoc-in [:application :answers id :value] value)
                     (set-validator-processing id)
                     (set-multi-value-changed id :value)
                     (set-field-visibility field))]
      {:db                 new-db
       :validate-debounced {:value                        value
                            :priorisoivat-hakukohderyhmat (get-in new-db [:form :priorisoivat-hakukohderyhmat])
                            :answers-by-key               (get-in new-db [:application :answers])
                            :field-descriptor             field
                            :editing?                     (get-in new-db [:application :editing?])
                            :virkailija?                  (contains? (:application new-db) :virkailija-secret)
                            :on-validated                 (fn [[valid? errors]]
                                                            (dispatch [:application/set-application-field-valid
                                                                       field valid? errors]))}})))

(defn- set-repeatable-field-values
  [db id group-idx data-idx value]
  (if (some? group-idx)
    (-> db
        (update-in [:application :answers id :values] (util/vector-of-length (inc group-idx)))
        (update-in [:application :answers id :values group-idx] (util/vector-of-length (inc data-idx)))
        (assoc-in [:application :answers id :values group-idx data-idx :value] value)
        (update-in [:application :answers id :values group-idx data-idx :valid] (fnil identity true)))
    (-> db
        (update-in [:application :answers id :values] (util/vector-of-length (inc data-idx)))
        (assoc-in [:application :answers id :values data-idx :value] value)
        (update-in [:application :answers id :values data-idx :valid] (fnil identity true)))))

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
  (if (some? group-idx)
    (-> db
        (update-in [:application :answers id :values] (util/vector-of-length (inc group-idx)))
        (update-in [:application :answers id :values group-idx] (util/vector-of-length (inc data-idx)))
        (assoc-in [:application :answers id :values group-idx data-idx :valid] valid?))
    (-> db
        (update-in [:application :answers id :values] (util/vector-of-length (inc data-idx)))
        (assoc-in [:application :answers id :values data-idx :valid] valid?))))

(defn- set-repeatable-application-field-top-level-valid
  [db id group-idx required? valid?]
  (let [values               (get-in db [:application :answers id :values])
        multi-value-answers? (some? group-idx)
        is-empty?            (if multi-value-answers?
                               (some empty? values)
                               (empty? values))
        all-valid?           (and (every? :valid (flatten values)) valid?)]
    (assoc-in db [:application :answers id :valid] (if is-empty?
                                                     (not required?)
                                                     all-valid?))))

(reg-event-fx
  :application/set-application-field-valid
  (fn [{db :db} [_ field-descriptor valid? errors]]
    (let [id (keyword (:id field-descriptor))
          rules (:rules field-descriptor)]
      (cond-> {:db (-> db
                       (assoc-in [:application :answers id :valid] valid?)
                       (assoc-in [:application :answers id :errors] errors))
               :dispatch-n [[:application/update-answers-validity]
                            [:application/set-validator-processed id]]}
              (not (empty? rules))
              (update :dispatch-n conj [:application/run-rules rules])))))

(reg-event-fx
  :application/set-repeatable-application-field-valid
  (fn [{:keys [db]} [_ field-descriptor group-idx data-idx required? valid?]]
    (let [id    (keyword (:id field-descriptor))
          rules (:rules field-descriptor)]
      (cond-> {:db         (-> db
                               (set-repeatable-application-repeated-field-valid id group-idx data-idx valid?)
                               (set-repeatable-application-field-top-level-valid id group-idx required? valid?))
               :dispatch-n [[:application/update-answers-validity]
                            [:application/set-validator-processed id]]}
              (not (empty? rules))
              (update :dispatch-n conj [:application/run-rules rules])))))

(reg-event-fx
  :application/set-repeatable-application-field
  (fn [{db :db} [_ field-descriptor value data-idx question-group-idx]]
    (let [id     (keyword (:id field-descriptor))
          new-db (-> db
                     (set-validator-processing id)
                     (set-repeatable-field-values id question-group-idx data-idx value)
                     (set-repeatable-field-value field-descriptor question-group-idx))]
      {:db                 new-db
       :validate-debounced {:value                        value
                            :priorisoivat-hakukohderyhmat (get-in new-db [:form :priorisoivat-hakukohderyhmat])
                            :answers                      (get-in new-db [:application :answers])
                            :field-descriptor             field-descriptor
                            :editing?                     (get-in new-db [:application :editing?])
                            :group-idx                    question-group-idx
                            :field-idx                    data-idx
                            :virkailija?                  (contains? (:application new-db) :virkailija-secret)
                            :on-validated                 (fn [[valid? _]]
                                                            (dispatch [:application/set-repeatable-application-field-valid
                                                                       field-descriptor
                                                                       question-group-idx
                                                                       data-idx
                                                                       (required? field-descriptor)
                                                                       valid?]))}})))

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
            (set-repeatable-field-value field-descriptor question-group-idx)

            true
            (set-repeatable-application-field-top-level-valid id question-group-idx (required? field-descriptor) true))))

(reg-event-db
  :application/remove-repeatable-application-field-value
  (fn [db [_ field-descriptor data-idx question-group-idx]]
    (remove-repeatable-field-value db field-descriptor data-idx question-group-idx)))

(defn default-error-handler [db [_ response]]
  (response->error-message db response))

(reg-event-db
  :application/run-rules
  (fn [db [_ rule]]
    (if (#{:submitting :submitted} (-> db :application :submit-status))
      db
      (application-run-rules db rule))))

(reg-event-db
  :application/default-handle-error
  default-error-handler)

(reg-event-db
 :application/default-http-ok-handler
 (fn [db _] db))

(reg-event-db
  :application/default-http-progress-handler
  (fn [db _] db))

(reg-event-db
  :state-update
  (fn [db [_ f]]
    (or (f db)
        db)))

(reg-event-fx
  :application/handle-postal-code-input
  (fn [{:keys [db]} [_ response]]
    {:db       (update-in db [:application :answers :postal-office]
                          merge {:value (autil/non-blank-val (:body response) [(-> db :form :selected-language) :fi])
                                 :valid true})
     :dispatch [:application/update-answers-validity]}))

(reg-event-fx
  :application/handle-postal-code-error
  (fn [{:keys [db]} _]
    {:db       (-> db
                   (update-in [:application :answers :postal-code]
                              merge {:valid false})
                   (update-in [:application :answers :postal-office]
                              merge {:value "" :valid false}))
     :dispatch [:application/update-answers-validity]}))

(reg-event-fx
  :application/set-multiple-choice-valid
  (fn [{db :db} [_ field-descriptor valid?]]
    (let [rules (:rules field-descriptor)
          id    (keyword (:id field-descriptor))]
      (cond-> {:db         (assoc-in db [:application :answers id :valid] valid?)
               :dispatch-n [[:application/set-validator-processed id]]}
              (not (empty? rules))
              (update :dispatch-n conj [:application/run-rules rules])))))

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
                 (set-validator-processing id)
                 (set-multi-value-changed id :value))]
      (if question-group-idx
        {:db                       db
         :validate-every-debounced {:values                       (get-in db [:application :answers id :value])
                                    :priorisoivat-hakukohderyhmat (get-in db [:form :priorisoivat-hakukohderyhmat])
                                    :answers-by-key               (get-in db [:application :answers])
                                    :field-descriptor             field-descriptor
                                    :editing?                     (get-in db [:application :editing?])
                                    :virkailija?                  (contains? (:application db) :virkailija-secret)
                                    :on-validated                 (fn [[valid? errors]]
                                                                    (dispatch [:application/set-multiple-choice-valid
                                                                               field-descriptor
                                                                               valid?]))}}
        {:db                 (set-field-visibility db field-descriptor)
         :validate-debounced {:value             (get-in db [:application :answers id :value])
                              :priorisoivat-hakukohderyhmat (get-in db [:form :priorisoivat-hakukohderyhmat])
                              :answers-by-key    (get-in db [:application :answers])
                              :field-descriptor  field-descriptor
                              :editing?          (get-in db [:application :editing?])
                              :virkailija?       (contains? (:application db) :virkailija-secret)
                              :on-validated      (fn [[valid? errors]]
                                                   (dispatch [:application/set-multiple-choice-valid
                                                              field-descriptor
                                                              valid?]))}}))))

(reg-event-fx
  :application/select-single-choice-button
  (fn [{db :db} [_ value field-descriptor question-group-idx]]
    (let [id                 (keyword (:id field-descriptor))
          form-key           (get-in db [:form :key])
          selection-id       (get-in db [:application :selection-id])
          selection-group-id (get-in field-descriptor [:params :selection-group-id])]
      {:db                 (-> (if (some? question-group-idx)
                                 (-> db
                                     (set-repeatable-field-values id question-group-idx 0 value)
                                     (set-repeatable-field-value field-descriptor question-group-idx))
                                 (assoc-in db [:application :answers id :value] value))
                               (set-multi-value-changed id :value)
                               (set-field-visibility field-descriptor)
                               (set-validator-processing id))
       :validate-debounced {:value                        value
                            :priorisoivat-hakukohderyhmat (get-in db [:form :priorisoivat-hakukohderyhmat])
                            :answers-by-key               (get-in db [:application :answers])
                            :field-descriptor             field-descriptor
                            :editing?                     (get-in db [:application :editing?])
                            :try-selection                (partial try-selection
                                                                   form-key
                                                                   selection-id
                                                                   selection-group-id)
                            :group-idx                    question-group-idx
                            :field-idx                    0
                            :virkailija?                  (contains? (:application db) :virkailija-secret)
                            :on-validated                 (fn [[valid? errors selection-limit]]
                                                            (when selection-group-id
                                                              (dispatch [:application/handle-selection-over-network-uncertain
                                                                         (and (not valid?) (not-empty errors))])
                                                              (when (-> (first selection-limit) :limit-reached)
                                                                (dispatch [:application/handle-update-selection-limits
                                                                           (first selection-limit) valid? id value])))
                                                            (if (some? question-group-idx)
                                                              (dispatch [:application/set-repeatable-application-field-valid
                                                                         field-descriptor
                                                                         question-group-idx
                                                                         0
                                                                         (required? field-descriptor)
                                                                         valid?])
                                                              (dispatch [:application/set-application-field-valid
                                                                         field-descriptor
                                                                         valid?
                                                                         errors])))}})))

(reg-event-fx
  :application/add-adjacent-fields
  (fn [{db :db} [_ field-descriptor question-group-idx]]
    {:dispatch-n
     (reduce (fn [dispatch child]
               (let [id (keyword (:id child))
                     new-idx (count (if question-group-idx
                                      (get-in db [:application :answers id :values question-group-idx])
                                      (get-in db [:application :answers id :values])))]
                 (conj dispatch [:application/set-repeatable-application-field
                                 child "" new-idx question-group-idx])))
             []
             (:children field-descriptor))}))

(reg-event-fx
  :application/remove-adjacent-field
  (fn [{:keys [db]} [_ field-descriptor row-idx question-group-idx]]
    {:db       (reduce #(remove-repeatable-field-value %1 %2 row-idx question-group-idx)
                       db
                       (:children field-descriptor))
     :dispatch [:application/update-answers-validity]}))

(defonce max-attachment-size-bytes
  (get (js->clj js/config) "attachment-file-max-size-bytes" (* 10 1024 1024)))

(reg-event-fx
  :application/start-attachment-upload
  (fn [{:keys [db]} [_ field-descriptor attachment-idx file retries question-group-idx]]
    (resumable-upload/upload-file
      "/hakemus/api/files/resumable"
      file
      (:id field-descriptor)
      attachment-idx
      (get-in db [:application :attachments-id])
      {:handler          [:application/handle-attachment-upload field-descriptor attachment-idx question-group-idx]
       :error-handler    [:application/handle-attachment-upload-error field-descriptor attachment-idx file (inc retries) question-group-idx]
       :progress-handler [:application-file-upload/handle-attachment-progress-resumable field-descriptor attachment-idx question-group-idx]
       :started-handler  [:application/handle-attachment-upload-started field-descriptor attachment-idx question-group-idx]})))

(reg-event-fx
  :application/add-single-attachment-resumable
  (fn [{:keys [db]} [_ field-descriptor attachment-idx file retries question-group-idx]]
    (let [id       (keyword (:id field-descriptor))
          filename (:filename @(subscribe [:application/answer
                                           id
                                           question-group-idx
                                           attachment-idx]))]
      {:db       (assoc-in db [:attachments-uploading id filename] :downloading)
       :dispatch [:application/start-attachment-upload field-descriptor attachment-idx file retries question-group-idx]})))

(reg-event-fx
  :application/add-attachments
  (fn [{:keys [db]} [_ field-descriptor question-group-idx files]]
    (let [id                   (keyword (:id field-descriptor))
          path                 (cond-> [:application :answers id :values]
                                       (some? question-group-idx)
                                       (conj question-group-idx))
          existing-attachments (get-in db path)
          new-files            (remove (fn [file]
                                         (some #(and (= (.-name file) (:filename %))
                                                     (= (.-size file) (:size %)))
                                               existing-attachments))
                                       files)
          new-attachments      (map (fn [file]
                                      (cond
                                        (< max-attachment-size-bytes (.-size file))
                                        {:value    ""
                                         :filename (.-name file)
                                         :size     (.-size file)
                                         :status   :error
                                         :valid    false
                                         :errors   [[:file-size-info (autil/size-bytes->str max-attachment-size-bytes)]]}

                                        (zero? (.-size file))
                                        {:value    ""
                                         :filename (.-name file)
                                         :size     (.-size file)
                                         :status   :error
                                         :valid    false
                                         :errors   [[:file-size-info-min]]}

                                        :else
                                        {:value         ""
                                         :filename      (.-name file)
                                         :size          (.-size file)
                                         :uploaded-size 0
                                         :last-progress (c/now)
                                         :speed         0
                                         :status        :uploading
                                         :valid         false}))
                                    new-files)]
      {:db         (-> db
                       (assoc-in [:application :answers id :valid] false)
                       (update-in [:application :answers id :values]
                                  (fnil identity (if (some? question-group-idx)
                                                   (vec (repeat (inc question-group-idx) []))
                                                   [])))
                       (assoc-in path (vec (concat existing-attachments
                                                   new-attachments))))
       :dispatch-n (keep-indexed (fn [idx file]
                                   (when (< 0 (.-size file) (inc max-attachment-size-bytes))
                                     [:application/add-single-attachment-resumable
                                      field-descriptor
                                      (+ (count existing-attachments) idx)
                                      file
                                      0
                                      question-group-idx]))
                                 new-files)})))

(reg-event-fx
  :application/set-attachment-valid
  (fn [{:keys [db]} [_ id required? valid?]]
    (let [answer                 (get-in db [:application :answers id])
          question-group-answer? (and (vector? (:values answer))
                                      (not (empty? (:values answer)))
                                      (every? vector? (:values answer)))
          new-db                 (assoc-in db [:application :answers id :valid]
                                           (and (if question-group-answer?
                                                  (every? (partial every? :valid) (:values answer))
                                                  (every? :valid (:values answer)))
                                                (not (and required?
                                                          (if question-group-answer?
                                                            (some empty? (:values answer))
                                                            (empty? (:values answer)))))
                                                valid?))]
      {:db       new-db
       :dispatch [:application/set-validator-processed id]})))

(reg-event-fx
  :application/handle-attachment-upload
  (fn [{db :db} [_ field-descriptor attachment-idx question-group-idx response]]
    (let [id       (keyword (:id field-descriptor))
          path     (if question-group-idx
                     [:application :answers id :values question-group-idx attachment-idx]
                     [:application :answers id :values attachment-idx])
          filename (:filename @(subscribe [:application/answer
                                           id
                                           question-group-idx
                                           attachment-idx]))]
      {:db                 (-> db
                               (update-in [:attachments-uploading id] dissoc filename)
                               (update-in path
                                          merge
                                          {:value    (:key response)
                                           :filename (:filename response)
                                           :size     (:size response)
                                           :status   :ready
                                           :valid    true})
                               (set-validator-processing id)
                               (set-multi-value-changed id :values))
       :validate-debounced {:value                        (get-in db path)
                            :priorisoivat-hakukohderyhmat (get-in db [:form :priorisoivat-hakukohderyhmat])
                            :answers-by-key               (get-in db [:application :answers])
                            :field-descriptor             field-descriptor
                            :editing?                     (get-in db [:application :editing?])
                            :virkailija?                  (contains? (:application db) :virkailija-secret)
                            :on-validated                 (fn [[valid? errors]]
                                                            (dispatch [:application/set-attachment-valid
                                                                       id
                                                                       (required? field-descriptor)
                                                                       valid?]))}})))

(reg-event-db
  :application/handle-attachment-upload-started
  (fn [db [_ field-descriptor attachment-idx question-group-idx request]]
    (let [id (keyword (:id field-descriptor))]
      (-> db
          (assoc-in (cond-> [:application :answers id :values]
                            (some? question-group-idx)
                            (conj question-group-idx)
                            true
                            (conj attachment-idx :request))
                    request)))))

(reg-event-fx
  :application/handle-attachment-upload-error
  (fn [{:keys [db]} [_ field-descriptor attachment-idx file retries question-group-idx response-status]]
    (let [id            (keyword (:id field-descriptor))
          current-error (case response-status
                          ; misc error in resumable file transfer, retry:
                          409 :file-upload-retransmit
                          ; rate limited:
                          429 :file-upload-failed
                          ; forbidden file type:
                          400 :file-type-forbidden
                          ; generic error, e.g. transfer interrupted:
                          :file-upload-error)]
      (if (and (contains? #{:file-upload-failed :retransmit} current-error) (< retries 3))
        {:db               db
         :delayed-dispatch {:dispatch-vec [:application/add-single-attachment-resumable field-descriptor attachment-idx file retries question-group-idx]
                            :timeout      (+ 2000 (rand-int 2000))}}
        {:db       (-> db
                       (update-in [:attachments-uploading id] dissoc (:filename @(subscribe [:application/answer
                                                                                             id
                                                                                             question-group-idx
                                                                                             attachment-idx])))
                       (update-in (if question-group-idx
                                    [:application :answers id :values question-group-idx attachment-idx]
                                    [:application :answers id :values attachment-idx])
                                  merge
                                  {:valid  false
                                   :status :error
                                   :errors [[current-error]]}))
         :dispatch [:application/set-attachment-valid
                    id
                    (required? field-descriptor)
                    false]}))))

(reg-event-fx
  :application/handle-attachment-delete
  (fn [{db :db} [_ field-descriptor question-group-idx attachment-key _]]
    (let [id (keyword (:id field-descriptor))]
      {:db       (-> db
                     (update-in [:application :answers id :values]
                                (fnil identity (if (some? question-group-idx)
                                                 (vec (repeat (inc question-group-idx) []))
                                                 [])))
                     (update-in (cond-> [:application :answers id :values]
                                        (some? question-group-idx)
                                        (conj question-group-idx))
                                (fn [values]
                                  (vec (remove #(= attachment-key (:value %)) values))))
                     (set-multi-value-changed id :values))
       :dispatch [:application/set-attachment-valid
                  id
                  (required? field-descriptor)
                  true]})))

(reg-event-fx
  :application/remove-attachment
  (fn [{:keys [db]} [_ field-descriptor question-group-idx attachment-idx]]
    (let [id   (keyword (:id field-descriptor))
          path (cond-> [:application :answers id :values]
                       (some? question-group-idx)
                       (conj question-group-idx)
                       true
                       (conj attachment-idx))
          key  (get-in db (conj path :value))]
      (if (and (not (get-in db [:application :editing?]))
               (= :ready (get-in db (conj path :status))))
        {:db   (-> db
                   (assoc-in [:application :answers id :valid] false)
                   (update-in path merge {:status :deleting
                                          :valid  false}))
         :http {:method  :delete
                :url     (str "/hakemus/api/files/" key)
                :handler [:application/handle-attachment-delete field-descriptor question-group-idx key]}}
        {:db       (-> db
                       (assoc-in [:application :answers id :valid] false)
                       (update-in (butlast path) autil/remove-nth attachment-idx)
                       (set-multi-value-changed id :values))
         :dispatch [:application/set-attachment-valid
                    id
                    (required? field-descriptor)
                    true]}))))

(reg-event-fx
  :application/cancel-attachment-upload
  (fn [{db :db} [_ field-descriptor question-group-idx attachment-idx]]
    {:http-abort (get-in db (cond-> [:application :answers (keyword (:id field-descriptor)) :values]
                                    (some? question-group-idx)
                                    (conj question-group-idx)
                                    true
                                    (conj attachment-idx :request)))}))

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

(reg-event-db
  :application/handle-feedback-submit
  (fn [db _] db))

(reg-event-fx
  :application/rating-feedback-submit
  (fn [{:keys [db]}]
    (let [lang-kw   (keyword (-> db :form :selected-language))
          new-db    (assoc-in db [:application :feedback :status] :feedback-submitted)
          feedback  (-> db :application :feedback)
          text      (:text feedback)
          post-data {:form-key   (-> db :form :key)
                     :form-id    (-> db :form :id)
                     :form-name  (autil/non-blank-val (-> db :form :name) [lang-kw :fi :sv :en])
                     :user-agent (.-userAgent js/navigator)
                     :rating     (:stars feedback)
                     :haku-oid   (-> db :form :tarjonta :haku-oid)
                     :feedback   (when text
                                   (subs text 0 2000))}]
      {:db   new-db
       :http {:method    :post
              :post-data post-data
              :url       "/hakemus/api/feedback"
              :handler   [:application/handle-feedback-submit]}})))

(reg-event-db
  :application/rating-form-toggle
  (fn [db _]
    (update-in db [:application :feedback :hidden?] not)))

(reg-event-fx
  :application/set-page-title
  (fn [{:keys [db]}]
    (let [lang-kw       (keyword (-> db :form :selected-language))
          title-prefix  (translations/get-hakija-translation :page-title lang-kw)
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
      [[:application/select-single-choice-button
        nil
        field-descriptor
        group-idx]]
      {:fieldType "multipleChoice"}
      (let [d [:application/toggle-multiple-choice-option
               field-descriptor
               (first (:options field-descriptor))
               group-idx]]
        [d d])
      {:fieldType "adjacentfieldset"}
      (mapv (fn [child]
              [:application/set-repeatable-application-field child "" 0 group-idx])
            (:children field-descriptor))
      {:fieldType "attachment"}
           ; Use handle attachment delete here since when calling with nil it 'initializes' an empty answer.
           ; Hacky solution but others would require much rework on the codebase.
      [[:application/handle-attachment-delete field-descriptor group-idx nil nil]]
      {:fieldClass "infoElement"}
      [])))

(reg-event-fx
  :application/add-question-group-row
  (fn add-question-group-row [{db :db} [_ field-descriptor]]
    (let [id           (keyword (:id field-descriptor))
          repeat-count (get-in db [:application :ui id :count] 1)]
      {:db         (-> db
                       (assoc-in [:application :ui id :count] (inc repeat-count))
                       (update-in [:application :ui id] dissoc :mouse-over-remove-button))
       :dispatch-n (mapcat (partial set-empty-value-dispatch repeat-count)
                           (:children field-descriptor))})))

(reg-event-fx
  :application/remove-question-group-row
  (fn [{:keys [db]} [_ field-descriptor idx]]
    (let [id                     (keyword (:id field-descriptor))
          with-decremented-count (-> db
                                     (update-in [:application :ui id :count] dec)
                                     (update-in [:application :ui id] dissoc :mouse-over-remove-button))
          rules                  (->> (:children field-descriptor)
                                      (map :rules)
                                      (apply merge))]
      {:db         (autil/reduce-form-fields
                    (fn [db child]
                      (let [id     (keyword (:id child))
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
                    (:children field-descriptor))
       :dispatch-n (cond-> [[:application/update-answers-validity]]
                           (not-empty rules)
                           (conj [:application/run-rules rules]))})))

(reg-event-fx
  :application/dropdown-change
  (fn [_ [_ field-descriptor value group-idx]]
    {:dispatch (if (some? group-idx)
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

(reg-event-fx
  :application/setup-window-unload
  (fn [_ _]
    {:set-window-close-callback nil}))
