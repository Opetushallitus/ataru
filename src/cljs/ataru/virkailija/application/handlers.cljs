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
            [ataru.virkailija.application.application-search-control-handlers :as asch]
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
                                          (assoc-in [:application :latest-form] nil)
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

(reg-event-fx
  :application/close-application
  (fn [{db :db} _]
    {:db       (close-application db)
     :dispatch [:application/stop-autosave]}))

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
          (assoc-in [:application :applications] updated-applications)))))

(reg-event-db
  :application/update-review-field
  (fn [db [_ field value]]
    (let [hakukohde-oids (-> db :application :selected-review-hakukohde-oids)]
      (reduce
        (fn [db oid] (update-review-field db field value oid))
        db
        hakukohde-oids))))

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

(reg-event-db
  :application/toggle-filter
  (fn [db [_ filter-id state]]
    (update-in db [:application :filters-checkboxes filter-id state] not)))

(reg-event-fx
  :application/apply-filters
  (fn [{:keys [db]} _]
    {:db       (-> db
                   (assoc-in [:application :filters] (get-in db [:application :filters-checkboxes]))
                   (assoc-in [:application :ensisijaisesti?] (get-in db [:application :ensisijaisesti?-checkbox]))
                   (assoc-in [:application :rajaus-hakukohteella] (get-in db [:application :rajaus-hakukohteella-value])))
     :dispatch [:application/reload-applications]}))

(defn- set-rajaus-hakukohteella
  [db hakukohde-oid]
  (cljs-util/update-url-with-query-params {:rajaus-hakukohteella hakukohde-oid})
  (assoc-in db [:application :rajaus-hakukohteella-value] hakukohde-oid))

(defn- set-ensisijaisesti
  [db ensisijaisesti?]
  (cljs-util/update-url-with-query-params {:ensisijaisesti ensisijaisesti?})
  (cond-> (assoc-in db [:application :ensisijaisesti?-checkbox] ensisijaisesti?)
          (not ensisijaisesti?)
          (set-rajaus-hakukohteella nil)))

(reg-event-db
  :application/set-ensisijaisesti
  (fn [db [_ ensisijaisesti?]] (set-ensisijaisesti db ensisijaisesti?)))

(reg-event-db
  :application/set-rajaus-hakukohteella
  (fn [db [_ hakukohde-oid]] (set-rajaus-hakukohteella db hakukohde-oid)))

(defn- undo-filters
  [db]
  (-> db
      (assoc-in [:application :filters-checkboxes] (get-in db [:application :filters]))
      (set-ensisijaisesti (get-in db [:application :ensisijaisesti?]))
      (set-rajaus-hakukohteella (get-in db [:application :rajaus-hakukohteella]))))

(reg-event-db
  :application/undo-filters
  (fn [db _] (undo-filters db)))

(reg-event-db
  :application/toggle-shown-time-column
  (fn [db _]
    (update-in db [:application :selected-time-column] #(if (= "created-time" %)
                                                          "submitted"
                                                          "created-time"))))

(reg-event-fx
  :application/remove-filters
  (fn [{:keys [db]} _]
    {:db       (-> db
                   (assoc-in [:application :filters] initial-db/default-filters)
                   (assoc-in [:application :filters-checkboxes] initial-db/default-filters)
                   (assoc-in [:application :ensisijaisesti?] false)
                   (assoc-in [:application :ensisijaisesti?-checkbox] false)
                   (assoc-in [:application :rajaus-hakukohteella] nil)
                   (assoc-in [:application :rajaus-hakukohteella-value] nil))
     :dispatch [:application/reload-applications]}))

(reg-event-fx
  :application/update-sort
  (fn [{:keys [db]} [_ column-id]]
    {:db       (update-in db [:application :sort]
                          #(if (= column-id (:order-by %))
                             (update % :order {"desc" "asc" "asc" "desc"})
                             (assoc % :order-by column-id)))
     :dispatch [:application/reload-applications]}))

(defn- keys->str
  [m]
  (into {} (map (fn [[k v]] [(name k) v]) m)))

(defn- add-review-state-counts
  [counts applications selected-hakukohde-oids review-type]
  (reduce (fn [counts application]
            (->> (:application-hakukohde-reviews application)
                 (filter #(and (or (empty? selected-hakukohde-oids)
                                   (contains? selected-hakukohde-oids (:hakukohde %)))
                               (= review-type (:requirement %))))
                 (map :state)
                 distinct
                 (reduce #(update %1 %2 inc) counts)))
          counts
          applications))

(defn- add-attachment-state-counts
  [counts applications selected-hakukohde-oids]
  (reduce (fn [counts application]
            (if-let [states (->> (:application-attachment-reviews application)
                                 (filter #(or (empty? selected-hakukohde-oids)
                                              (contains? selected-hakukohde-oids (:hakukohde %))))
                                 (map :state)
                                 distinct
                                 seq)]
              (reduce #(update %1 %2 inc) counts states)
              (update counts "no-requirements" inc)))
          counts
          applications))

(defn- fetch-applications-fx
  [{db :db :as fx}]
  (let [search-term    (get-in db [:application :search-control :search-term :parsed])
        form           (when-let [form-key (get-in db [:application :selected-form-key])]
                         {:form-key form-key})
        haku           (when-let [haku-oid (get-in db [:application :selected-haku])]
                         {:haku-oid haku-oid})
        hakukohde      (when-let [hakukohde-oid (get-in db [:application :selected-hakukohde])]
                         {:hakukohde-oid hakukohde-oid})
        hakukohderyhma (when-let [[haku-oid hakukohderyhma-oid] (get-in db [:application :selected-hakukohderyhma])]
                         (cond-> {:haku-oid           haku-oid
                                  :hakukohderyhma-oid hakukohderyhma-oid}
                                 (some? (get-in db [:application :rajaus-hakukohteella]))
                                 (assoc :rajaus-hakukohteella (get-in db [:application :rajaus-hakukohteella]))))]
    (if (some identity [search-term form haku hakukohde hakukohderyhma])
      (-> fx
          (assoc-in [:db :application :fetching-applications?] true)
          (assoc :http
                 {:id                  :applications-list
                  :method              :post
                  :path                "/lomake-editori/api/applications/list"
                  :params              (merge {:sort               (get-in db [:application :sort])
                                               :states-and-filters {:attachment-states-to-include (get-in db [:application :attachment-state-filter])
                                                                    :processing-states-to-include (get-in db [:application :processing-state-filter])
                                                                    :selection-states-to-include  (get-in db [:application :selection-state-filter])
                                                                    :filters                      (get-in db [:application :filters])}}
                                              search-term
                                              form
                                              haku
                                              hakukohde
                                              hakukohderyhma
                                              (when (get-in db [:application :ensisijaisesti?])
                                                {:ensisijaisesti true}))
                  :skip-parse-times?   true
                  :skip-flasher?       true
                  :handler-or-dispatch :application/handle-fetch-applications-response}))
      (assoc-in fx [:db :application :fetching-applications?] false))))

(reg-event-fx
  :application/handle-fetch-applications-response
  (fn [{:keys [db]} [_ {:keys [applications sort]}]]
    (let [selected-hakukohde-oids @(subscribe [:application/selected-hakukohde-oid-set])
          all-applications        (let [loaded   (get-in db [:application :applications])
                                        new-keys (set (map :key applications))]
                                    (into (filterv #(not (contains? new-keys (:key %))) loaded)
                                          applications))
          fetch-more?             (and (contains? sort :offset)
                                       (get-in db [:application :fetching-applications?]))
          db                      (-> db
                                      (assoc-in [:application :applications] all-applications)
                                      (update-in [:application :review-state-counts] add-review-state-counts applications selected-hakukohde-oids "processing-state")
                                      (update-in [:application :selection-state-counts] add-review-state-counts applications selected-hakukohde-oids "selection-state")
                                      (update-in [:application :attachment-state-counts] add-attachment-state-counts applications selected-hakukohde-oids)
                                      (assoc-in [:application :sort] sort)
                                      (assoc-in [:application :fetching-applications?] false))
          application-key         (cond
                                    (= 1 (count all-applications))     (-> all-applications first :key)
                                    (-> db :application :selected-key) (-> db :application :selected-key)
                                    :else                              (:application-key (cljs-util/extract-query-params)))]
      (if fetch-more?
        (fetch-applications-fx {:db db})
        {:db       db
         :dispatch (if application-key
                     [:application/select-application application-key nil false]
                     [:application/close-application])}))))

(defn- extract-unselected-review-states-from-query
  [query-params query-param states]
  (-> query-params
      query-param
      (clojure.string/split #",")
      (cljs-util/get-unselected-review-states states)))

(reg-event-db
  :application/set-filters-from-query
  (fn [db _]
    (let [query-params    (cljs-util/extract-query-params)
          ensisijaisesti? (= "true" (:ensisijaisesti query-params))]
      (-> db
          (assoc-in [:application :attachment-state-filter]
                    (extract-unselected-review-states-from-query
                     query-params
                     :attachment-state-filter
                     review-states/attachment-hakukohde-review-types-with-no-requirements))
          (assoc-in [:application :processing-state-filter]
                    (extract-unselected-review-states-from-query
                     query-params
                     :processing-state-filter
                     review-states/application-hakukohde-processing-states))
          (assoc-in [:application :selection-state-filter]
                    (extract-unselected-review-states-from-query
                     query-params
                     :selection-state-filter
                     review-states/application-hakukohde-selection-states))
          (assoc-in [:application :ensisijaisesti?]
                    ensisijaisesti?)
          (assoc-in [:application :rajaus-hakukohteella]
                    (when ensisijaisesti? (:rajaus-hakukohteella query-params)))
          (asch/set-search (or (:term query-params) ""))
          (undo-filters)))))

(reg-event-fx
  :application/reload-applications
  (fn [{:keys [db]} _]
    (let [db (-> db
                 (assoc-in [:application :applications] [])
                 (assoc-in [:application :review-state-counts] (get-in initial-db/default-db [:application :review-state-counts]))
                 (assoc-in [:application :selection-state-counts] (get-in initial-db/default-db [:application :selection-state-counts]))
                 (assoc-in [:application :attachment-state-counts] (get-in initial-db/default-db [:application :attachment-state-counts]))
                 (update-in [:application :sort] dissoc :offset)
                 (assoc-in [:application :fetching-applications?] true))]
      (cond-> {:db       db
               :dispatch [:application/refresh-haut-and-hakukohteet
                          fetch-applications-fx]}
              (some? (get-in db [:request-handles :applications-list]))
              (assoc :http-abort (get-in db [:request-handles :applications-list]))))))

(reg-event-db
  :application/stop-loading-applications
  (fn [db _]
    (assoc-in db [:application :fetching-applications?] false)))

(reg-event-db
  :application/show-more-applications
  (fn [db [_ currently-rendered]]
    (update-in db [:application :applications-to-render] #(min (count (get-in db [:application :applications]))
                                                               (max % (+ currently-rendered 10))))))

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
                                             latest-form
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
      (assoc-in [:application :latest-form] latest-form)
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
          :selected-hakukohderyhma))

(reg-event-fx
  :application/search-all-applications
  (fn [{db :db} [_ search-term]]
    {:db       (clear-selection db)
     :dispatch [:application/search-by-term search-term]}))

(reg-event-fx
  :application/select-form
  (fn [{db :db} [_ form-key]]
    {:db       (-> db
                   clear-selection
                   (assoc-in [:application :selected-form-key] form-key))
     :dispatch [:application/reload-applications]}))

(reg-event-fx
  :application/select-hakukohde
  (fn [{db :db} [_ hakukohde-oid]]
    {:db       (-> db
                   clear-selection
                   (assoc-in [:application :selected-hakukohde] hakukohde-oid))
     :dispatch [:application/reload-applications]}))

(reg-event-fx
  :application/select-hakukohderyhma
  (fn [{db :db} [_ [haku-oid hakukohderyhma-oid]]]
    {:db       (-> db
                   clear-selection
                   (assoc-in [:application :selected-hakukohderyhma] [haku-oid hakukohderyhma-oid]))
     :dispatch [:application/reload-applications]}))

(reg-event-fx
  :application/select-haku
  (fn [{db :db} [_ haku-oid]]
    {:db       (-> db
                   clear-selection
                   (assoc-in [:application :selected-haku] haku-oid))
     :dispatch [:application/reload-applications]}))

(defn- keys-to-names [m] (reduce-kv #(assoc %1 (name %2) %3) {} m))

(reg-event-fx
  :editor/handle-refresh-haut-and-hakukohteet
  (fn [{db :db} [_ {:keys [tarjonta-haut direct-form-haut haut hakukohteet hakukohderyhmat]} {:keys [callback-fx-transformer]}]]
    (callback-fx-transformer
     {:db (-> db
              (assoc-in [:application :haut :tarjonta-haut] (keys-to-names tarjonta-haut))
              (assoc-in [:application :haut :direct-form-haut] (keys-to-names direct-form-haut))
              (assoc-in [:application :forms] (keys-to-names direct-form-haut))
              (update :haut merge (keys-to-names haut))
              (update :hakukohteet merge (keys-to-names hakukohteet))
              (update :hakukohderyhmat merge (keys-to-names hakukohderyhmat))
              (update :fetching-haut dec)
              (update :fetching-hakukohteet dec))})))

(reg-event-fx
  :application/refresh-haut-and-hakukohteet
  (fn [{:keys [db]} [_ callback-fx-transformer]]
    (when (zero? (:fetching-haut db))
      {:db   (-> db
                 (update :fetching-haut inc)
                 (update :fetching-hakukohteet inc))
       :http {:method              :get
              :path                (str "/lomake-editori/api/haut?show-hakukierros-paattynyt="
                                        (boolean (:show-hakukierros-paattynyt db)))
              :handler-or-dispatch :editor/handle-refresh-haut-and-hakukohteet
              :handler-args        {:callback-fx-transformer (or callback-fx-transformer identity)}
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

(reg-event-db
  :application/set-excel-request-included-ids
  (fn [db [_ included-ids]]
    (assoc-in db [:application :excel-request :included-ids] included-ids)))

(reg-event-fx
  :application/submit-mass-information-request
  (fn [{:keys [db]} _]
    (let [message-and-subject (-> db :application :mass-information-request
                                  (select-keys [:message :subject]))
          application-keys    (map :key (get-in db [:application :applications]))]
      {:dispatch [:application/set-mass-information-request-form-state :submitting]
       :http     {:method              :post
                  :path                "/lomake-editori/api/applications/mass-information-request"
                  :params              {:application-keys    application-keys
                                        :message-and-subject message-and-subject}
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
    {:dispatch-n [[:application/set-mass-information-request-message ""]
                  [:application/set-mass-information-request-subject ""]
                  [:application/set-mass-information-request-form-state :enabled]
                  (when-let [current-application (-> db :application :selected-key)]
                    [:application/fetch-application current-application])]
     :db         (update-in db [:application :applications]
                            (partial map #(assoc % :new-application-modifications 0)))}))

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

(reg-event-db
  :application/set-mass-information-request-popup-visibility
  (fn [db [_ visible?]]
    (assoc-in db [:application :mass-information-request :visible?] visible?)))

(reg-event-db
  :application/set-mass-update-popup-visibility
  (fn [db [_ visible?]]
    (assoc-in db [:application :mass-update :visible?] visible?)))

(reg-event-db
  :application/set-excel-popup-visibility
  (fn [db [_ visible?]]
    (assoc-in db [:application :excel-request :visible?] visible?)))

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
  (fn [_ _]
    {:delayed-dispatch {:dispatch-vec [:application/reload-applications]
                        :delay        500}}))

(reg-event-fx
  :application/mass-update-application-reviews
  (fn [{:keys [db]} [_ from-state to-state]]
    {:http {:method              :post
            :params              {:application-keys (map :key (get-in db [:application :applications]))
                                  :from-state       from-state
                                  :to-state         to-state
                                  :hakukohde-oid    (or (-> db :application :rajaus-hakukohteella)
                                                        (-> db :application :selected-hakukohde))}
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

(reg-event-fx
  :application/get-virkailija-texts
  (fn [{:keys [db]} _]
    (if (-> db :editor :virkailija-texts)
      {:db db}
      {:db   db
       :http {:method              :get
              :path                "/lomake-editori/api/applications/virkailija-texts"
              :handler-or-dispatch :application/handle-get-virkailija-texts-response}})))

(reg-event-db
  :application/handle-get-virkailija-texts-response
  (fn [db [_ response]]
    (assoc-in db [:editor :virkailija-texts] response)))

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
                                    (when hakukohde
                                      {:hakukohde hakukohde})
                                    (when state-name
                                      {:state-name (name state-name)}))
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

(reg-event-db :application/toggle-only-selected-hakukohteet
  (fn [db _]
    (update-in db [:application :only-selected-hakukohteet] #(boolean (not %)))))

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

(reg-event-db
  :application/toggle-all-pohjakoulutus-filters
  (fn [db [_ all-enabled?]]
    (update-in
      db
      [:application :filters-checkboxes :base-education]
      (fn [filter-map] (reduce-kv (fn [acc k _] (assoc acc k (not all-enabled?))) {} filter-map)))))

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
          {:dispatch [:application/show-more-applications]}
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
  :store-request-handle-and-abort-ongoing
  (fn [{:keys [db]} [_ request-id request-handle]]
    (when (and request-id request-handle)
      (let [ongoing-request-handle (-> db :request-handles request-id)]
        (cond-> {:db (assoc-in db [:request-handles request-id] request-handle)}
                (some? ongoing-request-handle)
                (merge {:http-abort ongoing-request-handle}))))))

(reg-event-db
  :remove-request-handle
  (fn [db [_ request-id]]
    (if request-id
      (update db :request-handles dissoc request-id)
      db)))

(reg-event-fx
  :application/toggle-show-hakukierros-paattynyt
  (fn toggle-show-hakukierros-paattynyt [{:keys [db]} _]
    {:db       (update db :show-hakukierros-paattynyt not)
     :dispatch [:application/refresh-haut-and-hakukohteet]}))
