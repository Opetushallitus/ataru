(ns ataru.virkailija.application.handlers
  (:require [ataru.virkailija.virkailija-ajax :as ajax]
            [re-frame.core :refer [subscribe dispatch dispatch-sync reg-event-db reg-event-fx]]
            [ataru.virkailija.autosave :as autosave]
            [ataru.virkailija.application-sorting :as application-sorting]
            [ataru.virkailija.virkailija-ajax :refer [http]]
            [ataru.application.review-states :as review-states]
            [ataru.util :as util]
            [ataru.cljs-util :as cljs-util]
            [ataru.virkailija.temporal :as temporal]
            [reagent.core :as r]
            [taoensso.timbre :refer-macros [spy debug]]
            [ataru.feature-config :as fc]
            [ataru.url :as url]
            [camel-snake-kebab.core :as c]
            [camel-snake-kebab.extras :as ce]
            [cljs-time.core :as t]))

(reg-event-fx
  :application/select-application
  (fn [{:keys [db]} [_ application-key]]
    (if (not= application-key (get-in db [:application :selected-key]))
      (let [db (-> db
                   (assoc-in [:application :selected-key] application-key)
                   (assoc-in [:application :selected-application-and-form] nil)
                   (assoc-in [:application :review-comment] nil)
                   (assoc-in [:application :application-list-expanded?] false)
                   (assoc-in [:application :information-request] nil))]
        {:db         db
         :dispatch-n [[:application/stop-autosave]
                      [:application/fetch-application application-key]]}))))

(defn close-application [db]
  (cljs-util/update-url-with-query-params {:application-key nil})
  (-> db
      (assoc-in [:application :selected-review-hakukohde] nil)
      (assoc-in [:application :selected-key] nil)
      (assoc-in [:application :selected-application-and-form] nil)
      (assoc-in [:application :application-list-expanded?] true)))

(reg-event-db
 :application/close-application
 (fn [db [_ _]]
   (close-application db)))

(defn- processing-state-counts-for-application
  [{:keys [application-hakukohde-reviews]}]
  (frequencies
    (map
      :state
      (or
        (->> application-hakukohde-reviews
             (filter #(= "processing-state" (:requirement %)))
             (not-empty))
        [{:requirement "processing-state" :state review-states/initial-application-hakukohde-processing-state}]))))

(defn review-state-counts
  [applications]
  (reduce
    (fn [acc application]
      (merge-with + acc (processing-state-counts-for-application application)))
    {}
    applications))

(defn- update-review-field-of-selected-application-in-list
  [application selected-application-key field value]
  (if (= selected-application-key (:key application))
    (assoc application field value)
    application))

(defn- update-hakukohde-review-field-of-selected-application-in-list
  [application selected-application-key hakukohde review-field state]
  (if (= selected-application-key (:key application))
    (let [hakukohde-reviews             (or (:application-hakukohde-reviews application) [])
          reviews-with-existing-removed (remove
                                          (fn [review]
                                            (and
                                              (= (:requirement review) (name review-field))
                                              (= (:hakukohde review) hakukohde)))
                                          hakukohde-reviews)
          new-review                    {:requirement (name review-field)
                                         :state       state
                                         :hakukohde   hakukohde}]
      (assoc application :application-hakukohde-reviews (conj reviews-with-existing-removed new-review)))
    application))

(reg-event-db
 :application/update-review-field
 (fn [db [_ field value]]
   (let [selected-key           (get-in db [:application :selected-key])
         application-list       (get-in db [:application :applications])
         selected-hakukohde-oid (get-in db [:application :selected-review-hakukohde])
         is-hakukohde-review?   (-> (map first review-states/hakukohde-review-types)
                                    (set)
                                    (contains? field))
         updated-applications   (cond
                                  (some #{field} [:state :score])
                                  (mapv
                                    #(update-review-field-of-selected-application-in-list % selected-key field value)
                                    application-list)

                                  is-hakukohde-review?
                                  (mapv
                                    #(update-hakukohde-review-field-of-selected-application-in-list % selected-key selected-hakukohde-oid field value)
                                    application-list)

                                  :else
                                  application-list)
         db                     (cond-> db
                                  (and (= field :processing-state)
                                       (= value "information-request"))
                                  (assoc-in [:application :information-request :visible?] true))]
     (if is-hakukohde-review?
       (-> db
           (assoc-in [:application :review :hakukohde-reviews (keyword selected-hakukohde-oid) field] value)
           (assoc-in [:application :applications] updated-applications))
       (-> db
           (update-in [:application :review] assoc field value)
           (assoc-in [:application :applications] updated-applications)
           (assoc-in [:application :review-state-counts] (review-state-counts updated-applications)))))))

(defn- update-sort
  [db column-id swap-order?]
  (let [current-applications (get-in db [:application :applications])
        current-sort         (get-in db [:application :sort])
        new-order            (if swap-order?
                               (if (= :ascending (:order current-sort))
                                 :descending
                                 :ascending)
                               (:order current-sort))]
    (if (= column-id (:column current-sort))
      (-> db
          (update-in
            [:application :sort]
            assoc
            :order
            new-order)
          (assoc-in
            [:application :applications]
            (application-sorting/sort-by-column current-applications column-id new-order)))
      (-> db
          (assoc-in
            [:application :sort]
            {:column column-id :order :descending})
          (assoc-in
            [:application :applications]
            (application-sorting/sort-by-column current-applications column-id :descending))))))

(reg-event-db
 :application/update-sort
 (fn [db [_ column-id]]
   (update-sort db column-id true)))

(defn- parse-application-time
  [application]
  (assoc application :created-time (temporal/str->googdate (:created-time application))))

(reg-event-fx
  :application/handle-fetch-applications-response
  (fn [{:keys [db]} [_ {:keys [applications]}]]
    (let [applications-with-times (map parse-application-time applications)
          db (-> db
                 (assoc-in [:application :applications] applications-with-times)
                 (assoc-in [:application :fetching-applications] false)
                 (assoc-in [:application :review-state-counts] (review-state-counts applications-with-times))
                 (assoc-in [:application :sort] application-sorting/initial-sort)
                 (assoc-in [:application :information-request] nil)
                 (update-sort (:column application-sorting/initial-sort) false))
          application-key (if (= 1 (count applications-with-times))
                            (-> applications-with-times first :key)
                            (when-let [query-key (:application-key (cljs-util/extract-query-params))]
                              (some #{query-key} (map :key applications-with-times))))]
      {:db       db
       :dispatch (if application-key
                   [:application/select-application application-key]
                   [:application/close-application])})))

(defn- extract-unselected-review-states-from-query
  [query-param states]
  (-> (cljs-util/extract-query-params)
      query-param
      (clojure.string/split #",")
      (cljs-util/get-unselected-review-states states)))

(defn fetch-applications-fx [db path]
  {:db   (-> db
             (assoc-in [:application :fetching-applications] true)
             (assoc-in [:application :processing-state-filter] (extract-unselected-review-states-from-query
                                                                 :processing-state-filter
                                                                 review-states/application-hakukohde-processing-states))
             (assoc-in [:application :selection-state-filter] (extract-unselected-review-states-from-query
                                                                :selection-state-filter
                                                                review-states/application-hakukohde-selection-states)))
   :dispatch [:application/refresh-haut]
   :http {:method              :get
          :path                path
          :skip-parse-times?   true
          :handler-or-dispatch :application/handle-fetch-applications-response}})

(reg-event-fx
  :application/fetch-applications
  (fn [{:keys [db]} [_ form-key]]
    (fetch-applications-fx db (str "/lomake-editori/api/applications/list?formKey=" form-key))))

(reg-event-fx
  :application/fetch-applications-by-hakukohde
  (fn [{:keys [db]} [_ hakukohde-oid]]
    (fetch-applications-fx db (str "/lomake-editori/api/applications/list?hakukohdeOid=" hakukohde-oid))))

(reg-event-fx
  :application/fetch-applications-by-haku
  (fn [{:keys [db]} [_ haku-oid]]
    (fetch-applications-fx db (str "/lomake-editori/api/applications/list?hakuOid=" haku-oid))))

(reg-event-fx
  :application/fetch-applications-by-term
  (fn [{:keys [db]} [_ term type]]
    (let [query-param (case type
                        :ssn "ssn"
                        :dob "dob"
                        :email "email"
                        :name "name")]
      (fetch-applications-fx db (str "/lomake-editori/api/applications/list?" query-param "=" term)))))

(reg-event-db
 :application/review-updated
 (fn [db [_ response]]
   (assoc-in db [:application :events] (:events response))))

(defn answers-indexed
  "Convert the rest api version of application to a version which application
  readonly-rendering can use (answers are indexed with key in a map)"
  [application]
  (let [answers    (:answers application)
        answer-map (into {} (map (fn [answer] [(keyword (:key answer)) answer])) answers)]
    (assoc application :answers answer-map)))

(defn update-application-details [db {:keys [form
                                             application
                                             events
                                             review
                                             hakukohde-reviews
                                             information-requests
                                             review-notes]}]
  (-> db
      (assoc-in [:application :selected-application-and-form]
        {:form        form
         :application (answers-indexed application)})
      (assoc-in [:application :events] events)
      (assoc-in [:application :review] review)
      (assoc-in [:application :review-notes] review-notes)
      (assoc-in [:application :review :hakukohde-reviews] hakukohde-reviews)
      (update-in [:application :selected-review-hakukohde] (fn [current-hakukohde]
                                                             (or
                                                               (when (contains? (set (:hakukohde application)) current-hakukohde) current-hakukohde)
                                                               (or (-> application :hakukohde (first)) "form"))))
      (assoc-in [:application :information-requests] information-requests)))

(defn review-autosave-predicate [current prev]
  (if (not= (:id current) (:id prev))
    false
    ;timestamp instances for same timestamp fetched via ajax are not equal :(
    (not= (dissoc current :created-time) (dissoc prev :created-time))))

(defn start-application-review-autosave [db]
  (assoc-in
    db
    [:application :review-autosave]
    (autosave/interval-loop {:subscribe-path [:application :review]
                             :changed-predicate review-autosave-predicate
                             :handler (fn [current _]
                                        (ajax/http
                                          :put
                                          "/lomake-editori/api/applications/review"
                                          :application/review-updated
                                          :override-args {:params (select-keys current [:id
                                                                                        :application-id
                                                                                        :application-key
                                                                                        :score
                                                                                        :state
                                                                                        :hakukohde-reviews])}))})))

(reg-event-fx
  :application/handle-fetch-application-attachment-metadata
  (fn [{:keys [db]} [_ response]]
    (let [response-map       (group-by :key response)
          file-key->metadata (fn file-key->metadata [file-key-or-keys]
                               (if (vector? file-key-or-keys)
                                 (mapv file-key->metadata file-key-or-keys)
                                 (first (response-map file-key-or-keys))))
          set-file-metadata  (fn [answer]
                               (assoc answer :values (-> answer :value file-key->metadata)))
          db                 (->> (get-in db [:application :selected-application-and-form :application :answers])
                                  (map (fn [[_ {:keys [fieldType] :as answer}]]
                                         (cond-> answer
                                           (= fieldType "attachment")
                                           (set-file-metadata))))
                                  (reduce (fn [db {:keys [key] :as answer}]
                                            (assoc-in db [:application :selected-application-and-form :application :answers (keyword key)] answer))
                                          db))]
      {:db       db
       :dispatch [:application/start-autosave]})))

(reg-event-fx
  :application/fetch-application-attachment-metadata
  (fn [{:keys [db]} _]
    (let [query-part (->> (get-in db [:application :selected-application-and-form :application :answers])
                          (filter (comp (partial = "attachment") :fieldType second))
                          (map (comp :value second))
                          (flatten)
                          (url/items->query-part "key")
                          (clojure.string/join))
          path       (str "/lomake-editori/api/files/metadata" query-part)]
      (if (clojure.string/blank? query-part)
        ; sanity check to ensure autosave start in cases of (broken) applications with no values in attachment answer
        {:db       db
         :dispatch [:application/start-autosave]}
        {:db   db
         :http {:method              :get
                :path                path
                :handler-or-dispatch :application/handle-fetch-application-attachment-metadata}}))))

(defn- application-has-attachments? [db]
  (some (comp (partial = "attachment") :fieldType second)
        (get-in db [:application :selected-application-and-form :application :answers])))

(defn- parse-application-times
  [response]
  (let [answers                    (-> response :application :answers)
        form-content               (-> response :form :content)
        without-answers-or-content (-> response
                                       (update-in [:application] dissoc :answers)
                                       (update-in [:form] dissoc :content))
        with-times                 (ataru.virkailija.temporal/parse-times without-answers-or-content)]
    (-> with-times
        (assoc-in [:application :answers] answers)
        (assoc-in [:form :content] form-content))))

(reg-event-fx
  :application/handle-fetch-application
  (fn [{:keys [db]} [_ response]]
    (let [response-with-parsed-times (parse-application-times response)
          db                         (update-application-details db response-with-parsed-times)]
      {:db       db
       :dispatch (if (and (fc/feature-enabled? :attachment)
                          (application-has-attachments? db))
                   [:application/fetch-application-attachment-metadata]
                   [:application/start-autosave])})))

(reg-event-fx
  :application/fetch-application
  (fn [{:keys [db]} [_ application-id]]
    (when-let [autosave (get-in db [:application :review-autosave])]
      (autosave/stop-autosave! autosave))
    (let [db (assoc-in db [:application :review-autosave] nil)]
      {:db   db
       :http {:method              :get
              :path                (str "/lomake-editori/api/applications/" application-id)
              :handler-or-dispatch :application/handle-fetch-application
              :skip-parse-times?   true}})))

(reg-event-db
  :application/start-autosave
  (fn [db _]
    (start-application-review-autosave db)))

(reg-event-fx
  :application/stop-autosave
  (fn [{:keys [db]} _]
    (let [autosave (get-in db [:application :review-autosave])]
      (cond-> {:db db}
        (some? autosave) (assoc :stop-autosave autosave)))))

(reg-event-fx
 :application/clear-applications-haku-and-form-selections
 (fn [{db :db} _]
   (cljs-util/unset-query-param "term")
   {:db (-> db
            (assoc-in [:editor :selected-form-key] nil)
            (assoc-in [:application :applications] nil)
            (assoc-in [:application :search-control :search-term :value] "")
            (update-in [:application] dissoc :selected-form-key :selected-haku :selected-hakukohde))}))

(reg-event-db
 :application/select-form
 (fn [db [_ form-key]]
   (-> db
       (assoc-in [:application :selected-form-key] form-key))))

(reg-event-db
  :application/select-hakukohde
  (fn [db [_ hakukohde]]
    (-> db
        (update-in [:application] dissoc :selected-form-key :selected-haku)
        (assoc-in [:application :selected-hakukohde] hakukohde))))

(reg-event-db
  :application/select-haku
  (fn [db [_ haku]]
    (-> db
        (update :application dissoc :selected-form-key :selected-hakukohde)
        (assoc-in [:application :selected-haku] haku))))

(defn get-hakukohteet-from-haut [haut]
  (->> (:tarjonta-haut haut)
       (map :hakukohteet)
       (flatten)
       (map (fn [hakukohde] [(keyword (:oid hakukohde)) hakukohde]))
       (into {})))

(defn get-forms-from-haut [haut]
  (into {} (map (fn [form-haku] [(:key form-haku) form-haku]) (:direct-form-haut haut))))

(reg-event-db
  :editor/handle-refresh-haut
  (fn [db [_ haut]]
    (-> db
        (assoc-in [:application :haut] haut)
        (assoc-in [:application :hakukohteet] (get-hakukohteet-from-haut haut))
        (assoc-in [:application :forms] (get-forms-from-haut haut)))))

(reg-event-fx
  :application/refresh-haut
  (fn [{:keys [db]}]
    {:db   db
     :http {:method              :get
            :path                "/lomake-editori/api/haut"
            :handler-or-dispatch :editor/handle-refresh-haut
            :skip-parse-times?   true}}))

(reg-event-fx
  :application/navigate
  (fn [{:keys [db]} [_ path]]
    {:db       db
     :navigate path}))

(reg-event-fx
  :application/dispatch
  (fn [{:keys [db]} [_ dispatch-vec]]
    {:db       db
     :dispatch dispatch-vec}))

(reg-event-db
  :application/select-review-hakukohde
  (fn [db [_ selected-hakukohde-oid]]
    (assoc-in db [:application :selected-review-hakukohde] selected-hakukohde-oid)))

(reg-event-db
  :application/set-information-request-subject
  (fn [db [_ subject]]
    (assoc-in db [:application :information-request :subject] subject)))

(reg-event-db
  :application/set-information-request-message
  (fn [db [_ message]]
    (assoc-in db [:application :information-request :message] message)))

(reg-event-fx
  :application/submit-information-request
  (fn [{:keys [db]} _]
    (let [application-key (-> db :application :selected-application-and-form :application :key)]
      {:db   (assoc-in db [:application :information-request :state] :submitting)
       :http {:method              :post
              :path                "/lomake-editori/api/applications/information-request"
              :params              (-> db :application :information-request
                                       (select-keys [:message :subject])
                                       (assoc :application-key application-key))
              :handler-or-dispatch :application/handle-submit-information-request-response}})))

(reg-event-db
  :application/set-information-request-window-visibility
  (fn [db [_ visible?]]
    (assoc-in db [:application :information-request :visible?] visible?)))

(reg-event-fx
  :application/handle-submit-information-request-response
  (fn [{:keys [db]} [_ response]]
    {:db             (-> db
                         (assoc-in [:application :information-request] {:state    :submitted
                                                                        :visible? true})
                         (update-in [:application :information-requests] (fnil identity []))
                         (update-in [:application :information-requests] #(conj % response)))
     :dispatch-later [{:ms       3000
                       :dispatch [:application/reset-submit-information-request-state]}]}))

(reg-event-db
  :application/reset-submit-information-request-state
  (fn [db _]
    (let [application-key (-> db :application :selected-key)]
      (-> db
          (assoc-in [:application :information-request] {:visible? false})
          (update-in [:application :applications] (partial map (fn [application]
                                                                 (cond-> application
                                                                   (= (:key application) application-key)
                                                                   (assoc :new-application-modifications 0)))))))))

(reg-event-fx
  :application/handle-mass-update-application-reviews
  (fn [{:keys [db]} [_ _]]
    (let [db-application (:application db)
          selected-type  @(subscribe [:application/application-list-selected-by])
          selected-id    (if (= :selected-form-key selected-type)
                           (:selected-form-key db-application)
                           (-> db-application selected-type :oid))
          dispatch-kw    (case selected-type
                           :selected-form-key :application/fetch-applications
                           :selected-haku :application/fetch-applications-by-haku
                           :selected-hakukohde :application/fetch-applications-by-hakukohde)]
      (if selected-type
        {:db db
         :dispatch [dispatch-kw selected-id]}
        {:db db}))))

(reg-event-fx
  :application/mass-update-application-reviews
  (fn [{:keys [db]} [_ application-keys from-state to-state]]
    {:db   (assoc-in db [:application :fetching-applications] true)
     :http {:method              :post
            :params              {:application-keys application-keys
                                  :from-state       from-state
                                  :to-state         to-state
                                  :hakukohde-oid    (-> db :application :selected-hakukohde :oid)}
            :path                "/lomake-editori/api/applications/mass-update"
            :handler-or-dispatch :application/handle-mass-update-application-reviews}}))

(reg-event-fx
  :application/resend-modify-application-link
  (fn [{:keys [db]} _]
    (let [application-key (-> db :application :selected-key)]
      {:db   (assoc-in db [:application :modify-application-link :state] :submitting)
       :http {:method              :post
              :params              {:application-key application-key}
              :path                (str "/lomake-editori/api/applications/" application-key "/resend-modify-link")
              :handler-or-dispatch :application/handle-resend-modify-application-link-response}})))

(reg-event-fx
  :application/handle-resend-modify-application-link-response
  (fn [{:keys [db]} [_ response]]
    {:db             (-> db
                         (assoc-in [:application :modify-application-link :state] :submitted)
                         (update-in [:application :events] (fnil identity []))
                         (update-in [:application :events] #(conj % response)))
     :dispatch-later [{:ms       3000
                       :dispatch [:application/fade-out-resend-modify-application-link-confirmation-dialog]}]}))

(reg-event-fx
  :application/fade-out-resend-modify-application-link-confirmation-dialog
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:application :modify-application-link :state] :disappearing)
     :dispatch-later [{:ms 1000
                       :dispatch [:application/reset-resend-modify-application-link-state]}]}))

(reg-event-db
  :application/reset-resend-modify-application-link-state
  (fn [db _]
    (assoc-in db [:application :modify-application-link :state] nil)))

(reg-event-db
  :application/toggle-review-area-settings-visibility
  (fn [db _]
    (let [not-or-true (fnil not false)
          visible?    (-> db :application :review-settings :visible? not-or-true)
          keys->false (partial reduce-kv
                               (fn [config review-key _]
                                 (assoc config review-key false))
                               {})]
      (cond-> (assoc-in db [:application :review-settings :visible?] visible?)
        visible?
        (update-in [:application :ui/review] keys->false)))))

(reg-event-fx
  :application/toggle-review-state-setting
  (fn [{:keys [db]} [_ setting-kwd]]
    (let [not-or-false (fnil not true)
          enabled?     (-> db :application :review-settings :config setting-kwd not-or-false)]
      {:db   (assoc-in db [:application :review-settings :config setting-kwd] :updating)
       :http {:method              :post
              :params              {:setting-kwd setting-kwd
                                    :enabled     enabled?}
              :path                "/lomake-editori/api/applications/review-setting"
              :handler-or-dispatch :application/handle-toggle-review-state-setting-response}})))

(reg-event-db
  :application/handle-toggle-review-state-setting-response
  (fn [db [_ response]]
    (assoc-in db [:application :review-settings :config (-> response :setting-kwd keyword)] (:enabled response))))

(reg-event-fx
  :application/get-virkailija-settings
  (fn [{:keys [db]} _]
    {:db   db
     :http {:method              :get
            :path                "/lomake-editori/api/applications/virkailija-settings"
            :handler-or-dispatch :application/handle-get-virkailija-settings-response}}))

(reg-event-db
  :application/handle-get-virkailija-settings-response
  (fn [db [_ response]]
    (let [review-config (->> response
                             (ce/transform-keys c/->kebab-case-keyword)
                             :review)]
      (update-in db
                 [:application :review-settings :config]
                 merge
                 review-config))))

(reg-event-db
  :application/toggle-review-list-visibility
  (fn [db [_ list-kwd]]
    (update-in db [:application :ui/review list-kwd] (fnil not false))))

(reg-event-fx
  :application/add-review-note
  (fn [{:keys [db]} [_ note]]
    (let [application-key (-> db :application :selected-key)
          note-idx        (-> db :application :review-notes count)
          db              (-> db
                              (update-in [:application :review-notes]
                                         (cljs-util/vector-of-length (inc note-idx)))
                              (assoc-in [:application :review-notes note-idx] {:created-time (t/now)
                                                                               :notes        note
                                                                               :animated?    true})
                              (assoc-in [:application :review-comment] nil))]
      {:db   db
       :http {:method              :post
              :params              {:notes           note
                                    :application-key application-key}
              :path                "/lomake-editori/api/applications/notes"
              :handler-or-dispatch :application/handle-add-review-note-response
              :handler-args        {:note-idx note-idx}}})))

(reg-event-fx :application/handle-add-review-note-response
  (fn [{:keys [db]} [_ resp args]]
    (let [db (update-in db [:application :review-notes (:note-idx args)] merge resp)]
      {:db             db
       :dispatch-later [{:ms 1000 :dispatch [:application/reset-review-note-animations (:note-idx args)]}]})))

(reg-event-db :application/reset-review-note-animations
  (fn [db [_ note-idx]]
    (update-in db [:application :review-notes note-idx] dissoc :animated?)))

(reg-event-db :application/set-review-comment-value
  (fn [db [_ review-comment]]
    (assoc-in db [:application :review-comment] review-comment)))

(reg-event-fx :application/remove-review-note
  (fn [{:keys [db]} [_ note-idx]]
    (let [note-id (-> db :application :review-notes (get note-idx) :id)
          db      (assoc-in db [:application :review-notes note-idx :state] :removing)]
      {:db   db
       :http {:method              :delete
              :path                (str "/lomake-editori/api/applications/notes/" note-id)
              :handler-or-dispatch :application/handle-remove-review-note-response}})))

(reg-event-db :application/handle-remove-review-note-response
  (fn [db [_ resp]]
    (let [note-with-id (comp (partial = (:id resp)) :id)
          remove-note  (comp vec (partial remove note-with-id))]
      (update-in db [:application :review-notes] remove-note))))

(def application-active-state (-> review-states/application-review-states (first) (first)))
(def application-inactive-state (-> review-states/application-review-states (last) (first)))

(reg-event-db
  :application/set-application-activeness
  (fn [db [_ active?]]
    (assoc-in db [:application :review :state] (if active?
                                                 application-active-state
                                                 application-inactive-state))))
