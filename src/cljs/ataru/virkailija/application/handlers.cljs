(ns ataru.virkailija.application.handlers
  (:require [ataru.virkailija.virkailija-ajax :as ajax]
            [re-frame.core :refer [subscribe dispatch dispatch-sync reg-event-db reg-event-fx]]
            [ataru.virkailija.autosave :as autosave]
            [ataru.virkailija.virkailija-ajax :refer [http]]
            [ataru.application.review-states :as review-states]
            [ataru.virkailija.db :as initial-db]
            [ataru.util :as util]
            [ataru.cljs-util :as cljs-util]
            [ataru.virkailija.temporal :as temporal]
            [reagent.core :as r]
            [taoensso.timbre :refer-macros [spy debug]]
            [ataru.feature-config :as fc]
            [ataru.url :as url]
            [camel-snake-kebab.core :as c]
            [camel-snake-kebab.extras :as ce]
            [cljs-time.core :as t]
            [ataru.application.application-states :as application-states]
            [re-frame.core :as re-frame]
            [clojure.core.match :refer [match]]
            [ataru.application.review-states :as review-states]
            [ataru.application.application-states :as application-states]))

(defn- state-filter->query-param
  [db filter all-states]
  (when-let [filters (seq (clojure.set/difference
                           (set (map first all-states))
                           (set (get-in db [:application filter]))))]
    (str (name filter) "=" (clojure.string/join "," filters))))

(defn- applications-link
  [db]
  (let [selected-form           (get-in db [:application :selected-form-key])
        selected-haku           (get-in db [:application :selected-haku])
        selected-hakukohde      (get-in db [:application :selected-hakukohde])
        selected-hakukohderyhma (get-in db [:application :selected-hakukohderyhma])
        term                    (when (= :search-term (get-in db [:application :search-control :show]))
                                  (when-let [term (get-in db [:application :search-control :search-term :value])]
                                    (str "term=" term)))
        application-key         (when-let [application-key (get-in db [:application :selected-key])]
                                  (str "application-key=" application-key))
        ensisijaisesti          (when (or (some? selected-hakukohde)
                                          (some? selected-hakukohderyhma))
                                  (str "ensisijaisesti=" (get-in db [:application :ensisijaisesti?])))
        attachment-state-filter (state-filter->query-param
                                 db
                                 :attachment-state-filter
                                 review-states/attachment-hakukohde-review-types-with-no-requirements)
        processing-state-filter (state-filter->query-param
                                 db
                                 :processing-state-filter
                                 review-states/application-hakukohde-processing-states)
        selection-state-filter  (state-filter->query-param
                                 db
                                 :selection-state-filter
                                 review-states/application-hakukohde-selection-states)
        query-params            (when-let [params (->> [term
                                                        application-key
                                                        ensisijaisesti
                                                        attachment-state-filter
                                                        processing-state-filter
                                                        selection-state-filter]
                                                       (filter some?)
                                                       seq)]
                                  (str "?" (clojure.string/join "&" params)))]
    (cond (some? selected-form)
          (str "/lomake-editori/applications/" selected-form query-params)
          (some? selected-haku)
          (str "/lomake-editori/applications/haku/" selected-haku query-params)
          (some? selected-hakukohde)
          (str "/lomake-editori/applications/hakukohde/" selected-hakukohde query-params)
          (some? selected-hakukohderyhma)
          (str "/lomake-editori/applications/haku/" (first selected-hakukohderyhma)
               "/hakukohderyhma/" (second selected-hakukohderyhma)
               query-params))))

(reg-event-fx
  :application/select-application
  (fn [{:keys [db]} [_ application-key selected-hakukohde-oid with-newest-form?]]
    (let [different-application? (not= application-key (get-in db [:application :selected-key]))]
      (cond
       different-application? (let [db (-> db
                                           (assoc-in [:application :selected-key] application-key)
                                           (assoc-in [:application :selected-application-and-form] nil)
                                           (assoc-in [:application :review-comment] nil)
                                           (assoc-in [:application :application-list-expanded?] false)
                                           (assoc-in [:application :information-request] nil))
                                    db (if selected-hakukohde-oid
                                         (assoc-in db [:application :selected-review-hakukohde-oids] [selected-hakukohde-oid])
                                         db)]
                                {:db         db
                                 :dispatch-n [[:application/stop-autosave]
                                              [:application/fetch-application application-key]]})
       with-newest-form? {:db         (-> db
                                          (assoc-in [:application :selected-application-and-form] nil)
                                          (assoc-in [:application :alternative-form] nil)
                                          (assoc-in [:application :selected-review-hakukohde-oids] [selected-hakukohde-oid]))
                          :dispatch-n [[:application/select-review-hakukohde selected-hakukohde-oid]
                                       [:application/fetch-application application-key true]]}
       selected-hakukohde-oid {:db         (-> db
                                               (assoc-in [:application :selected-review-hakukohde-oids] [selected-hakukohde-oid]))
                               :dispatch-n [[:application/select-review-hakukohde selected-hakukohde-oid]]}
       :else nil))))

(defn close-application [db]
  (cljs-util/update-url-with-query-params {:application-key nil})
  (-> db
      (assoc-in [:application :metadata-not-found] nil)
      (assoc-in [:application :previously-closed-application] (-> db :application :selected-application-and-form :application :key))
      (assoc-in [:application :selected-review-hakukohde-oids] nil)
      (assoc-in [:application :selected-key] nil)
      (assoc-in [:application :selected-application-and-form] nil)
      (assoc-in [:application :application-list-expanded?] true)))

(reg-event-db
  :application/close-application
  (fn [db _]
    (close-application db)))

; TODO REMOVE BEGIN?
(defn- processing-state-counts-for-application
  [{:keys [application-hakukohde-reviews]} included-hakukohde-oid-set]
  (->> (or
         (->> application-hakukohde-reviews
              (filter (fn [review]
                        (and
                          (= "processing-state" (:requirement review))
                          (or (nil? included-hakukohde-oid-set)
                              (contains? included-hakukohde-oid-set (:hakukohde review))))))
              (not-empty))
         [{:requirement "processing-state" :state review-states/initial-application-hakukohde-processing-state}])
       (map :state)
       (frequencies)))

(defn review-state-counts
  [applications]
  (let [included-hakukohde-oid-set @(subscribe [:application/hakukohde-oids-from-selected-hakukohde-or-hakukohderyhma])]
    (reduce
      (fn [acc application]
        (merge-with + acc (processing-state-counts-for-application application included-hakukohde-oid-set)))
      {}
      applications)))

(defn- map-vals-to-zero [m]
  (into {} (for [[k v] m] [k 0])))

(defn attachment-state-counts
  [applications included-hakukohde-oid-set]
  (reduce
    (fn [acc application]
      (merge-with (fn [prev new] (+ prev (if (not-empty new) 1 0)))
                  acc
                  (group-by :state (cond->> (application-states/attachment-reviews-with-no-requirements application)
                                            (some? included-hakukohde-oid-set)
                                            (filter #(contains? included-hakukohde-oid-set (:hakukohde %)))))))
    (map-vals-to-zero review-states/attachment-hakukohde-review-types-with-no-requirements)
    applications))

; TODO REMOVE END?

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

(defn update-review-field [db field value hakukohde-oid]
  (let [selected-key           (get-in db [:application :selected-key])
        application-list       (get-in db [:application :applications])
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
                                  #(update-hakukohde-review-field-of-selected-application-in-list % selected-key hakukohde-oid field value)
                                  application-list)

                                :else
                                application-list)
        db                     (cond-> db
                                       (and (= field :processing-state)
                                            (= value "information-request"))
                                       (assoc-in [:application :information-request :visible?] true))]
    (if is-hakukohde-review?
      (-> db
          (assoc-in [:application :review :hakukohde-reviews (keyword hakukohde-oid) field] value)
          (assoc-in [:application :applications] updated-applications))
      (-> db
          (update-in [:application :review] assoc field value)
          (assoc-in [:application :applications] updated-applications)
          (assoc-in [:application :review-state-counts] (review-state-counts updated-applications))))))


(reg-event-db
  :application/update-review-field
  (fn [db [_ field value]]
    (let [hakukohde-oids       (-> db :application :selected-review-hakukohde-oids)]
      (-> (reduce (fn [db oid] (update-review-field db field value oid)) db hakukohde-oids)
          ;(filter-applications)
          ))))

(defn- update-attachment-hakukohde-review-field-of-selected-application-in-list
  [application selected-application-key hakukohde attachment-key state]
  (if (= selected-application-key (:key application))
    (let [reviews-with-existing-removed (remove
                                          (fn [review]
                                            (and
                                             (= (:attachment-key review) attachment-key)
                                             (= (:hakukohde review) hakukohde)))
                                          (:application-attachment-reviews application))
          new-review                    {:attachment-key attachment-key
                                         :state          state
                                         :hakukohde      hakukohde}]
      (assoc application :application-attachment-reviews (conj reviews-with-existing-removed new-review)))
    application))

(reg-event-db
  :application/update-attachment-review
  (fn [db [_ attachment-key hakukohde-oid state]]
    (let [selected-key           (get-in db [:application :selected-key])
          application-list       (get-in db [:application :applications])
          updated-applications   (mapv
                                   #(update-attachment-hakukohde-review-field-of-selected-application-in-list
                                     % selected-key hakukohde-oid (name attachment-key) state)
                                   application-list)]
      (-> db
          (assoc-in [:application :review :attachment-reviews (keyword hakukohde-oid) attachment-key] state)
          (assoc-in [:application :applications] updated-applications)
          (assoc-in [:application :attachment-state-counts] (attachment-state-counts
                                                              updated-applications
                                                              @(subscribe [:application/hakukohde-oids-from-selected-hakukohde-or-hakukohderyhma])))))))

(defn- update-sort
  [db column-id swap-order?]
  (let [current-sort (get-in db [:application :sort])
        new-order    (if swap-order?
                       (if (= :ascending (:order current-sort))
                         :descending
                         :ascending)
                       (:order current-sort))]
    (if (= column-id (:column current-sort))
      (update-in db
                 [:application :sort]
                 assoc
                 :order
                 new-order)
      (assoc-in db
                [:application :sort]
                {:column column-id :order :descending}))))

(reg-event-fx
  :application/toggle-filter
  (fn [{:keys [db]} [_ filter-id state]]
    (println "toggle" filter-id state (get-in db [:application :filters filter-id state]))
    {:db       (update-in db [:application :filters filter-id state] not)
     :dispatch [:application/update-application-filters]}))

(reg-event-fx
  :application/toggle-shown-time-column
  (fn [{:keys [db]} _]
    (let [new-value (if (= :created-time (-> db :application :selected-time-column))
                      :original-created-time
                      :created-time)]
      {:db       (-> db
                     (assoc-in [:application :selected-time-column] new-value)
                     (update-sort new-value true))
       :dispatch [:application/update-application-filters]})))

(reg-event-fx
  :application/remove-filters
  (fn [{:keys [db]} _]
    (let [initial-filters     (get-in initial-db/default-db [:application :filters])
          all-enabled-filters (clojure.walk/postwalk #(if (boolean? %) true %) initial-filters)]
      {:db       (-> db
                     (assoc-in [:application :filters] all-enabled-filters))
       :dispatch [:application/update-application-filters]})))

(reg-event-fx
  :application/update-sort
  (fn [{:keys [db]} [_ column-id]]
    {:db       (update-sort db column-id true)
     :dispatch [:application/update-application-filters]}))

(defn- keys->str
  [m]
  (into {} (map (fn [[k v]] [(name k) v]) m)))

(reg-event-fx
  :application/handle-fetch-applications-response
  (fn [{:keys [db]} [_ {:keys [applications aggregate-data]}]]
    (let [db              (-> (if (not-empty (-> db :application :applications))
                                (update-in db [:application :applications] into applications)
                                (assoc-in db [:application :applications] applications))
                              (assoc-in [:application :fetching-applications] false)
                              (assoc-in [:application :fetching-next-page] false)
                              (assoc-in [:application :total-count] (:total-count aggregate-data))
                              (assoc-in [:application :filtered-count] (:filtered-count aggregate-data))
                              (assoc-in [:application :review-state-counts] (keys->str (:review-state-counts aggregate-data)))
                              (assoc-in [:application :attachment-state-counts] (keys->str (:attachment-state-counts aggregate-data)))
                              (assoc-in [:application :information-request] nil))
          application-key (cond
                            (= 1 (count applications)) (-> applications first :key)
                            (-> db :application :selected-key) (-> db :application :selected-key)
                            :else (:application-key (cljs-util/extract-query-params)))]
      {:db       db
       :dispatch (if application-key
                   [:application/select-application application-key nil]
                   [:application/close-application])})))

(defn- extract-unselected-review-states-from-query
  [query-param states]
  (-> (cljs-util/extract-query-params)
      query-param
      (clojure.string/split #",")
      (cljs-util/get-unselected-review-states states)))

(defn fetch-applications-fx [db params]
  (let [hakukohde-oids-from-hakukohde-or-ryhma @(subscribe [:application/hakukohde-oids-from-selected-hakukohde-or-hakukohderyhma])
        selected-hakukohteet-set               (cond
                                                 (some? hakukohde-oids-from-hakukohde-or-ryhma)
                                                 hakukohde-oids-from-hakukohde-or-ryhma
                                                 (some? (-> db :application :selected-form-key))
                                                 #{"form"}
                                                 :else
                                                 nil)
        previous-filters                       (-> db :application :previous-fetch-filters)
        previous-params                        (-> db :application :previous-fetch-params)
        previous-sort                          (-> db :application :previous-sort)
        first-load?                            (nil? previous-params)
        reset-filters?                         (and (not first-load?) (not= previous-params params))
        filters                                (-> (if reset-filters? initial-db/default-db db) :application :filters)
        attachment-states-to-include           (-> (if reset-filters? initial-db/default-db db) :application :attachment-state-filter)
        processing-states-to-include           (-> (if reset-filters? initial-db/default-db db) :application :processing-state-filter)
        selection-states-to-include            (-> (if reset-filters? initial-db/default-db db) :application :selection-state-filter)
        sort                                   (-> db :application :sort)
        reset-list?                            (or reset-filters?
                                                   (not= sort previous-sort)
                                                   (not= previous-filters
                                                         [attachment-states-to-include processing-states-to-include selection-states-to-include filters]))
        page                                   (if reset-list? 0 (-> db :application :application-list-page))
        new-db                                 (cond-> (-> db
                                                           (assoc-in [:application :fetching-applications] true)
                                                           (assoc-in [:application :fetching-next-page] (not reset-list?))
                                                           (assoc-in [:application :previous-sort] sort)
                                                           (assoc-in [:application :previous-fetch-params] params)
                                                           (assoc-in [:application :previous-fetch-filters] [attachment-states-to-include
                                                                                                             processing-states-to-include
                                                                                                             selection-states-to-include
                                                                                                             filters])
                                                           (assoc-in [:application :attachment-state-filter] attachment-states-to-include)
                                                           (assoc-in [:application :processing-state-filter] processing-states-to-include)
                                                           (assoc-in [:application :selection-state-filter] selection-states-to-include)
                                                           (assoc-in [:application :filters] filters))
                                                       first-load? (->
                                                                     (assoc-in [:application :attachment-state-filter]
                                                                               (extract-unselected-review-states-from-query
                                                                                 :attachment-state-filter
                                                                                 review-states/attachment-hakukohde-review-types-with-no-requirements))
                                                                     (assoc-in [:application :processing-state-filter]
                                                                               (extract-unselected-review-states-from-query
                                                                                 :processing-state-filter
                                                                                 review-states/application-hakukohde-processing-states))
                                                                     (assoc-in [:application :selection-state-filter]
                                                                               (extract-unselected-review-states-from-query
                                                                                 :selection-state-filter
                                                                                 review-states/application-hakukohde-selection-states)))
                                                       reset-list? (->
                                                                     (assoc-in [:application :application-list-page] 0)
                                                                     (assoc-in [:application :applications] [])))]
    {:db         new-db
     :dispatch-n [(when reset-list? [:application/refresh-haut-and-hakukohteet])]
     :http       {:method              :post
                  :path                "/lomake-editori/api/applications/list"
                  :params              (merge {:page               page
                                               :sort               sort
                                               :states-and-filters {:attachment-states-to-include (-> new-db :application :attachment-state-filter)
                                                                    :processing-states-to-include (-> new-db :application :processing-state-filter)
                                                                    :selection-states-to-include  (-> new-db :application :selection-state-filter)
                                                                    :selected-hakukohteet         selected-hakukohteet-set
                                                                    :filters                      filters}}
                                              params)
                  :skip-parse-times?   true
                  :handler-or-dispatch :application/handle-fetch-applications-response}}))

(reg-event-fx
  :application/update-applications-immediate
  (fn [{:keys [db]} _]
    (fetch-applications-fx
      db
      (-> db :application :previous-fetch-params))))

(reg-event-fx
  :application/update-application-filters
  (fn [_ _]
    {:dispatch-debounced {:id       :update-applications-list
                          :dispatch [:application/update-applications-immediate]
                          :timeout  1000}}))

(reg-event-fx
  :application/fetch-applications
  (fn [{:keys [db]} [_ form-key]]
    (fetch-applications-fx
      db
      {:form-key form-key})))

(reg-event-fx
  :application/fetch-applications-by-hakukohde
  (fn [{:keys [db]} [_ hakukohde-oid]]
    (fetch-applications-fx
     db
     {:hakukohde-oid hakukohde-oid
      :ensisijaisesti (get-in db [:application :ensisijaisesti?] false)})))

(reg-event-fx
  :application/fetch-applications-by-hakukohderyhma
  (fn [{:keys [db]} [_ [haku-oid hakukohderyhma-oid]]]
    (fetch-applications-fx
      db
      {:haku-oid haku-oid
       :hakukohderyhma-oid hakukohderyhma-oid
       :ensisijaisesti (get-in db [:application :ensisijaisesti?] false)
       :rajaus-hakukohteella (get-in db [:application :selected-ryhman-ensisijainen-hakukohde] nil)})))

(reg-event-fx
  :application/fetch-applications-by-haku
  (fn [{:keys [db]} [_ haku-oid]]
    (fetch-applications-fx
      db
      {:haku-oid haku-oid})))

(reg-event-fx
  :application/fetch-applications-by-term
  (fn [{:keys [db]} [_ term type]]
    (fetch-applications-fx
      db
      {type term})))

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

(defn- review-notes-by-hakukohde-and-state-name
  [review-notes]
  (let [notes-by-hakukohde (->> review-notes
                                (filter #(some? (:hakukohde %)))
                                (group-by :hakukohde))]
    (reduce-kv (fn [by-hakukohde hakukohde notes]
                 (let [notes-by-state-name (group-by :state-name notes)]
                   (assoc by-hakukohde
                          (keyword hakukohde)
                          (reduce-kv (fn [by-state-name state-name notes]
                                       (assoc by-state-name (keyword state-name) (-> notes first :notes)))
                                     {}
                                     notes-by-state-name))))
               {}
               notes-by-hakukohde)))

(defn update-application-details [db {:keys [form
                                             alternative-form
                                             application
                                             events
                                             review
                                             hakukohde-reviews
                                             attachment-reviews
                                             information-requests
                                             review-notes]}]
  (-> db
      (assoc-in [:application :selected-application-and-form]
        {:form        form
         :application (answers-indexed application)})
      (assoc-in [:application :alternative-form] alternative-form)
      (assoc-in [:application :events] events)
      (assoc-in [:application :review] review)
      (assoc-in [:application :review-notes] review-notes)
      (assoc-in [:application :notes] (review-notes-by-hakukohde-and-state-name review-notes))
      (assoc-in [:application :review :hakukohde-reviews] hakukohde-reviews)
      (assoc-in [:application :review :attachment-reviews] attachment-reviews)
      (assoc-in [:application :information-requests] information-requests)
      (update-in [:application :selected-review-hakukohde-oids]
        (fn [current-hakukohde-oids]
          (if (and (not-empty (:hakukohde application))
                   (not-empty current-hakukohde-oids)
                   (clojure.set/superset? (set (:hakukohde application))
                                          (set current-hakukohde-oids)))
            current-hakukohde-oids
            [(or (first (:hakukohde application)) "form")])))))

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
                                                                                        :hakukohde-reviews
                                                                                        :attachment-reviews])}))})))

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
  :application/handle-metadata-not-found
  (fn [{:keys [db]} _]
    {:db       (assoc-in db [:application :metadata-not-found] true)
     :dispatch [:application/start-autosave]}))

(reg-event-fx
  :application/fetch-application-attachment-metadata
  (fn [{:keys [db]} _]
    (let [file-keys (->> (get-in db [:application :selected-application-and-form :application :answers])
                         (filter (comp (partial = "attachment") :fieldType second))
                         (map (comp :value second))
                         (flatten))]
      (if (empty? file-keys)
        ; sanity check to ensure autosave starts if application has no attachments
        {:db       db
         :dispatch [:application/start-autosave]}
        {:db   db
         :http {:method              :post
                :path                "/lomake-editori/api/files/metadata"
                :params              {:keys file-keys}
                :override-args {:error-handler #(dispatch [:application/handle-metadata-not-found file-keys])}
                :handler-or-dispatch :application/handle-fetch-application-attachment-metadata}}))))

(defn- application-has-attachments? [db]
  (some (comp (partial = "attachment") :fieldType second)
        (get-in db [:application :selected-application-and-form :application :answers])))

(defn- parse-application-times
  [response]
  (let [answers           (-> response :application :answers)
        tarjonta          (-> response :application :tarjonta)
        form-content      (-> response :form :content)
        without-huge-data (-> response
                              (update-in [:application] dissoc :answers)
                              (update-in [:application] dissoc :tarjonta)
                              (update-in [:form] dissoc :content))
        with-times        (ataru.virkailija.temporal/parse-times without-huge-data)]
    (-> with-times
        (assoc-in [:application :answers] answers)
        (assoc-in [:application :tarjonta] tarjonta)
        (assoc-in [:form :content] form-content))))

(reg-event-fx
  :application/handle-fetch-application
  (fn [{:keys [db]} [_ response]]
    (let [response-with-parsed-times (parse-application-times response)
          db                         (-> db
                                         (update-application-details response-with-parsed-times)
                                         (assoc-in [:application :loading?] false))]
      {:db         db
       :dispatch-n [(if (application-has-attachments? db)
                      [:application/fetch-application-attachment-metadata]
                      [:application/start-autosave])
                    [:application/get-application-change-history (-> response :application :key)]]})))

(reg-event-db
  :application/handle-fetch-application-error
  (fn [db _]
    (assoc-in db [:application :loading?] false)))

(reg-event-fx
  :application/fetch-application
  (fn [{:keys [db]} [_ application-id newest-form?]]
    (when-let [autosave (get-in db [:application :review-autosave])]
      (autosave/stop-autosave! autosave))
    (let [db (-> db
                 (assoc-in [:application :review-autosave] nil)
                 (assoc-in [:application :loading?] true))]
      {:db   db
       :http {:method              :get
              :path                (str "/lomake-editori/api/applications/" application-id
                                        (when newest-form? "?newest-form=true"))
              :handler-or-dispatch :application/handle-fetch-application
              :override-args       {:error-handler #(dispatch [:application/handle-fetch-application-error])}
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

(defn- clear-selection
  [db]
  (update db :application dissoc
          :selected-form-key
          :selected-haku
          :selected-hakukohde
          :selected-hakukohderyhma
          :selected-ryhman-ensisijainen-hakukohde))

(reg-event-fx
  :application/clear-applications-haku-and-form-selections
  (fn [{db :db} _]
    {:db       (clear-selection db)
     :dispatch [:application/search-by-term ""]}))

(reg-event-db
  :application/select-form
  (fn [db [_ form-key]]
    (-> db
        clear-selection
        (assoc-in [:application :selected-form-key] form-key))))

(reg-event-db
  :application/select-ryhman-ensisijainen-hakukohde
  (fn [db [_ hakukohde-oid]]
    (-> db
        (assoc-in [:application :selected-ryhman-ensisijainen-hakukohde] hakukohde-oid))))

(reg-event-db
  :application/select-hakukohde
  (fn [db [_ hakukohde-oid]]
    (-> db
        clear-selection
        (assoc-in [:application :selected-hakukohde] hakukohde-oid))))

(reg-event-db
  :application/select-hakukohderyhma
  (fn [db [_ [haku-oid hakukohderyhma-oid]]]
    (-> db
        clear-selection
        (assoc-in [:application :selected-hakukohderyhma] [haku-oid hakukohderyhma-oid]))))

(reg-event-db
  :application/select-haku
  (fn [db [_ haku-oid]]
    (-> db
        clear-selection
        (assoc-in [:application :selected-haku] haku-oid))))

(defn- set-ensisijaisesti
  [db ensisijaisesti?]
  (assoc-in db [:application :ensisijaisesti?] ensisijaisesti?))

(reg-event-db
  :application/set-ensisijaisesti
  (fn [db [_ ensisijaisesti?]]
    (set-ensisijaisesti db ensisijaisesti?)))

(reg-event-fx
  :application/navigate-to-ensisijaisesti
  (fn [{:keys [db]} [_ ensisijaisesti?]]
    {:navigate (applications-link (set-ensisijaisesti db ensisijaisesti?))}))

(defn- keys-to-names [m] (reduce-kv #(assoc %1 (name %2) %3) {} m))

(reg-event-db
  :editor/handle-refresh-haut-and-hakukohteet
  (fn [db [_ {:keys [tarjonta-haut direct-form-haut haut hakukohteet hakukohderyhmat]}]]
    (-> db
        (assoc-in [:application :haut :tarjonta-haut] (keys-to-names tarjonta-haut))
        (assoc-in [:application :haut :direct-form-haut] (keys-to-names direct-form-haut))
        (assoc-in [:application :forms] (keys-to-names direct-form-haut))
        (update :haut merge (keys-to-names haut))
        (update :hakukohteet merge (keys-to-names hakukohteet))
        (update :hakukohderyhmat merge (keys-to-names hakukohderyhmat))
        (update :fetching-haut dec)
        (update :fetching-hakukohteet dec))))

(reg-event-fx
  :application/refresh-haut-and-hakukohteet
  (fn [{:keys [db]}]
    (when (zero? (:fetching-haut db))
      {:db   (-> db
                 (update :fetching-haut inc)
                 (update :fetching-hakukohteet inc))
       :http {:method              :get
              :path                "/lomake-editori/api/haut"
              :handler-or-dispatch :editor/handle-refresh-haut-and-hakukohteet
              :skip-parse-times?   true
              :cache-ttl           (* 1000 60 5)}})))

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
    (update-in db [:application :selected-review-hakukohde-oids]
      (fn [hakukohde-oids]
        (if (contains? (set hakukohde-oids) selected-hakukohde-oid)
          (filter #(not= selected-hakukohde-oid %) hakukohde-oids)
          (cons selected-hakukohde-oid hakukohde-oids))))))

(reg-event-db
  :application/set-mass-information-request-form-state
  (fn [db [_ state]]
    (assoc-in db [:application :mass-information-request :form-status] state)))

(reg-event-fx
  :application/cancel-mass-information-request
  (fn [{:keys [db]} _]
    (when (= :confirm (get-in db [:application :mass-information-request :form-status]))
      {:dispatch [:application/set-mass-information-request-form-state :enabled]})))

(reg-event-fx
  :application/confirm-mass-information-request
  (fn [_ _]
    {:dispatch       [:application/set-mass-information-request-form-state :confirm]
     :dispatch-later [{:dispatch [:application/cancel-mass-information-request]
                       :ms       3000}]}))

(reg-event-db
  :application/set-mass-information-request-subject
  (fn [db [_ subject]]
    (cond-> (assoc-in db [:application :mass-information-request :subject] subject)
            (not= :enabled (-> db :application :mass-information-request :form-status))
            (assoc-in [:application :mass-information-request :form-status] :enabled))))

(reg-event-db
  :application/set-mass-information-request-message
  (fn [db [_ message]]
    (cond-> (assoc-in db [:application :mass-information-request :message] message)
            (not= :enabled (-> db :application :mass-information-request :form-status))
            (assoc-in [:application :mass-information-request :form-status] :enabled))))

(reg-event-fx
  :application/submit-mass-information-request
  (fn [{:keys [db]} [_ application-keys]]
    (let [message-and-subject (-> db :application :mass-information-request
                                  (select-keys [:message :subject]))
          requests            (map #(assoc message-and-subject :application-key %) application-keys)]
      {:dispatch [:application/set-mass-information-request-form-state :submitting]
       :http     {:method              :post
                  :path                "/lomake-editori/api/applications/mass-information-request"
                  :params              requests
                  :handler-or-dispatch :application/handle-submit-mass-information-request-response}})))

(reg-event-fx
  :application/handle-submit-mass-information-request-response
  (fn [_ _]
    {:dispatch       [:application/set-mass-information-request-form-state :submitted]
     :dispatch-later [{:ms       3000
                       :dispatch [:application/reset-submit-mass-information-request-state]}]}))

(reg-event-fx
  :application/reset-submit-mass-information-request-state
  (fn [{:keys [db]} _]
    (let [current-application (-> db :application :selected-key)
          application-keys    (->> @(subscribe [:application/filtered-applications])
                                   (map :key)
                                   set)]
      {:dispatch-n [[:application/set-mass-information-request-message ""]
                    [:application/set-mass-information-request-subject ""]
                    [:application/set-mass-information-request-form-state :enabled]
                    (when current-application [:application/fetch-application current-application])]
       :db         (-> db
                       (update-in
                         [:application :applications]
                         (partial map (fn [application]
                                        (cond-> application
                                                (contains? application-keys (:key application))
                                                (assoc :new-application-modifications 0))))))})))

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
    (let [selected-type  @(subscribe [:application/application-list-selected-by])
          dispatch-kw    (case selected-type
                           :selected-form-key :application/fetch-applications
                           :selected-haku :application/fetch-applications-by-haku
                           :selected-hakukohde :application/fetch-applications-by-hakukohde
                           nil)]
      (if dispatch-kw
        {:db db
         :dispatch [dispatch-kw (-> db :application selected-type)]}
        {:db db}))))

(reg-event-fx
  :application/mass-update-application-reviews
  (fn [{:keys [db]} [_ application-keys from-state to-state]]
    {:db   (assoc-in db [:application :fetching-applications] true)
     :http {:method              :post
            :params              {:application-keys application-keys
                                  :from-state       from-state
                                  :to-state         to-state
                                  :hakukohde-oid    (-> db :application :selected-hakukohde)}
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
  :application/add-review-notes
  (fn [{:keys [db]} [_ text state-name]]
    (let [selected-hakukohde-oids (get-in db [:application :selected-review-hakukohde-oids])]
      {:db         db
       :dispatch-n (map (fn [hakukohde] [:application/add-review-note text state-name hakukohde]) selected-hakukohde-oids)})))

(reg-event-fx
  :application/add-review-note
  (fn [{:keys [db]} [_ text state-name hakukohde]]
    (let [application-key         (-> db :application :selected-key)
          tmp-id                  (cljs-util/new-uuid)
          note                    (merge {:notes           text
                                          :application-key application-key}
                                    (when state-name
                                      {:hakukohde  hakukohde
                                       :state-name (name state-name)}))
          db                      (-> db
                                      (update-in [:application :review-notes]
                                        (fn [notes]
                                          (vec (cons (merge note
                                                       {:created-time (t/now)
                                                        :id           tmp-id
                                                        :animated?    true})
                                                     notes))))
                                      (assoc-in [:application :review-comment] nil))]
      {:db   db
       :http {:method              :post
              :params              note
              :path                "/lomake-editori/api/applications/notes"
              :handler-or-dispatch :application/handle-add-review-note-response
              :handler-args        {:tmp-id tmp-id}}})))

(reg-event-fx :application/handle-add-review-note-response
  (fn [{:keys [db]} [_ resp {:keys [tmp-id]}]]
    {:db             (update-in db [:application :review-notes]
                                (fn [notes]
                                  (mapv (fn [note]
                                          (if (= tmp-id (:id note))
                                            (merge note resp)
                                            note))
                                        notes)))
     :dispatch-later [{:ms 1000 :dispatch [:application/reset-review-note-animations (:id resp)]}]}))

(reg-event-db :application/reset-review-note-animations
  (fn [db [_ note-id]]
    (update-in db [:application :review-notes]
               (fn [notes]
                 (mapv (fn [note]
                         (if (= note-id (:id note))
                           (dissoc note :animated?)
                           note))
                       notes)))))

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

(reg-event-db
  :application/handle-change-history-response
  (fn [db [_ response]]
    (assoc-in db [:application :selected-application-and-form :application-change-history] response)))

(reg-event-fx
  :application/get-application-change-history
  (fn [{:keys [db]} [_ application-key]]
    {:db   db
     :http {:method              :get
            :path                (str "/lomake-editori/api/applications/" application-key "/changes")
            :handler-or-dispatch :application/handle-change-history-response}}))

(reg-event-db
  :application/open-application-version-history
  (fn [db [_ event]]
    (assoc-in db [:application :selected-application-and-form :selected-event] event)))

(reg-event-db
  :application/close-application-version-history
  (fn [db _]
    (update-in db [:application :selected-application-and-form] dissoc :selected-event)))

(reg-event-db
  :application/remove-field-highlight
  (fn [db [_ field-id]]
    (let [highlighted-fields (-> db :application :selected-application-and-form :highlighted-fields)
          updated-fields     (remove #(= field-id %) highlighted-fields)]
      (assoc-in db [:application :selected-application-and-form :highlighted-fields] updated-fields))))

(reg-event-fx
  :application/highlight-field
  (fn [{:keys [db]} [_ field-id]]
    (.scrollIntoView (.getElementById js/document (name field-id)) (js-obj "behavior" "smooth"))
    {:db (update-in db [:application :selected-application-and-form :highlighted-fields] conj field-id)
     :dispatch-later [{:ms 3000 :dispatch [:application/remove-field-highlight field-id]}]}))

(reg-event-fx
  :application/toggle-all-pohjakoulutus-filters
  (fn [{:keys [db]} [_ all-enabled?]]
    {:db       (update-in db [:application :filters :base-education]
                          (fn [filter-map] (reduce-kv (fn [acc k _] (assoc acc k (not all-enabled?))) {} filter-map)))
     :dispatch [:application/update-application-filters]}))

(reg-event-fx
  :application/navigate-application-list
  (fn [{:keys [db]} [_ step]]
    (let [applications            (-> db :application :applications)
          application-count       (count applications)
          current-application-key (-> db :application :selected-key)
          selected-hakukohde      (-> db :application :selected-hakukohde)
          current-application-idx (util/first-index-of #(= (:key %) current-application-key) applications)
          is-last?                (= current-application-idx (dec application-count))
          next-application-idx    (if is-last?
                                    current-application-idx
                                    (if (nil? current-application-idx)
                                      0
                                      (+ current-application-idx step)))
          guarded-idx             (mod next-application-idx application-count)
          next-application-key    (-> applications (nth guarded-idx) :key)]
      (when next-application-key
        (if is-last?
          {:dispatch [:application/load-next-page]}
          {:update-url-query-params {:application-key next-application-key}
           :dispatch                [:application/select-application next-application-key selected-hakukohde false]})))))

(reg-event-fx
  :application/scroll-list-to-selected-or-previously-closed-application
  (fn [{:keys [db]} _]
    (when-let [application-key (or
                                 (-> db :application :previously-closed-application)
                                 (-> db :application :selected-key))]
      {:db                            (update db :application dissoc :previously-closed-application)
       :scroll-to-application-in-list application-key})))

(reg-event-fx
  :application/load-next-page
  (fn [{:keys [db]} _]
    (let [total-count  (-> db :application :filtered-count)
          loaded-count (-> db :application :applications (count))]
      (when (< loaded-count total-count)
        {:db       (update-in db [:application :application-list-page] inc)
         :dispatch [:application/update-applications-immediate]}))))
