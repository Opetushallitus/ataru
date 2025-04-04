(ns ataru.hakija.application-handlers
  (:require [ataru.config :as config]
            [ataru.constants :as constants]
            [clojure.string :as string]
            [re-frame.core :refer [reg-event-db reg-event-fx dispatch subscribe after inject-cofx]]
            [ataru.application-common.application-field-common :refer [sanitize-value]]
            [schema.core :as s]
            [ataru.application.option-visibility :as option-visibility]
            [ataru.feature-config :as fc]
            [ataru.hakija.schema :as schema]
            [ataru.component-data.base-education-module-higher :as higher-module]
            [ataru.cljs-util :as util]
            [ataru.util :as autil]
            [ataru.hakija.ht-util :as ht-util]
            [ataru.tutkinto.tutkinto-util :as tutkinto-util]
            [ataru.hakija.person-info-fields :as person-info-fields]
            [ataru.hakija.rules :as rules]
            [ataru.hakija.resumable-upload :as resumable-upload]
            [ataru.hakija.try-selection :refer [try-selection]]
            [ataru.translations.translation-util :as translations]
            [cljs.core.match :refer-macros [match]]
            [ataru.hakija.handlers-util :as handlers-util]
            [ataru.hakija.application :refer [create-initial-answers
                                              create-application-to-submit
                                              extract-wrapper-sections]]
            [ataru.hakija.application.field-visibility :as field-visibility]
            [ataru.component-data.value-transformers :as value-transformers]
            [cljs-time.core :as c]
            [cljs-time.format :as f]
            [cljs-time.coerce :refer [to-long]]
            [ataru.hakija.demo :as demo]
            [ataru.hakija.validation-error :as validation-error]))

(def db-validator (s/validator schema/Db))

(def check-schema-interceptor
  (after (fn [db _]
           (when (fc/feature-enabled? :schema-validation)
             (db-validator db)))))

(defn initialize-db [_ _]
  {:form        nil
   :application {:attachments-id (random-uuid)
                 :answers        {}
                 :editing?       false}})

(reg-event-db
  :application/set-secret-delivery-status
  [check-schema-interceptor]
  (fn [db [_ status]]
    (assoc-in db [:application :secret-delivery-status] status)))

(reg-event-fx
  :application/handle-get-application-by-hakija-secret-error
  [check-schema-interceptor]
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
  [check-schema-interceptor]
  (fn [db _]
    (assoc-in db [:application :secret-delivery-status] :completed)))

(reg-event-fx
  :application/send-new-secret
  [check-schema-interceptor]
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
  [check-schema-interceptor]
  get-application-by-hakija-secret)

(defn- get-application-by-virkailija-secret
  [{:keys [db]} [_ virkailija-secret]]
  {:db   db
   :http {:method  :get
          :url     (str "/hakemus/api/application?virkailija-secret=" virkailija-secret)
          :handler [:application/handle-get-application {:virkailija-secret virkailija-secret}]}})

(reg-event-fx
  :application/get-application-by-virkailija-secret
  [check-schema-interceptor]
  get-application-by-virkailija-secret)

(reg-event-fx
  :application/get-latest-form-by-key
  [check-schema-interceptor]
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
  [check-schema-interceptor]
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
  :application/start-oppija-session-polling
  (fn [{:keys [db]} [_]]
    (let [logged-in? (get-in db [:oppija-session :logged-in])
          polling-interval (-> js/config
                               js->clj
                               (get "oppija-session-polling-interval"))]
      ;Tämä kuuntelija auttaa sellaisiin tilanteisiin, kun selain on syystä tai toisesta ollut unten mailla pidempään.
      (.addEventListener js/window "focus" (fn [] (dispatch [:application/oppija-session-login-refresh-if-late])))
      (if logged-in?
        {:db db
         :interval {:action :start
                    :id :oppija-session-polling
                    :frequency polling-interval
                    :event [:application/oppija-session-login-refresh]}}
        db))))

(reg-event-fx
  :application/stop-oppija-session-polling
  (fn [{:keys [db]} [_]]
    (let [logged-in? (get-in db [:oppija-session :logged-in])]
      (if (not logged-in?)
        {:db db
         :interval {:action :stop
                    :id :oppija-session-polling
                    :event [:application/oppija-session-login-refresh]}}
        db))))

(reg-event-fx
  :application/oppija-session-login-refresh-if-late
  (fn [{:keys [db]} [_]]
    (let [polling-interval (-> js/config
                               js->clj
                               (get "oppija-session-polling-interval"))
          logged-in? (get-in db [:oppija-session :logged-in])
          last-poll (get-in db [:oppija-session :last-refresh])
          now (.getTime (js/Date.))
          long-time-since-last-poll? (> now (+ last-poll polling-interval 30000))]
      (if (and logged-in? long-time-since-last-poll?)
        {:db   (assoc-in db [:oppija-session :last-refresh] now)
         :http {:method  :get
                :url     (str "/hakemus/auth/session")
                :handler [:application/handle-oppija-session-login-refresh]}}
        {:db db}))))

(reg-event-fx
  :application/oppija-session-login-refresh
  (fn [{:keys [db]} [_]]
    {:db   db
     :http {:method  :get
            :url     (str "/hakemus/auth/session")
            :handler [:application/handle-oppija-session-login-refresh]}}))

(reg-event-db
  :application/show-session-expiry-warning-dialog
  (fn [db [_ warning-minutes]]
    (assoc-in db [:oppija-session :session-expires-in-minutes-warning] warning-minutes)))

(reg-event-fx
  :application/schedule-extra-poll-after-predicted-expiry-if-needed
  (fn [{:keys [db]} [_ seconds-left-in-session]]
    (let [already-set? (get-in db [:oppija-session :expiry-poll-scheduled] false)
          polling-interval-seconds  (/ (-> js/config
                                           js->clj
                                           (get "oppija-session-polling-interval"))
                                       1000)
          need-to-set? (and (not already-set?) (>= (+ polling-interval-seconds 5) seconds-left-in-session))
          timeout-millis (* 1000 (+ 2 seconds-left-in-session))]
      (if need-to-set?
        {:db (-> db
                 (assoc-in [:oppija-session :expiry-poll-scheduled] true))
         :dispatch-debounced {:timeout  timeout-millis
                              :id       "oppija-session-expires-extra-poll"
                              :dispatch [:application/oppija-session-login-refresh]}}
        {:db db}))))

(reg-event-fx
  :application/show-session-expiry-warning-dialog-if-needed
  (fn [{:keys [db]} [_ seconds-left-in-session]]
    (let [previous-warnings (get-in db [:oppija-session :activated-warnings] #{})
          polling-interval-seconds  (/ (-> js/config
                                           js->clj
                                           (get "oppija-session-polling-interval"))
                                       1000)
          extra-margin-seconds 30 ;Pelataan varman päälle ettei esimerkiksi hitaiden kutsujen kanssa käy yllätyksiä
          warning-to-set (ht-util/warning-to-set seconds-left-in-session polling-interval-seconds extra-margin-seconds previous-warnings)]
      (if warning-to-set
        {:db (-> db
                 (update-in [:oppija-session :activated-warnings] (fn [warnings] (conj (or warnings #{}) (first warning-to-set)))))
         :dispatch-debounced {:timeout  (second warning-to-set)
                              :id       (str "oppija-session-expires-warning-" (first warning-to-set))
                              :dispatch [:application/show-session-expiry-warning-dialog (first warning-to-set)]}}
        {:db db}))))

(reg-event-fx
  :application/handle-oppija-session-login-refresh
  (fn [{:keys [db]} [_ response]]
    (let [session-data (get-in response [:body])
          logged-in? (:logged-in session-data)]
      {:db (-> db
               (assoc-in [:oppija-session :logged-in] logged-in?)
               (assoc-in [:oppija-session :expired] (not logged-in?))
               (assoc-in [:oppija-session :last-refresh] (.getTime (js/Date.))))
       :dispatch-n [[:application/show-session-expiry-warning-dialog-if-needed (:seconds-left session-data)]
                    [:application/schedule-extra-poll-after-predicted-expiry-if-needed (:seconds-left session-data)]
                    (when (not logged-in?) [:application/stop-oppija-session-polling])]})))

(reg-event-fx
  :application/get-oppija-session
  (fn [{:keys [db]} [_]]
    {:db   db
     :http {:method  :get
            :url     (str "/hakemus/auth/session")
            :handler [:application/handle-oppija-session-fetch]
            :error-handler [:application/handle-ht-error]}}))

(reg-event-fx
  :application/get-latest-form-by-haku
  [check-schema-interceptor]
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

(reg-event-fx
  :application/set-demo-requested
  [check-schema-interceptor (inject-cofx :now)]
  (fn [{:keys [db now]} [_ demo-lang]]
    {:db
     (-> db
       (assoc :demo-requested true)
       (assoc :demo-lang demo-lang)
       (assoc :demo-modal-open? true)
       (assoc :today now))}))

(reg-event-db
  :application/close-demo-modal
  [check-schema-interceptor]
  (fn [db _]
    (assoc db :demo-modal-open? false)))

(defn- send-application-with-api
  [db method]
  (when-not (-> db :application :submit-status)
    {:db   (-> db (assoc-in [:application :submit-status] :submitting) (dissoc :error))
     :http {:method        method
            :url           "/hakemus/api/application"
            :post-data     (create-application-to-submit (:application db)
                                                         (:form db)
                                                         (get-in db [:form :selected-language])
                                                         (:strict-warnings-on-unchanged-edits? db)
                                                         (:oppija-session db))
            :handler       [:application/handle-submit-response]
            :error-handler [:application/handle-submit-error]}}))

(defn- send-application-demo
  []
  {:dispatch [:application/handle-submit-response]})

(defn send-application [db method]
  (if (demo/demo? db)
    (send-application-demo)
    (send-application-with-api db method)))

(reg-event-db
  :application/handle-submit-response
  [check-schema-interceptor]
  (fn [db [_ response]]
    (let [payment (-> response :body :payment)]
      (-> db
        (assoc-in [:application :submit-status] :submitted)
        (cond-> payment (assoc-in [:application :submit-details] payment))))))

(defn response->error-message [db response]
  (assoc db :error {:code    (keyword (get-in response [:body :code] "internal-server-error"))
                    :message "Tapahtui virhe"
                    :lang    (keyword (get-in response [:body :lang]))
                    :detail  (str response)}))

(reg-event-fx
  :application/handle-submit-error
  [check-schema-interceptor]
  (fn [cofx [_ response]]
    {:db (-> (update (:db cofx) :application dissoc :submit-status)
             (response->error-message response))}))

(reg-event-fx
  :application/submit
  [check-schema-interceptor]
  (fn [{:keys [db]} _]
    (send-application db :post)))

(reg-event-fx
  :application/edit
  [check-schema-interceptor]
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

(defn set-field-visibilities
  [db]
  (rules/run-all-rules
    (reduce field-visibility/set-field-visibility db (get-in db [:form :content]))
    (:flat-form-content db)))

(defn- set-have-finnish-ssn
  [db flat-form-content]
  (let [cannot-view?   (some #(and (= "ssn" (:id %)) (:cannot-view %))
                             flat-form-content)
        ssn-value      (get-in db [:application :answers :ssn :value])
        have-ssn-value (if (or (and cannot-view? (nil? ssn-value))
                               (not (string/blank? ssn-value)))
                         "true"
                         "false")]
    (update-in db [:application :answers :have-finnish-ssn]
               merge {:valid  true
                      :value  have-ssn-value
                      :values {:value have-ssn-value
                               :valid true}})))

(defn- populate-hakukohde-answers-if-necessary
  "Populate hakukohde answers for legacy applications where only top-level hakukohde array exists"
  [db]
  (let [hakukohteet (-> db :application :hakukohde)
        hakukohde-answers (-> db :application :answers :hakukohteet :values)]
    (if (and (not-empty hakukohteet)
             (empty? hakukohde-answers))
      (-> db
          (assoc-in [:application :answers :hakukohteet :values] (mapv (fn [oid] {:valid true :value oid}) hakukohteet))
          (assoc-in [:application :answers :hakukohteet :value] (vec hakukohteet))
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

(defn- is-answered? [value]
  (if (vector? value)
    (if (or (vector? (first value)) (nil? (first value)))
      (some #(and (not-empty %) (every? some? %)) value)
      (and (not-empty value) (every? some? value)))
    (some? value)))

(defn- is-question-group-value?
  [value]
  (and (vector? value) (or (vector? (first value)) (nil? (first value)))))

(defn- merge-question-group-value
  [field-descriptor value]
  (mapv #(when (vector? %)
           (if (and (= "oppiaineen-arvosanat-valinnaiset-kielet" (:children-of field-descriptor))
                 (empty? %))
             [{:value "" :valid true}]
             (mapv (fn [value] {:valid true :value value}) %)))
    value))

(defn- merge-value [answer field-descriptor value]
  (merge answer {:valid  (boolean (or (:valid answer)
                                    (:per-hakukohde field-descriptor)
                                    (:cannot-edit field-descriptor)
                                    (is-answered? (sanitize-value field-descriptor value nil))))
                 :value  value
                 :values (cond (is-question-group-value? value)
                               (merge-question-group-value field-descriptor value)

                               (vector? value)
                               (mapv (fn [value] {:valid true :value value}) value)

                               :else
                               {:value value
                                :valid true})
                 }))

(defn- original-values->answers [db]
  (update-in db [:application :answers]
             (partial reduce-kv
                      (fn [answers answer-key answer]
                        (assoc answers answer-key (assoc answer :original-value (:value answer))))
                      {})))

(defn- merge-submitted-answers [db submitted-answers flat-form-content]
  (let [form-fields-by-id (autil/group-by-first (comp keyword :id) flat-form-content)]
    (-> (reduce (fn [db answer]
                  (let [id               (keyword (:key answer))
                        field-descriptor (get form-fields-by-id id)]
                    (if (contains? (get-in db [:application :answers]) id)
                      (update-in db [:application :answers id]
                                 #(cond (= :email id)
                                        (-> %
                                            (merge-value field-descriptor (:value answer))
                                            (assoc :verify (:value answer)))

                                        (= "attachment" (:fieldType field-descriptor))
                                        (let [values (mapv (fn self [value]
                                                             (cond (vector? value)
                                                                   (mapv self value)
                                                                   (nil? value)
                                                                   nil
                                                                   :else
                                                                   {:value    (:key value)
                                                                    :filename (:filename value)
                                                                    :size     (:size value)
                                                                    :status   :ready
                                                                    :valid    true}))
                                                           (:value answer))]
                                          (merge % {:value  (mapv (fn self [value]
                                                                    (cond (vector? value)
                                                                          (mapv self value)
                                                                          (nil? value)
                                                                          nil
                                                                          :else
                                                                          (:value value)))
                                                                 values)
                                                    :values values
                                                    :valid  true}))

                                        :else
                                        (merge-value % field-descriptor (:value answer))))
                      db)))
                db
                submitted-answers)
        (populate-hakukohde-answers-if-necessary)
        (set-have-finnish-ssn flat-form-content)
        (original-values->answers)
        (rules/run-all-rules flat-form-content)
        (set-question-group-row-amounts))))

(defn- set-question-group-id
  ([field]
   (let [update-group-child (partial set-question-group-id (keyword (:id field)))
         update-followups (fn [option] (update option :followups (partial map set-question-group-id)))]
     (if (= "questionGroup" (:fieldClass field))
       (update field :children (partial map update-group-child))
       (reduce
         (fn [field' option-field] (update field' option-field (partial map update-followups)))
         (if (contains? field :children)
           (update field :children (partial map set-question-group-id))
           field)
         (autil/find-option-fields field)))))
  ([question-group-id field]
   (let [update-child (partial set-question-group-id question-group-id)
         update-followups (fn [option] (update option :followups (partial map update-child)))]
     (reduce
       (fn [field' option-field] (update field' option-field (partial map update-followups)))
       (cond-> (assoc-in field [:params :question-group-id] question-group-id)
               (contains? field :children)
               (update :children (partial map update-child)))
       (autil/find-option-fields field)))))

(defn- set-adjacent-field-id
  [field-descriptor]
  (if (= "adjacentfieldset" (:fieldType field-descriptor))
    (update field-descriptor :children (partial mapv #(assoc-in % [:params :adjacent-field-id] (keyword (:id field-descriptor)))))
    field-descriptor))

(defn- handle-form [db answers server-date form]
  (let [hakuaika-on?               (-> form :tarjonta :hakuaika :on)
        hakuaika-end               (->> form :tarjonta :hakukohteet
                                        (map :hakuaika)
                                        (filter :on)
                                        (sort-by :end >)
                                        first
                                        :end)
        time-diff                  (if (some? server-date)
                                     (- (->> (clojure.string/replace server-date " GMT" "")
                                             (f/parse (f/formatter "EEE, dd MMM yyyy HH:mm:ss"))
                                             to-long)
                                        (.getTime (js/Date.)))
                                     0)
        form                       (-> (languages->kwd form)
                                       (set-form-language)
                                       (update :content (partial map set-question-group-id))
                                       (update :content (partial autil/map-form-fields set-adjacent-field-id))
                                       (assoc :hakuaika-end hakuaika-end)
                                       (assoc :time-delta-from-server time-diff))
        valid-hakukohde-oids       (set (->> form :tarjonta :hakukohteet
                                             (filter #(or (demo/demo? db form) (get-in % [:hakuaika :on])))
                                             (map :oid)))
        preselected-hakukohde-oids (->> db :application :preselected-hakukohde-oids
                                        (filter #(contains? valid-hakukohde-oids %)))
        form-hakukohde-oids        (->> (:content form)
                                     (filter #(= "hakukohteet" (:id %)))
                                     first
                                     :options
                                     (mapv :value))
        preselected-hakukohde-oids (cond
                                     (> (count preselected-hakukohde-oids) 0)
                                     preselected-hakukohde-oids

                                     ; Jos lomakkeella on vain yksi hakukohde, käytetään sitä
                                     (= 1 (count form-hakukohde-oids))
                                     form-hakukohde-oids

                                     :else
                                     [])
        excluded-attachment-ids-when-yo-and-jyemp (higher-module/non-yo-attachment-ids form)
        questions                  (demo/apply-when-demo db form demo/remove-unwanted-validators (:content form))
        hakukohde-oids-to-duplicate (if-let [application-hakukohde-oids (get-in db [:application :hakukohde])]
                                      application-hakukohde-oids
                                      preselected-hakukohde-oids)
        questions-with-duplicates  (handlers-util/duplicate-questions-for-hakukohteet-during-form-load (get-in form [:tarjonta :hakukohteet]) hakukohde-oids-to-duplicate questions)
        flat-form-content          (autil/flatten-form-fields questions-with-duplicates)
        selected-language          (:selected-language form)
        initial-answers            (create-initial-answers flat-form-content preselected-hakukohde-oids selected-language)]
    (-> db
        (update :form (fn [{:keys [selected-language]}]
                        (cond-> form
                                (some? selected-language)
                                (assoc :selected-language selected-language))))
        (assoc :flat-form-content flat-form-content)
        (assoc-in [:application :excluded-attachment-ids-when-yo-and-jyemp] excluded-attachment-ids-when-yo-and-jyemp)
        (assoc-in [:application :answers] initial-answers)
        (assoc-in [:application :show-hakukohde-search] false)
        (assoc-in [:form :content] questions-with-duplicates)
        (assoc-in [:application :validators-processing] #{})
        (assoc :strict-warnings-on-unchanged-edits? hakuaika-on?)
        (assoc :wrapper-sections (extract-wrapper-sections form))
        (merge-submitted-answers answers flat-form-content)
        (set-field-visibilities))))

(defn- selection-limits [{:keys [flat-form-content]}]
  (let [limited? (fn [{:keys [validators]}]
                   (some #(= "selection-limit" %) validators))]
    (->> flat-form-content
         (filter limited?)
         (map :id)
         (set)
         (not-empty))))

(defn- is-selection-limit? [item]
  (boolean (some #(= "selection-limit" %) item)))

(defn get-question-ids-by-question-parent-id [db parent-id]
  (map
    #(:id %) (filter (fn [item] (and (= (:children-of item) parent-id) (is-selection-limit? (:validators item)))) (:flat-form-content db))))

(defn get-selection-parent-id [db question-id]
  (:children-of
    (first (filter (fn [item] (= (:id item) (name question-id))) (:flat-form-content db)))))

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

(defn reset-other-selections [db question-id _]
  (let [parent-id (get-selection-parent-id db question-id)
        question-group-question-ids (get-question-ids-by-question-parent-id db parent-id)]
    (if (some? parent-id)
      (reduce (fn [db key]
              (if (= key question-id)
                db
                (-> db
                    (assoc-in [:application :answers key :values :value] nil)
                    (assoc-in [:application :answers key :value] nil)
                    (assoc-in [:application :answers key :valid] false))))
            db
              (map keyword question-group-question-ids))
      db)))

(reg-event-fx
  :application/handle-update-selection-limits
  [check-schema-interceptor]
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
  [check-schema-interceptor]
  (fn [db [_ response]]
    (set-limit-reached db (:body response))))

(reg-event-fx
  :application/post-handle-form-dispatches
  [check-schema-interceptor]
  (fn [{:keys [db]} _]
    (let [selection-limited (selection-limits db)
          virkailija-secret (get-in db [:application :virkailija-secret])
          hakija-secret (get-in db [:application :secret])
          form-allows-hakeminen-tunnistautuneena? (get-in db [:form :properties :allow-hakeminen-tunnistautuneena] false)
          demo? (demo/demo? db)]
      (merge
        {:db         (assoc db :selection-limited selection-limited)
         :dispatch-n [[:application/hakukohde-query-change (atom "")]
                      [:application/set-page-title]
                      [:application/validate-hakukohteet]
                      [:application/hide-form-sections-with-text-component-visibility-rules]
                      [:application/fetch-koulutustyypit]
                      (when (and
                              (clojure.string/blank? virkailija-secret)
                              (clojure.string/blank? hakija-secret)
                              form-allows-hakeminen-tunnistautuneena?
                              (not demo?)
                              (fc/feature-enabled? :hakeminen-tunnistautuneena))
                        [:application/get-oppija-session])]}
        (when (and selection-limited (not demo?))
          {:http {:method  :put
                  :url     (str "/hakemus/api/selection-limit?form-key=" (-> db :form :key))
                  :handler [:application/handle-selection-limit]}})))))

(defn- handle-get-application [{:keys [db]}
                               [_
                                {:keys [secret virkailija-secret]}
                                response]]
  (let [{:keys [application person form koski-tutkinnot]} (:body response)
        [secret-kwd secret-val]           (if-not (clojure.string/blank? secret)
                                            [:secret secret]
                                            [:virkailija-secret virkailija-secret])]
    (util/set-query-param "application-key" (:key application))
    {:db       (-> (if koski-tutkinnot
                     (assoc-in db [:application :koski-tutkinnot] (tutkinto-util/sort-koski-tutkinnot koski-tutkinnot))
                     db)
                   (assoc-in [:application :application-identifier] (:application-identifier application))
                   (assoc-in [:application :editing?] true)
                   (assoc-in [:application secret-kwd] secret-val)
                   (assoc-in [:application :state] (:state application))
                   (assoc-in [:application :hakukohde] (:hakukohde application))
                   (assoc-in [:application :person] person)
                   (assoc-in [:application :cannot-edit-because-in-processing] (:cannot-edit-because-in-processing application))
                   (assoc-in [:form :selected-language] (or (keyword (:lang application)) :fi))
                   (handle-form (:answers application) (get-in response [:headers "date"]) form))
     :dispatch-n [[:application/set-itse-syotetyt-visibility (not (seq koski-tutkinnot))]
                  [:application/post-handle-form-dispatches]]}))

(reg-event-fx
  :application/handle-get-application
  [check-schema-interceptor]
  handle-get-application)

(reg-event-db
  :flasher
  [check-schema-interceptor]
  (fn [db [_ flash]]
    (assoc db :flasher flash)))

(reg-event-fx
  :application/handle-form
  [check-schema-interceptor]
  (fn [{:keys [db]} [_ response]]
    {:db       (handle-form db nil (get-in response [:headers "date"]) (:body response))
     :dispatch [:application/post-handle-form-dispatches]}))

(defn- prefill-and-lock-answers [db]
  (if (get-in db [:oppija-session :logged-in])
    (let [locked-answers (get-in db [:oppija-session :fields])]
      (reduce (fn [db [key {:keys [locked value]}]]
                (if (not (clojure.string/blank? value))
                  ;Fixme ehkä, Mitä jos cas-oppijan kautta saadaan syystä tai toisesta
                  ;ei-validi arvo? Se ois kyllä huono juttu, validaatio tai ei.
                  ;ehkä cas-oppijalta tulevat arvot vois validoida jo backendissä.
                  ;ssn erityistapaus, tarkistetaan onko jo hakenut haussa.
                  (update-in db [:application :answers key] (fn [ans]
                                                              (merge (-> ans
                                                                         (assoc :cannot-edit true)
                                                                         (assoc :value value)
                                                                         (assoc :valid true);Tässä oletetaan nyt, että kaikki epätyhjät arvot ovat valideja. Niin ei välttämättä oikeasti ole.
                                                                         (assoc :locked locked)
                                                                         (assoc-in [:values :value] value)
                                                                         (assoc-in [:values :valid] true))
                                                                     (when (= key :email) {:verify value}))))
                  db))
              db
              locked-answers))
    db))

(reg-event-fx
  :application/handle-ht-error
  [check-schema-interceptor]
  (fn [{:keys [db]} [_ response]]
    (js/console.log (str "Handle oppija session error fetch, resp" response))
    {:db (assoc-in db [:oppija-session :session-fetch-errored] true)}))

(defn- set-tutkinto-fetch-status-as-needed
  [db tutkinto-fetch-needed]
  (if tutkinto-fetch-needed
    db
    (assoc-in db [:oppija-session :tutkinto-fetch-handled] true)))

(reg-event-fx
  :application/handle-oppija-session-fetch
  [check-schema-interceptor]
  (fn [{:keys [db]} [_ response]]
    (let [session-data (get-in response [:body])
          requested-koski-levels (tutkinto-util/koski-tutkinto-levels-in-form (:form db))
          tutkinto-fetch-needed (and requested-koski-levels
                                     (= (:auth-type session-data) constants/auth-type-strong))]
      {:db (-> db
               (assoc :oppija-session (assoc session-data :session-fetched true))
               (assoc-in [:oppija-session :last-refresh] (.getTime (js/Date.)))
               (set-tutkinto-fetch-status-as-needed tutkinto-fetch-needed)
               (set-field-visibilities)
               (prefill-and-lock-answers))
       :dispatch-n [[:application/run-rules {:update-gender-and-birth-date-based-on-ssn nil
                                             :change-country-of-residence nil}]
                    [:application/fetch-has-applied-for-oppija-session session-data]
                    (when (:logged-in session-data)
                      [:application/start-oppija-session-polling]
                      (when tutkinto-fetch-needed
                        [:application/fetch-tutkinnot (string/join "," requested-koski-levels)]))]})))

(reg-event-fx
  :application/fetch-has-applied-for-oppija-session
  [check-schema-interceptor]
  (fn [{:keys [db]} [_ session-data]]
    (let [haku-oid (get-in db [:form :tarjonta :haku-oid])
          can-submit-multiple? (get-in db [:form :tarjonta :can-submit-multiple-applications])
          ssn (get-in session-data [:fields :ssn :value])
          eidas-id (get-in session-data [:eidas-id])
          body {:haku-oid haku-oid
                :ssn      ssn
                :eidas-id eidas-id}]
      (if (and (not can-submit-multiple?)
               haku-oid
               (or ssn eidas-id))
        {:db   db
         :http {:method    :post
                :url       "/hakemus/api/has-applied"
                :post-data body
                :handler   [:application/handle-fetch-has-applied-for-oppija-session]}}
        {:db db}))))

(reg-event-fx
  :application/handle-fetch-has-applied-for-oppija-session
  [check-schema-interceptor]
  (fn [{:keys [db]} [_ response]]
    (let [has-applied? (get-in response [:body :has-applied])]
      {:db (-> db
               (assoc-in [:application :has-applied] has-applied?))})))

(reg-event-db
  :application/network-online
  [check-schema-interceptor]
  (fn [db _]
    (if (= :network-offline (get-in db [:error :code]))
      (dissoc db :error)
      db)))

(reg-event-db
  :application/handle-selection-over-network-uncertain
  [check-schema-interceptor]
  (fn [db [_ uncertain?]]
    (if uncertain?
      (assoc-in db [:application :selection-over-network-uncertain?] true)
      (update db :application dissoc :selection-over-network-uncertain?))))

(reg-event-db
  :application/network-offline
  [check-schema-interceptor]
  (fn [db _]
    (if (get db :error)
      db
      (assoc-in db [:error :code] :network-offline))))

(reg-event-db
  :application/initialize-db
  [check-schema-interceptor]
  initialize-db)

(defn set-validator-processing
  [db id]
  (update-in db [:application :validators-processing] conj id))

(reg-event-db
  :application/set-validator-processed
  [check-schema-interceptor]
  (fn [db [_ id]]
    (update-in db [:application :validators-processing] disj id)))

(defn- transform-value [value field-descriptor]
  (let [t (case (:id field-descriptor)
            "birth-date" value-transformers/birth-date
            identity)]
    (or (t value) value)))

(reg-event-fx
  :application/set-email-verify-field
  [check-schema-interceptor]
  (fn [{:keys [db]} [_ field-descriptor value verify-value]]
    (let [id (keyword (:id field-descriptor))]
      {:db       (assoc-in db [:application :answers id :verify] verify-value)
       :dispatch [:application/set-repeatable-application-field field-descriptor nil nil value]})))

(defn set-repeatable-field-values
  [db id group-idx data-idx value]
  (cond (some? group-idx)
        (let [data-idx (or data-idx 0)]
          (-> db
              (update-in [:application :answers id :values] (util/vector-of-length (inc group-idx)))
              (update-in [:application :answers id :values group-idx] (util/vector-of-length (inc data-idx)))
              (assoc-in [:application :answers id :values group-idx data-idx :value] value)
              (update-in [:application :answers id :values group-idx data-idx :valid] (fnil identity true))))
        (some? data-idx)
        (-> db
            (update-in [:application :answers id :values] (util/vector-of-length (inc data-idx)))
            (assoc-in [:application :answers id :values data-idx :value] value)
            (update-in [:application :answers id :values data-idx :valid] (fnil identity true)))
        :else
        (-> db
            (assoc-in [:application :answers id :values :value] value)
            (update-in [:application :answers id :values :valid] (fnil identity true)))))

(defn- toggle-multiple-choice-option
  [db field-descriptor group-idx option-value]
  (let [id     (keyword (:id field-descriptor))
        toggle (fn [values]
                 (vec
                  (keep (fn [option]
                          (let [value (some #(when (= (:value option) (:value %)) %) values)]
                            (cond (not (= option-value (:value option)))
                                  value
                                  (some? value)
                                  nil
                                  :else
                                  {:value (:value option)
                                   :valid true})))
                        (:options field-descriptor))))]
    (if (some? group-idx)
      (-> db
          (update-in [:application :answers id :values] (util/vector-of-length (inc group-idx)))
          (update-in [:application :answers id :values group-idx] toggle))
      (update-in db [:application :answers id :values] toggle))))

(defn set-repeatable-field-value
  [db id]
  (let [values (get-in db [:application :answers id :values])]
    (assoc-in db [:application :answers id :value]
              (cond (is-question-group-value? values)
                    (mapv #(when (vector? %)
                             (mapv :value %)) values)
                    (vector? values)
                    (mapv :value values)
                    :else
                    (:value values)))))

(defn- set-repeatable-application-repeated-field-valid
  [db id group-idx data-idx valid?]
  (cond (some? group-idx)
        (let [data-idx (or data-idx 0)]
          (-> db
              (update-in [:application :answers id :values] (util/vector-of-length (inc group-idx)))
              (update-in [:application :answers id :values group-idx] #(when (vector? %)
                                                                         (-> %
                                                                             ((util/vector-of-length (inc data-idx)))
                                                                             (assoc-in [data-idx :valid] valid?))))))
        (some? data-idx)
        (-> db
            (update-in [:application :answers id :values] (util/vector-of-length (inc data-idx)))
            (assoc-in [:application :answers id :values data-idx :valid] valid?))
        :else
        (assoc-in db [:application :answers id :values :valid] valid?)))

(defn- set-multiple-choice-option-valid
  [db id group-idx option-value valid?]
  (let [set-valid (fn [values]
                    (when (vector? values)
                      (mapv (fn [value]
                              (if (= option-value (:value value))
                                (assoc value :valid valid?)
                                value))
                            values)))]
    (if (some? group-idx)
      (-> db
          (update-in [:application :answers id :values] (util/vector-of-length (inc group-idx)))
          (update-in [:application :answers id :values group-idx] set-valid))
      (update-in db [:application :answers id :values] set-valid))))

(defn- set-repeatable-application-field-top-level-valid
  [db id valid?]
  (let [values (get-in db [:application :answers id :values])
        valid-attachment? (fn [attachment]
                            (or (= :deleting (:status attachment))
                                (:valid attachment)))]
    (assoc-in db [:application :answers id :valid]
              (and valid?
                   (cond (is-question-group-value? values)
                         (every? #(or (nil? %) (every? valid-attachment? %)) values)
                         (vector? values)
                         (every? valid-attachment? values)
                         :else
                         (valid-attachment? values))))))

(reg-event-db
  :application/unset-question-group-field-value
  [check-schema-interceptor]
  (fn [db [_ field-descriptor group-idx]]
    (let [id (keyword (:id field-descriptor))]
      (-> (if (some? group-idx)
            (-> db
                (update-in [:application :answers id :values] (util/vector-of-length (inc group-idx)))
                (assoc-in [:application :answers id :values group-idx] nil))
            (assoc-in db [:application :answers id :values] nil))
          (set-repeatable-field-value id)
          (set-repeatable-application-field-top-level-valid id true)))))

(reg-event-db
  :application/unset-non-question-group-field-value
  [check-schema-interceptor]
  (fn [db [_ field-descriptor]]
    (let [id             (keyword (:id field-descriptor))
          lang-kw        (keyword (-> db :form :selected-language))
          initial-answer (get (create-initial-answers [field-descriptor] [] lang-kw) id)
          answer         (assoc initial-answer :original-value (:value initial-answer))
          limit-reached  (get-in db [:application :answers id :limit-reached])]
      (cond-> (assoc-in db [:application :answers id] answer)
              (some? limit-reached) (assoc-in [:application :answers id :limit-reached] limit-reached)))))

(defn set-empty-value-dispatch
  [group-idx field-descriptor]
  (match field-descriptor
         {:fieldType (:or "dropdown" "textField" "textArea")}
         [[:application/set-repeatable-application-field
           field-descriptor
           group-idx
           nil
           ""]]
         {:fieldType "singleChoice"}
         [[:application/set-repeatable-application-field
           field-descriptor
           group-idx
           nil
           nil]]
         {:fieldType "multipleChoice"}
         (let [d [:application/toggle-multiple-choice-option
                  field-descriptor
                  group-idx
                  (first (:options field-descriptor))]]
           [d d])
         {:fieldType "adjacentfieldset"}
         (mapv (fn [child]
                 [:application/set-repeatable-application-field child group-idx 0 ""])
               (:children field-descriptor))
         {:fieldType "attachment"}
         ;; Use handle attachment delete here since when calling with nil it 'initializes' an empty answer.
         ;; Hacky solution but others would require much rework on the codebase.
         [[:application/handle-attachment-delete field-descriptor group-idx nil nil nil]]
         :else
         nil))

(defn- option-visible? [field-descriptor option values]
  (let [visibility-checker (option-visibility/visibility-checker field-descriptor values)
        result (visibility-checker option)]
    result))

(defn- set-question-group-followup-values-dispatch
  [db field-descriptor value]
  (->> (for [option (:options field-descriptor)
             child  (autil/flatten-form-fields (:followups option))
             :when (autil/answerable? child)
             [group-idx values] (map-indexed vector value)]
         (if (option-visible? field-descriptor option values)
           (when (nil? (get-in db [:application :answers (keyword (:id child)) :values group-idx]))
             (set-empty-value-dispatch group-idx child))
           [[:application/unset-question-group-field-value child group-idx]]))
    (mapcat identity)
    vec))

(defn- set-non-question-group-followup-values-dispatch
  [field-descriptor value]
  (->> (for [option (:options field-descriptor)
             child  (autil/flatten-form-fields (:followups option))
             :when (and
                     (autil/answerable? child)
                     (not (option-visible? field-descriptor option value)))]
         [[:application/unset-non-question-group-field-value child]])
    (mapcat identity)
    vec))

(reg-event-fx
  :application/set-followup-values
  [check-schema-interceptor]
  (fn [{db :db} [_ field-descriptor]]
    (let [id    (keyword (:id field-descriptor))
          value (get-in db [:application :answers id :value])]
      (if (is-question-group-value? value)
        {:dispatch-n (set-question-group-followup-values-dispatch db field-descriptor value)}
        {:dispatch-n (set-non-question-group-followup-values-dispatch field-descriptor value)}))))

(reg-event-fx
  :application/set-repeatable-application-field-valid
  [check-schema-interceptor]
  (fn [{:keys [db]} [_ field-descriptor group-idx data-idx valid? errors]]
    (let [id (keyword (:id field-descriptor))]
      {:db         (-> db
                       (set-repeatable-application-repeated-field-valid id group-idx data-idx valid?)
                       (set-repeatable-application-field-top-level-valid id valid?)
                       (assoc-in [:application :answers id :errors] errors))
       :dispatch-n [[:application/set-validator-processed id]
                    [:application/run-rules (:rules field-descriptor)]]})))

(reg-event-db
  :application/hide-form-sections-with-text-component-visibility-rules
  (fn [db _]
    (let [form-content (:flat-form-content db)
          section-ids-with-visibility-rules (map :section-name (autil/visibility-conditions form-content))]
      (reduce
        #(field-visibility/set-field-visibility %1 %2)
        db
        section-ids-with-visibility-rules))))

(reg-event-db
  :application/handle-section-visibility-conditions
  (fn [db _]
    (set-field-visibilities db)))

(defn- handle-section-visibility-conditions-on-change?
  [field-descriptor]
  (and
    (seq (:section-visibility-conditions field-descriptor))
    (#{"dropdown" "singleChoice" "multipleChoice"} (:fieldType field-descriptor))))

(reg-event-fx
  :application/set-repeatable-application-field
  [check-schema-interceptor]
  (fn [{db :db} [_ field-descriptor question-group-idx data-idx value]]
    (let [id                            (keyword (:id field-descriptor))
          value                         (transform-value value field-descriptor)
          form-key                      (get-in db [:form :key])
          selection-id                  (get-in db [:application :selection-id])
          selection-group-id            (get-in field-descriptor [:params :selection-group-id])
          db                 (-> db
                                 (set-repeatable-field-values id question-group-idx data-idx value)
                                 (set-repeatable-field-value id)
                                 (field-visibility/set-field-visibility field-descriptor)
                                 (set-validator-processing id))]
      {:db                 db
       :dispatch-n         [[:application/set-followup-values field-descriptor]
                            (when (handle-section-visibility-conditions-on-change? field-descriptor)
                              [:application/handle-section-visibility-conditions])]
       :validate-debounced {:value                        value
                            :priorisoivat-hakukohderyhmat (get-in db [:form :priorisoivat-hakukohderyhmat])
                            :answers-by-key               (get-in db [:application :answers])
                            :field-descriptor             field-descriptor
                            :editing?                     (get-in db [:application :editing?])
                            :group-idx                    question-group-idx
                            :field-idx                    data-idx
                            :virkailija?                  (contains? (:application db) :virkailija-secret)
                            :try-selection                (partial try-selection
                                                                   db
                                                                   form-key
                                                                   selection-id
                                                                   selection-group-id)
                            :on-validated                 (fn [[valid? errors selection-limit]]
                                                            (when selection-group-id
                                                              (dispatch [:application/handle-selection-over-network-uncertain
                                                                         (and (not valid?) (not-empty errors))])
                                                              (when (-> (first selection-limit) :limit-reached)
                                                                (dispatch [:application/handle-update-selection-limits
                                                                           (first selection-limit) valid? id value])))
                                                            (dispatch [:application/set-repeatable-application-field-valid
                                                                       field-descriptor
                                                                       question-group-idx
                                                                       data-idx
                                                                       valid?
                                                                       errors]))}})))

(defn- remove-repeatable-field-value
  [db field-descriptor question-group-idx data-idx]
  (let [id (keyword (:id field-descriptor))
        preserve-first-value (fn [v idx]
                               (if (= (count v) 1)
                                 (-> v
                                     (assoc-in [idx :value] "")
                                     (assoc-in [idx :valid]
                                               (not (boolean (some #(= "required" %) (:validators field-descriptor))))))
                                 (autil/remove-nth v idx)))]
    (-> (if (some? question-group-idx)
          (update-in db [:application :answers id :values question-group-idx] preserve-first-value data-idx)
          (update-in db [:application :answers id :values] preserve-first-value data-idx))
        (set-repeatable-field-value id)
        (set-repeatable-application-field-top-level-valid id true))))

(reg-event-db
  :application/remove-repeatable-application-field-value
  [check-schema-interceptor]
  (fn [db [_ field-descriptor question-group-idx data-idx]]
    (remove-repeatable-field-value db field-descriptor question-group-idx data-idx)))

(defn default-error-handler [db [_ response]]
  (when (>= (:status response) 500)
    (dispatch [:add-toast-message "Taustapalvelukutsu epäonnistui: "  (:status response) " " (:response response)]))
  (response->error-message db response))

(reg-event-db
  :application/run-rules
  [check-schema-interceptor]
  (fn [db [_ rules]]
    (if (#{:submitting :submitted} (-> db :application :submit-status))
      db
      (rules/run-rules db rules))))

(reg-event-db
  :application/default-handle-error
  [check-schema-interceptor]
  default-error-handler)

(reg-event-db
  :application/default-http-ok-handler
  [check-schema-interceptor]
  (fn [db _] db))

(reg-event-db
  :application/default-http-progress-handler
  [check-schema-interceptor]
  (fn [db _] db))

(reg-event-db
  :state-update
  [check-schema-interceptor]
  (fn [db [_ f]]
    (or (f db)
        db)))

(reg-event-db
  :application/handle-postal-code-input
  [check-schema-interceptor]
  (fn [db [_ response]]
    (let [id    :postal-office
          value (autil/non-blank-val (:body response) [(-> db :form :selected-language) :fi :sv :en])]
      (-> db
                     (set-repeatable-field-values id nil nil value)
                     (set-repeatable-field-value id)
                     (set-repeatable-application-repeated-field-valid id nil nil true)
                     (set-repeatable-application-field-top-level-valid id true)))))

(reg-event-db
  :application/handle-postal-code-error
  [check-schema-interceptor]
  (fn [db _]
    (let [id :postal-office]
      (-> db
          (set-repeatable-field-values id nil nil "")
          (set-repeatable-field-value id)
          (set-repeatable-application-repeated-field-valid id nil nil false)
          (set-repeatable-application-field-top-level-valid id false)
          (update-in [:application :answers :postal-code]
                     merge {:valid  false
                            :errors [(validation-error/person-info-validation-error :postal-office-missing)]})))))

(reg-event-fx
  :application/set-multiple-choice-option-valid
  [check-schema-interceptor]
  (fn [{:keys [db]} [_ field-descriptor group-idx option-value valid? errors]]
    (let [id (keyword (:id field-descriptor))]
      {:db         (-> db
                       (set-multiple-choice-option-valid id group-idx option-value valid?)
                       (set-repeatable-application-field-top-level-valid id valid?)
                       (assoc-in [:application :answers id :errors] errors))
       :dispatch-n [[:application/set-validator-processed id]
                    [:application/run-rules (:rules field-descriptor)]]})))

(reg-event-fx
  :application/toggle-multiple-choice-option
  [check-schema-interceptor]
  (fn [{db :db} [_ field-descriptor question-group-idx option]]
    (let [id (keyword (:id field-descriptor))
          db (-> db
                 (toggle-multiple-choice-option field-descriptor
                                                question-group-idx
                                                (:value option))
                 (set-repeatable-field-value id)
                 (field-visibility/set-field-visibility field-descriptor)
                 (set-validator-processing id)
                 (rules/run-pohjakoulutusristiriita-rule))]
      {:db                 db
       :dispatch           [:application/set-followup-values field-descriptor]
       :validate-debounced {:value                        (if (some? question-group-idx)
                                                            (get-in db [:application :answers id :value question-group-idx])
                                                            (get-in db [:application :answers id :value]))
                            :priorisoivat-hakukohderyhmat (get-in db [:form :priorisoivat-hakukohderyhmat])
                            :answers-by-key               (get-in db [:application :answers])
                            :field-descriptor             field-descriptor
                            :editing?                     (get-in db [:application :editing?])
                            :virkailija?                  (contains? (:application db) :virkailija-secret)
                            :on-validated                 (fn [[valid? errors]]
                                                            (dispatch [:application/set-multiple-choice-option-valid
                                                                       field-descriptor
                                                                       question-group-idx
                                                                       (:value option)
                                                                       valid?
                                                                       errors]))}})))

(reg-event-fx
  :application/add-adjacent-fields
  [check-schema-interceptor]
  (fn [{db :db} [_ field-descriptor question-group-idx]]
    {:dispatch-n (mapv (fn [child]
                         (let [id      (keyword (:id child))
                               new-idx (count (if (some? question-group-idx)
                                                (get-in db [:application :answers id :values question-group-idx])
                                                (get-in db [:application :answers id :values])))]
                           [:application/set-repeatable-application-field
                            child question-group-idx new-idx ""]))
                       (:children field-descriptor))}))

(reg-event-db
  :application/remove-adjacent-field
  [check-schema-interceptor]
  (fn [db [_ field-descriptor question-group-idx row-idx]]
    (reduce #(remove-repeatable-field-value %1 %2 question-group-idx row-idx)
                       db
                       (:children field-descriptor))))

(defonce max-attachment-size-bytes
  (get (js->clj js/config) "attachment-file-max-size-bytes" (* 10 1024 1024)))

(reg-event-fx
  :application/start-attachment-upload
  [check-schema-interceptor]
  (fn [{:keys [db]} [_ field-descriptor question-group-idx attachment-idx file retries]]
    (resumable-upload/upload-file
      "/hakemus/api/files/signed-upload"
      "/hakemus/api/files/mark-upload-delivered"
      file
      (:id field-descriptor)
      attachment-idx
      (get-in db [:application :attachments-id])
      {:handler          [:application/handle-attachment-upload field-descriptor question-group-idx attachment-idx]
       :error-handler    [:application/handle-attachment-upload-error field-descriptor question-group-idx attachment-idx file (inc retries)]
       :progress-handler [:application-file-upload/handle-attachment-progress-resumable field-descriptor attachment-idx question-group-idx]
       :started-handler  [:application/handle-attachment-upload-started field-descriptor question-group-idx attachment-idx]})))

#_{:clj-kondo/ignore [:dfreeman.re-frame/sub-in-event-handler]}
; TODO: Replace subscribe call in this event handler with a selector function call
(reg-event-fx
  :application/add-single-attachment-resumable
  [check-schema-interceptor]
  (fn [{:keys [db]} [_ field-descriptor question-group-idx attachment-idx file retries]] 
    (let [id       (keyword (:id field-descriptor)) 
          filename (:filename @(subscribe [:application/answer
                                           id
                                           question-group-idx
                                           attachment-idx]))]
      {:db       (assoc-in db [:attachments-uploading id filename] :downloading)
       :dispatch [:application/start-attachment-upload field-descriptor question-group-idx attachment-idx file retries]})))

(reg-event-fx
  :application/add-attachments
  [check-schema-interceptor]
  (fn [{:keys [db]} [_ field-descriptor question-group-idx files]]
    (let [id                   (keyword (:id field-descriptor))
          path                 (cond-> [:application :answers id :values]
                                       (some? question-group-idx)
                                       (conj question-group-idx))
          existing-attachments (get-in db path)
          new-files            (remove (fn [file]
                                         (some #(and (= (.-name file) (:filename %))
                                                     (= (.-size file) (:size %)))
                                               (filter #(not= (:status %) :deleting) existing-attachments)))
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
      {:db         (-> (if (some? question-group-idx)
                         (-> db
                             (update-in [:application :answers id :values] (util/vector-of-length (inc question-group-idx)))
                             (update-in [:application :answers id :values question-group-idx] #(if (nil? %)
                                                                                                 []
                                                                                                 %))
                             (update-in [:application :answers id :values question-group-idx] into new-attachments))
                         (update-in db [:application :answers id :values] into new-attachments))
                       (set-repeatable-field-value id)
                       (set-repeatable-application-field-top-level-valid id true))
       :dispatch-n (keep-indexed (fn [idx file]
                                   (when (< 0 (.-size file) (inc max-attachment-size-bytes))
                                     [:application/add-single-attachment-resumable
                                      field-descriptor
                                      question-group-idx
                                      (+ (count existing-attachments) idx)
                                      file
                                      0]))
                                 new-files)})))

(reg-event-fx
  :application/set-attachment-valid
  [check-schema-interceptor]
  (fn [{:keys [db]} [_ field-descriptor valid? errors]]
    (let [id (keyword (:id field-descriptor))]
      {:db         (-> db
                       (set-repeatable-application-field-top-level-valid id valid?)
                       (assoc-in [:application :answers id :errors] errors))
       :dispatch-n [[:application/set-validator-processed id]
                    [:application/run-rules (:rules field-descriptor)]]})))

#_{:clj-kondo/ignore [:dfreeman.re-frame/sub-in-event-handler]}
; TODO: Replace subscribe call in this event handler with a selector function call
(reg-event-fx
  :application/handle-attachment-upload
  [check-schema-interceptor]
  (fn [{db :db} [_ field-descriptor question-group-idx attachment-idx response]]
    (let [id       (keyword (:id field-descriptor))
          path     (if question-group-idx
                     [:application :answers id :values question-group-idx attachment-idx]
                     [:application :answers id :values attachment-idx])
          filename (:filename @(subscribe [:application/answer
                                           id
                                           question-group-idx
                                           attachment-idx]))
          new-db   (-> db
                       (update-in [:attachments-uploading id] dissoc filename)
                       (assoc-in path {:value    (:key response)
                                       :filename (:filename response)
                                       :size     (:size response)
                                       :status   :ready
                                       :valid    true})
                       (set-repeatable-field-value id)
                       (set-validator-processing id))]
      {:db                 new-db
       :validate-debounced {:value                        (if (some? question-group-idx)
                                                            (get-in new-db [:application :answers id :value question-group-idx])
                                                            (get-in new-db [:application :answers id :value]))
                            :priorisoivat-hakukohderyhmat (get-in db [:form :priorisoivat-hakukohderyhmat])
                            :answers-by-key               (get-in db [:application :answers])
                            :field-descriptor             field-descriptor
                            :editing?                     (get-in db [:application :editing?])
                            :virkailija?                  (contains? (:application db) :virkailija-secret)
                            :on-validated                 (fn [[valid? errors]]
                                                            (dispatch [:application/set-attachment-valid
                                                                       field-descriptor
                                                                       valid?
                                                                       errors]))}})))

(reg-event-db
  :application/handle-attachment-upload-started
  [check-schema-interceptor]
  (fn [db [_ field-descriptor question-group-idx attachment-idx request]]
    (let [id (keyword (:id field-descriptor))]
      (if (some? question-group-idx)
        (assoc-in db [:application :answers id :values question-group-idx attachment-idx :request] request)
        (assoc-in db [:application :answers id :values attachment-idx :request] request)))))

#_{:clj-kondo/ignore [:dfreeman.re-frame/sub-in-event-handler]}
; TODO: Replace subscribe call in this event handler with a selector function call
(reg-event-fx
  :application/handle-attachment-upload-error
  [check-schema-interceptor]
  (fn [{:keys [db]} [_ field-descriptor question-group-idx attachment-idx file retries response-status]]
    (let [id            (keyword (:id field-descriptor))
          current-error (case response-status
                          ;; misc error in resumable file transfer, retry:
                          409 :file-upload-retransmit
                          ;; rate limited:
                          429 :file-upload-failed
                          ;; forbidden file type:
                          400 :file-type-forbidden
                          ;; generic error, e.g. transfer interrupted:
                          :file-upload-error)]
      
      (if (and (contains? #{:file-upload-failed :retransmit} current-error) (< retries 3))
        {:db               db
         :delayed-dispatch {:dispatch-vec [:application/add-single-attachment-resumable
                                           field-descriptor
                                           question-group-idx
                                           attachment-idx
                                           file
                                           retries]
                            :timeout      (+ 2000 (rand-int 2000))}}
        {:db (-> db
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
                             :errors [[current-error]]}))}))))

(reg-event-fx
  :application/handle-attachment-delete
  [check-schema-interceptor]
  (fn [{db :db} [_ field-descriptor question-group-idx attachment-idx attachment-key _]]
    (let [id     (keyword (:id field-descriptor))
          new-db (-> (if (some? question-group-idx)
                       (update-in db [:application :answers id :values] (util/vector-of-length (inc question-group-idx)))
                       db)
                     (update-in (cond-> [:application :answers id :values]
                                  (some? question-group-idx)
                                  (conj question-group-idx))
                                (fn [values]
                                  (if (some? attachment-idx)
                                    (autil/remove-nth values attachment-idx)
                                    (vec (remove #(= attachment-key (:value %)) values)))))
                     (set-repeatable-field-value id)
                     (set-validator-processing id))]

      {:db                 new-db
       :validate-debounced {:value                        (if (some? question-group-idx)
                                                            (get-in new-db [:application :answers id :value question-group-idx])
                                                            (get-in new-db [:application :answers id :value]))
                            :priorisoivat-hakukohderyhmat (get-in db [:form :priorisoivat-hakukohderyhmat])
                            :answers-by-key               (get-in db [:application :answers])
                            :field-descriptor             field-descriptor
                            :editing?                     (get-in db [:application :editing?])
                            :virkailija?                  (contains? (:application db) :virkailija-secret)
                            :on-validated                 (fn [[valid? errors]]
                                                            (dispatch [:application/set-attachment-valid
                                                                       field-descriptor
                                                                       valid?
                                                                       errors]))}})))

(reg-event-fx
  :application/remove-attachment
  [check-schema-interceptor]
  (fn [{:keys [db]} [_ field-descriptor question-group-idx attachment-idx]]
    (let [id   (keyword (:id field-descriptor))
          path (cond-> [:application :answers id :values]
                       (some? question-group-idx)
                       (conj question-group-idx)
                       true
                       (conj attachment-idx))
          key  (get-in db (conj path :value))]
      (merge
       {:db (-> db
                (update-in path merge {:status :deleting
                                       :valid  false})
                (set-repeatable-application-field-top-level-valid id true))}
       {:dispatch [:application/handle-attachment-delete
                   field-descriptor
                   question-group-idx
                   attachment-idx
                   key
                   nil]}))))

(reg-event-fx
  :application/cancel-attachment-upload
  [check-schema-interceptor]
  (fn [{db :db} [_ field-descriptor question-group-idx attachment-idx]]
    {:http-abort (get-in db (cond-> [:application :answers (keyword (:id field-descriptor)) :values]
                                    (some? question-group-idx)
                                    (conj question-group-idx)
                                    true
                                    (conj attachment-idx :request)))}))

(reg-event-db
  :application/rating-hover
  [check-schema-interceptor]
  (fn [db [_ star-number]]
    (assoc-in db [:application :feedback :star-hovered] star-number)))

(reg-event-db
  :application/rating-submit
  [check-schema-interceptor]
  (fn [db [_ star-number]]
    (-> db
        (assoc-in [:application :feedback :stars] star-number)
        (assoc-in [:application :feedback :status] :rating-given))))

(reg-event-db
  :application/rating-update-feedback
  [check-schema-interceptor]
  (fn [db [_ feedback-text]]
    (assoc-in db [:application :feedback :text] feedback-text)))

(reg-event-db
  :application/handle-feedback-submit
  [check-schema-interceptor]
  (fn [db _] db))

(reg-event-fx
  :application/rating-feedback-submit
  [check-schema-interceptor]
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
  [check-schema-interceptor]
  (fn [db _]
    (update-in db [:application :feedback :hidden?] not)))

(reg-event-fx
 :application/redirect-to-maksut
 [check-schema-interceptor]
 (fn [{:keys [db]}]
   (when-let [url (get-in db [:application :submit-details :url])]
     (set! (.. js/window -location -href) url))))

(reg-event-fx
  :application/redirect-to-tunnistautuminen
  [check-schema-interceptor]
  (fn [_ [_ lang]]
    (let [location (.. js/window -location)
          service-url (config/get-public-config [:applicant :service_url])
          target (str service-url "/hakemus/auth/oppija?lang=" lang "&target=" location)]
      (set! (.. js/window -location -href) target)
      nil)))

(reg-event-fx
  :application/redirect-to-logout
  [check-schema-interceptor]
  (fn [_ [_ lang]]
    (let [service-url (config/get-public-config [:applicant :service_url])
          target (str service-url "/hakemus/auth/oppija/logout?lang=" lang)]
      (.removeEventListener js/window "beforeunload" util/confirm-window-close!)
      (set! (.. js/window -location -href) target)
      nil)))
(reg-event-fx
  :application/redirect-to-opintopolku-etusivu
  [check-schema-interceptor]
  (fn [_ [_ lang]]
    (let [service-url (config/get-public-config [:applicant :service_url])
          target (str service-url "/konfo/" lang "/")]
      (.removeEventListener js/window "beforeunload" util/confirm-window-close!)
      (set! (.. js/window -location -href) target)
      nil)))

(reg-event-fx
  :application/redirect-to-oma-opintopolku
  [check-schema-interceptor]
  (fn [_]
    (let [service-url (config/get-public-config [:applicant :service_url])
          target (str service-url "/oma-opintopolku/")]
      (.removeEventListener js/window "beforeunload" util/confirm-window-close!)
      (set! (.. js/window -location -href) target)
      nil)))

(reg-event-db
  :application/set-tunnistautuminen-declined
  [check-schema-interceptor]
  (fn [db _]
    (assoc-in db [:oppija-session :tunnistautuminen-declined] true)))

(reg-event-db
  :application/set-active-notification-modal
  [check-schema-interceptor]
  (fn [db [_ params]]
    (assoc-in db [:application :notification-modal] params)))

(reg-event-db
  :application/toggle-logout-menu
  [check-schema-interceptor]
  (fn [db _]
    (update-in db [:oppija-session :logout-menu-open] not)))

(reg-event-db
  :application/close-session-expires-warning-dialog
  [check-schema-interceptor]
  (fn [db _]
    (assoc-in db [:oppija-session :session-expires-in-minutes-warning] nil)))

(reg-event-fx
  :application/set-page-title
  [check-schema-interceptor]
  (fn [{:keys [db]}]
    (let [lang-kw       (keyword (-> db :form :selected-language))
          title-prefix  (translations/get-hakija-translation :page-title lang-kw)
          title-suffix  (or
                          (lang-kw (-> db :form :tarjonta :haku-name))
                          (-> db :form :name lang-kw))]
      {:db db
       :set-page-title (str title-prefix " – " title-suffix)})))

(reg-event-fx
  :application/add-question-group-row
  [check-schema-interceptor]
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
  [check-schema-interceptor]
  (fn [{:keys [db]} [_ field-descriptor idx]]
    (let [id                   (keyword (:id field-descriptor))
          descendants          (->> (:children field-descriptor)
                                    autil/flatten-form-fields
                                    (filter autil/answerable?))
          count-decremented    (-> db
                                   (update-in [:application :ui id :count] dec)
                                   (update-in [:application :ui id] dissoc :mouse-over-remove-button))
          descendants-modified (reduce (fn [db child]
                                         (let [id (keyword (:id child))]
                                           (-> db
                                               (update-in [:application :answers id :values] (util/vector-of-length (inc idx)))
                                               (update-in [:application :answers id :values] autil/remove-nth idx)
                                               (set-repeatable-field-value id)
                                               (set-repeatable-application-field-top-level-valid id true))))
                                       count-decremented
                                       descendants)]
      {:db         (field-visibility/set-field-visibility
                    descendants-modified
                    field-descriptor)
       :dispatch-n  (conj (mapv (fn [descendant]
                           [:application/run-rules (:rules descendant)])
                         descendants) [:application/handle-section-visibility-conditions])})))

(reg-event-db
  :application/remove-question-group-mouse-over
  [check-schema-interceptor]
  (fn [db [_ field-descriptor idx]]
    (assoc-in db [:application :ui (keyword (:id field-descriptor)) :mouse-over-remove-button idx] true)))

(reg-event-db
  :application/remove-question-group-mouse-out
  [check-schema-interceptor]
  (fn [db [_ field-descriptor idx]]
    (assoc-in db [:application :ui (keyword (:id field-descriptor)) :mouse-over-remove-button idx] false)))

(reg-event-fx
  :application/setup-window-unload
  [check-schema-interceptor]
  (fn [_ _]
    {:set-window-close-callback nil}))
