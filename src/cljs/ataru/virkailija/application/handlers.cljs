(ns ataru.virkailija.application.handlers
  (:require [ajax.protocols :as pr]
            [ataru.application.application-states :as application-states]
            [ataru.application.filtering :as application-filtering]
            [ataru.application.review-states :as review-states]
            [ataru.cljs-util :as cljs-util]
            [ataru.component-data.base-education-module-higher :as higher-module]
            [ataru.tarjonta.haku :as haku]
            [ataru.util :as util :refer [assoc?]]
            [ataru.virkailija.application.application-list.virkailija-application-list-handlers :as virkailija-application-list-handlers]
            [ataru.virkailija.application.application-search-control-handlers :as asch]
            [ataru.virkailija.application.application-selectors :refer [get-tutu-form?
                                                                        hakukohde-oids-from-selected-hakukohde-or-hakukohderyhma
                                                                        selected-hakukohde-oid-set]]
            [ataru.virkailija.application.mass-review.virkailija-mass-review-handlers]
            [ataru.virkailija.application.pohjakoulutus-toinen-aste.pohjakoulutus-toinen-aste-handlers :as pohjakoulutus-toinen-aste-handlers]
            [ataru.virkailija.autosave :as autosave]
            [ataru.virkailija.db :as initial-db]
            [ataru.virkailija.editor.editor-selectors :refer [db-all-organizations-have-only-opinto-ohjaaja-rights?]]
            [ataru.virkailija.temporal :as temporal]
            [ataru.virkailija.virkailija-ajax :as ajax]
            [camel-snake-kebab.core :as c]
            [camel-snake-kebab.extras :as ce]
            [cljs-time.core :as t]
            [clojure.set :as clj-set]
            [clojure.string :as clj-string]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx
                                   subscribe]]))

(defn- valintalaskentakoostepalvelu-valintalaskenta-dispatch-vec [db]
  (->> db
       :application
       :selected-application-and-form
       :application
       :hakukohde
       (transduce (comp (filter (fn [hakukohde-oid]
                                  (-> db :application :valintalaskentakoostepalvelu (get hakukohde-oid) :valintalaskenta nil?)))
                        (map (fn [hakukohde-oid]
                               [:virkailija-kevyt-valinta/fetch-valintalaskentakoostepalvelu-valintalaskenta-in-use?
                                {:hakukohde-oid hakukohde-oid}])))
                  conj)))

(defn- hyvaksynnan-ehto-hakemukselle-dispatch [db]
  (let [application-key (get-in db [:application :selected-key])]
    [:hyvaksynnan-ehto/get-ehdot-koko-hakemukselle
     application-key]))

(reg-event-fx
 :application/select-application
 (fn [{:keys [db]} [_ application-key selected-hakukohde-oid with-newest-form?]]
   (let [different-application?     (not= application-key (get-in db [:application :selected-key]))
         selected-hakukohde-oid-set (cond (set? selected-hakukohde-oid) selected-hakukohde-oid
                                          (some? selected-hakukohde-oid) #{selected-hakukohde-oid}
                                          :else #{})]
     (merge
      {:db (cond-> db
             different-application?
             (-> (assoc-in [:application :selected-key] application-key)
                 (assoc-in [:application :selected-application-and-form] nil)
                 (assoc-in [:application :review-comment] nil)
                 (assoc-in [:application :application-list-expanded?] false)
                 (assoc-in [:application :information-request] nil))

             selected-hakukohde-oid
             (assoc-in [:application :selected-review-hakukohde-oids] (vec selected-hakukohde-oid-set))

             with-newest-form?
             (->
              (assoc-in [:application :selected-application-and-form] nil)
              (assoc-in [:application :latest-form] nil)))}
      (when-let [dispatches
                 (cond different-application?
                       [[:application/stop-autosave]
                        [:application/fetch-application application-key with-newest-form?]]

                       with-newest-form?
                       [[:application/select-review-hakukohde (first selected-hakukohde-oid-set)]
                        [:application/fetch-application application-key true]])]
        {:dispatch-n dispatches})))))

(defn- close-application [db]
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
  (into {} (for [[k _] m] [k 0])))

(defn- attachment-state-counts
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

(defn- update-review-field [db field value hakukohde-oid]
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
                                                            (hakukohde-oids-from-selected-hakukohde-or-hakukohderyhma db)))))))

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

(reg-event-fx
 :application/fetch-applications
 (fn [{db :db} [_ {:keys [fetch-valintalaskenta-in-use-and-valinnan-tulos-for-applications?]}]]
   (let [search-term              (get-in db [:application :search-control :search-term :parsed])
         haku                     (when-let [haku-oid (get-in db [:application :selected-haku])]
                                    {:haku-oid haku-oid})
         form                     (when-let [form-key (get-in db [:application :selected-form-key])]
                                    {:form-key form-key})
         hakukohde                (when-let [hakukohde-oid (get-in db [:application :selected-hakukohde])]
                                    {:hakukohde-oid hakukohde-oid})
         hakukohderyhma           (when-let [[haku-oid hakukohderyhma-oid] (get-in db [:application :selected-hakukohderyhma])]
                                    (cond-> {:haku-oid           haku-oid
                                             :hakukohderyhma-oid hakukohderyhma-oid}
                                      (some? (get-in db [:application :rajaus-hakukohteella]))
                                      (assoc :rajaus-hakukohteella (get-in db [:application :rajaus-hakukohteella]))))
         attachment-review-states (get-in db [:application :filters :attachment-review-states])
         question-answer-filter   (get-in db [:application :filters :question-answer-filtering-options])
         option-answers           (map (fn [[field vals]]
                                         {:key field
                                          :options (keep #(when (second %) (first %)) (:options vals))
                                          :use-original-question (:use-original-question vals)
                                          :use-original-followup (:use-original-followup vals)})
                                       question-answer-filter)]
     (if (some identity [search-term form haku hakukohde hakukohderyhma])
       {:db            (assoc-in db [:application :fetching-applications?] true)
        :http          {:id                  :applications-list
                        :method              :post
                        :path                "/lomake-editori/api/applications/list"
                        :params              (merge {:sort                     (get-in db [:application :sort] {:order-by "applicant-name"
                                                                                                                :order    "asc"})
                                                     :attachment-review-states attachment-review-states
                                                     :option-answers           option-answers
                                                     :states-and-filters       {:attachment-states-to-include (get-in db [:application :attachment-state-filter])
                                                                                :processing-states-to-include (get-in db [:application :processing-state-filter])
                                                                                :filters                      (get-in db [:application :filters])
                                                                                :school-filter                (get-in db [:application :school-filter])
                                                                                :classes-of-school            (get-in db [:application :classes-of-school])}}
                                                    search-term
                                                    form
                                                    haku
                                                    hakukohde
                                                    hakukohderyhma
                                                    (when (get-in db [:application :ensisijaisesti?])
                                                      {:ensisijaisesti true}))
                        :skip-parse-times?   true
                        :skip-flasher?       true
                        :handler-or-dispatch :application/handle-fetch-applications-response
                        :override-args {:error-handler (fn [response]
                                                         (when (= (count (str (:status response))) 3)
                                                           (dispatch [:add-toast-message (str "Hakemusten hakeminen epäonnistui, status: " (:status response))]))
                                                         (dispatch [:application/mark-fetch-applications-error]))}
                        :handler-args        {:fetch-valintalaskenta-in-use-and-valinnan-tulos-for-applications?
                                              fetch-valintalaskenta-in-use-and-valinnan-tulos-for-applications?}}}
       {:db (assoc-in db [:application :fetching-applications?] false)}))))

(reg-event-fx
 :application/handle-fetch-applications-response
 (fn [{:keys [db]} [_ {:keys [applications sort]} {:keys [fetch-valintalaskenta-in-use-and-valinnan-tulos-for-applications?]}]]
   (let [selected-hakukohde-oids (selected-hakukohde-oid-set db)
         all-applications        (let [loaded   (get-in db [:application :applications])
                                       new-keys (set (map :key applications))]
                                   (into (filterv #(not (contains? new-keys (:key %))) loaded)
                                         applications))
         fetch-more?             (and (contains? sort :offset)
                                      (get-in db [:application :fetching-applications?]))
         db                      (-> db
                                     (assoc-in [:application :applications] all-applications)
                                     (update-in [:application :review-state-counts] application-filtering/add-review-state-counts applications selected-hakukohde-oids "processing-state")
                                     (update-in [:application :attachment-state-counts] add-attachment-state-counts applications selected-hakukohde-oids)
                                     (assoc-in [:application :sort] sort)
                                     (assoc-in [:application :fetching-applications?] false))
         application-key-param   (:application-key (cljs-util/extract-query-params))
         selected-key            (-> db :application :selected-key)
         application-key         (cond
                                   (= 1 (count all-applications))
                                   (-> all-applications first :key)

                                   (or selected-key
                                       application-key-param)
                                   (-> (filter #(or (= (:key %) selected-key)
                                                    (= (:key %) application-key-param)) all-applications)
                                       (first)
                                       :key))
         applications-with-no-sijoittelu (filter (fn [{haku-oid :haku}]
                                                   (some-> db
                                                           :haut
                                                           (get haku-oid)
                                                           :sijoittelu
                                                           not)) applications)
         fetch-valintalaskenta-in-use-dispatches
         (->> applications-with-no-sijoittelu
              (reduce (fn [acc {hakukohde-oids  :hakukohde}]
                        (-> acc
                            (update
                             :virkailija-kevyt-valinta/fetch-valintalaskentakoostepalvelu-valintalaskenta-in-use?
                             (fnil into #{})
                             hakukohde-oids)))
                      {})
              (mapcat (fn [[event args]]
                        (map (fn [arg]
                               [event
                                (case event
                                  :virkailija-kevyt-valinta/fetch-valintalaskentakoostepalvelu-valintalaskenta-in-use?
                                  {:hakukohde-oid arg})])
                             args)))
              (into []))
         combined-kevyt-valinta-dispatches (cond-> fetch-valintalaskenta-in-use-dispatches
                                             (not-empty applications-with-no-sijoittelu)
                                             (conj [:virkailija-kevyt-valinta/fetch-valinnan-tulos-monelle
                                                    {:application-keys (map :key applications-with-no-sijoittelu)}]))
         dispatches              (as-> [] dispatches'

                                   (if (and fetch-valintalaskenta-in-use-and-valinnan-tulos-for-applications?
                                            (not-empty combined-kevyt-valinta-dispatches))
                                     (into dispatches' combined-kevyt-valinta-dispatches)
                                     (conj dispatches' [:virkailija-kevyt-valinta/filter-applications]))

                                   (if fetch-more?
                                     (conj dispatches' [:application/fetch-applications])
                                     (conj dispatches' (if application-key
                                                         [:application/select-application application-key nil false]
                                                         [:application/close-application]))))]
     {:db         (assoc-in db
                            [:kevyt-valinta :multiple-requests-count]
                            (count combined-kevyt-valinta-dispatches))
      :dispatch-n dispatches})))

(defn- extract-unselected-review-states-from-query
  [query-params query-param states]
  (-> query-params
      query-param
      (clj-string/split #",")
      (cljs-util/get-unselected-review-states states)))

(reg-event-db
 :application/set-filters-from-query
 (fn [db [_ form-key]]
   (let [query-params    (cljs-util/extract-query-params)
         ensisijaisesti? (= "true" (:ensisijaisesti query-params))
         tutu-form?      (get-tutu-form? form-key)
         processing-states (if tutu-form?
                             review-states/application-hakukohde-processing-states
                             review-states/application-hakukohde-processing-states-normal)]
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
                    processing-states))
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
         (virkailija-application-list-handlers/undo-filters)))))

(reg-event-db
 :application/handle-fetch-form-contents
 (fn [db [_ form]]
   (-> db
       (assoc-in [:application :form-key-for-haku] [(get-in db [:application :selected-haku]) (:key form)])
       (assoc-in [:forms (:key form)] form)
       (assoc-in [:forms (:key form) :flat-form-fields] (util/flatten-form-fields (:content form)))
       (assoc-in [:forms (:key form) :form-fields-by-id] (util/form-fields-by-id form)))))

(reg-event-fx
 :application/fetch-form-contents
 (fn [{db :db} _]
   (let [selected-haku (or (get-in db [:application :selected-haku])
                           (get-in db [:hakukohteet (get-in db [:application :selected-hakukohde]) :haku-oid])
                           (get-in db [:application :selected-hakukohderyhma 0]))
         selected-form (or (get-in db [:application :selected-form-key])
                           (get-in db [:haut selected-haku :ataru-form-key]))]
     (if selected-form
       {:http {:method              :get
               :path                (str "/lomake-editori/api/forms/latest/" selected-form)
               :handler-or-dispatch :application/handle-fetch-form-contents
               :skip-parse-times?   true}}
       (when selected-haku
         {:http {:method              :get
                 :path                (str "/lomake-editori/api/forms/latest-by-haku/" selected-haku)
                 :handler-or-dispatch :application/handle-fetch-form-contents
                 :skip-parse-times?   true}})))))

(reg-event-fx
 :application/reload-applications
 (fn [{:keys [db]} [_ user-allowed-fetching?]]
   (if (or user-allowed-fetching?
           (get-in db [:application :search-control :search-term :parsed :application-oid])
           (get-in db [:application :user-allowed-fetching?]))
     (let [haku-oid (or (get-in db [:application :selected-haku])
                        (first (get-in db [:application :selected-hakukohderyhma])))
           hakukohde-oid (get-in db [:application :selected-hakukohde])
           fetch-paattyneet-haut? (not (or (boolean haku-oid) (boolean hakukohde-oid)))]
       (cond-> {:db       (-> db
                              (assoc-in [:application :applications] [])
                              (assoc-in [:application :review-state-counts] (get-in initial-db/default-db [:application :review-state-counts]))
                              (assoc-in [:application :selection-state-counts] (get-in initial-db/default-db [:application :selection-state-counts]))
                              (assoc-in [:application :kevyt-valinta-selection-state-counts] (get-in initial-db/default-db [:application :kevyt-valinta-selection-state-counts]))
                              (assoc-in [:application :kevyt-valinta-vastaanotto-state-counts] (get-in initial-db/default-db [:application :kevyt-valinta-vastaanotto-state-counts]))
                              (assoc-in [:application :attachment-state-counts] (get-in initial-db/default-db [:application :attachment-state-counts]))
                              (update-in [:application :sort] dissoc :offset)
                              (assoc-in [:application :fetching-applications?] true)
                              (assoc-in [:application :user-allowed-fetching?] true)
                              (assoc-in [:application :fetching-applications-errored?] false))
                :dispatch [:application/refresh-haut-and-hakukohteet haku-oid hakukohde-oid fetch-paattyneet-haut? [[:application/fetch-applications
                                                                                                                     {:fetch-valintalaskenta-in-use-and-valinnan-tulos-for-applications? true}]
                                                                                                                    [:application/fetch-form-contents]]]}
         (some? (get-in db [:request-handles :applications-list]))
         (assoc :http-abort (get-in db [:request-handles :applications-list]))))
     {:db db
      :dispatch [:application/fetch-form-contents]})))

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

(defn- parse-rights-by-hakukohde
  [application]
  (assoc application
         :rights-by-hakukohde
         (into {} (map (fn [[key rights]]
                         [(name key) (set (map keyword rights))])
                       (:rights-by-hakukohde application)))))

(defn- add-non-yo-attachment-ids
  [application form]
  (assoc application
         :excluded-attachment-ids-when-yo-and-jyemp
         (higher-module/non-yo-attachment-ids form)))

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

(defn- update-application-details [db {:keys [form
                                              latest-form
                                              application
                                              events
                                              review
                                              hakukohde-reviews
                                              attachment-reviews
                                              information-requests
                                              review-notes
                                              master-oid]}]
  (-> db
      (assoc-in [:application :selected-application-and-form]
                {:form        form
                 :application (-> application
                                  answers-indexed
                                  parse-rights-by-hakukohde
                                  (add-non-yo-attachment-ids form))})
      (assoc-in [:application :latest-form] latest-form)
      (assoc-in [:application :events] events)
      (assoc-in [:application :review] review)
      (assoc-in [:application :review-notes] review-notes)
      (assoc-in [:application :notes] (review-notes-by-hakukohde-and-state-name review-notes))
      (assoc-in [:application :review :hakukohde-reviews] hakukohde-reviews)
      (assoc-in [:application :review :attachment-reviews] attachment-reviews)
      (assoc-in [:application :information-requests] information-requests)
      (assoc-in [:application :selected-application-and-form :application :person :master-oid] master-oid)
      (update-in [:application :selected-review-hakukohde-oids]
                 (fn [current-hakukohde-oids]
                   (let
                    [review-hakukohde-oids-to-keep                 (clj-set/intersection (set (:hakukohde application))
                                                                                         (set current-hakukohde-oids))]
                     (cond
                       (not-empty review-hakukohde-oids-to-keep)

                       review-hakukohde-oids-to-keep

                       (and (not-empty (:hakukohde application))
                            (:selected-hakukohde application))
                       [(:selected-hakukohde application)]

                       :else
                       [(or (first (:hakukohde application)) "form")]))))))

(defn- review-autosave-predicate [current prev]
  (if (not= (:id current) (:id prev))
    false
    ;timestamp instances for same timestamp fetched via ajax are not equal :(
    (not= (dissoc current :created-time) (dissoc prev :created-time))))

(defn- start-application-review-autosave [db]
  (assoc-in
   db
   [:application :review-autosave]
   (autosave/interval-loop {:subscribe-path [:application :review]
                            :changed-predicate review-autosave-predicate
                            :handler (fn [current _]
                                       (let [selected-review-hakukohde-oids @(subscribe [:state-query [:application :selected-review-hakukohde-oids]])
                                             filtered-hakukohde-reviews (select-keys (current :hakukohde-reviews) (map keyword selected-review-hakukohde-oids))]
                                         (ajax/http
                                          :put
                                          "/lomake-editori/api/applications/review"
                                          :application/review-updated
                                          :override-args {:params (merge (select-keys current [:id
                                                                                               :application-id
                                                                                               :application-key
                                                                                               :score
                                                                                               :state
                                                                                               :attachment-reviews])
                                                                         (hash-map :hakukohde-reviews filtered-hakukohde-reviews))})))})))

(reg-event-db
 :application/handle-fetch-application-attachment-metadata
 (fn [db [_ response]]
   (let [metadata (util/group-by-first :key response)]
     (update-in db [:application :selected-application-and-form :application :answers]
                (fn [answers]
                  (->> answers
                       (map (fn [[key answer]]
                              [key (if (= "attachment" (:fieldType answer))
                                     (assoc answer :values
                                            (if (and (seq (:value answer))
                                                     (or (nil? (first (:value answer)))
                                                         (vector? (first (:value answer)))))
                                              (mapv #(when-not (nil? %) (mapv metadata %))
                                                    (:value answer))
                                              (mapv metadata (:value answer))))
                                     answer)]))
                       (into {})))))))

(reg-event-fx
 :application/handle-metadata-not-found
 (fn [{:keys [db]} _]
   {:db       (assoc-in db [:application :metadata-not-found] true)
    :dispatch [:application/start-autosave]}))

(reg-event-fx
 :application/fetch-application-attachment-metadata
 (fn [{:keys [db]} _]
   (let [file-keys (->> (get-in db [:application :selected-application-and-form :application :answers])
                        vals
                        (filter #(= "attachment" (:fieldType %)))
                        (mapcat #(if (and (seq (:value %))
                                          (or (nil? (first (:value %)))
                                              (vector? (first (:value %)))))
                                   (mapcat identity (:value %))
                                   (:value %))))]
     (if (empty? file-keys)
       {:db       db
        :dispatch [:application/start-autosave]}
       {:db       db
        :dispatch [:application/start-autosave]
        :http     {:method              :post
                   :path                "/lomake-editori/api/files/metadata"
                   :params              {:keys file-keys}
                   :override-args       {:error-handler #(dispatch [:application/handle-metadata-not-found file-keys])}
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
        with-times        (temporal/parse-times without-huge-data)]
    (-> with-times
        (assoc-in [:application :answers] answers)
        (assoc-in [:application :tarjonta] tarjonta)
        (assoc-in [:form :content] form-content))))

(reg-event-fx
 :application/fetch-applicant-school
 (fn [_ [_ haku-oid applicant-henkilo-oid hakemus-datetime]]
   {:http {:method              :get
           :path                (str "/lomake-editori/api/applications/opiskelija/" applicant-henkilo-oid
                                     "?haku-oid=" haku-oid "&hakemus-datetime=" hakemus-datetime)
           :handler-or-dispatch :application/handle-fetch-applicant-school-response}}))

(reg-event-db
 :application/handle-fetch-applicant-school-response
 (fn [db [_ response]]
   (let [applicant-oppilaitos-name (:oppilaitos-name response)
         applicant-luokka          (:luokka response)]
     (-> db
         (assoc-in [:application :selected-application-and-form :application :person :oppilaitos-name] applicant-oppilaitos-name)
         (assoc-in [:application :selected-application-and-form :application :person :luokka] applicant-luokka)))))

(defn create-fetch-applicant-school-event-if-toisen-asteen-yhteishaku-or-jatkuva-haku
  [application]
  (when (or
         (haku/toisen-asteen-yhteishaku? (:tarjonta application))
         (haku/jatkuva-haku? (:tarjonta application)))
    [:application/fetch-applicant-school
     (:haku application)
     (-> application :person :oid)
     (:created-time application)]))

(reg-event-fx
 :application/handle-fetch-application
 (fn [{:keys [db]} [_ response]]
   (let [application-key            (-> response :application :key)
         form-key                   (-> response :form :key)
         response-with-parsed-times (parse-application-times response)
         db                         (-> db
                                        (update-application-details response-with-parsed-times)
                                        (assoc-in [:application :loading?] false))
         dispatches                 (vec
                                     (concat
                                      [(if (application-has-attachments? db)
                                         [:application/fetch-application-attachment-metadata]
                                         [:application/start-autosave])
                                       (when (not (db-all-organizations-have-only-opinto-ohjaaja-rights? db))
                                         [:liitepyynto-information-request/get-deadlines application-key])
                                       (when (get-tutu-form? form-key)
                                         [:tutu-payment/fetch-payments application-key])
                                       [:application/get-application-change-history application-key]]
                                      (valintalaskentakoostepalvelu-valintalaskenta-dispatch-vec db)
                                      [(hyvaksynnan-ehto-hakemukselle-dispatch db)]
                                      [[:virkailija-kevyt-valinta/fetch-valinnan-tulos
                                        {:application-key application-key
                                         :memoize         true}]]
                                      [(create-fetch-applicant-school-event-if-toisen-asteen-yhteishaku-or-jatkuva-haku (:application response))]
                                      (pohjakoulutus-toinen-aste-handlers/create-fetch-applicant-pohjakoulutus-event-if-toisen-asteen-yhteishaku (:application response))))]
     {:db         db
      :dispatch-debounced {:timeout 500
                           :id :fetch-attachment-types
                           :dispatch [:editor/fetch-attachment-types-koodisto]}
      :dispatch-n dispatches})))

(reg-event-db
 :application/mark-fetch-applications-error
 (fn [db _]
   (-> db
       (assoc-in [:application :user-allowed-fetching?] false)
       (assoc-in [:application :fetching-applications-errored?] true))))


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
 :mark-refresh-haut-and-hakukohteet-error
 (fn [db _]
   (assoc db :fetching-haut-and-hakukohteet-errored true)))

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
          :applications
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
                  (assoc-in [:application :selected-form-key] form-key)
                  (assoc-in [:application :user-allowed-fetching?] false))
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
                  (assoc-in [:application :selected-hakukohderyhma] [haku-oid hakukohderyhma-oid])) ; ovde selektuje grupu
    :dispatch [:application/reload-applications]}))

(reg-event-fx
 :application/select-haku
 (fn [{db :db} [_ haku-oid]]
   {:db       (-> db
                  clear-selection
                  (assoc-in [:application :selected-haku] haku-oid)
                  (assoc-in [:application :user-allowed-fetching?] false))
    :dispatch [:application/reload-applications]}))

(defn- keys-to-names [m] (reduce-kv #(assoc %1 (name %2) %3) {} m))

(reg-event-fx
 :application/handle-refresh-haut-and-hakukohteet
 (fn [{db :db} [_ {:keys [tarjonta-haut direct-form-haut haut hakukohteet hakukohderyhmat]} {:keys [dispatch-n-after]}]]
   {:db         (-> db
                    (update-in [:application :haut :tarjonta-haut] merge (keys-to-names tarjonta-haut))
                    (update-in [:application :haut :direct-form-haut] merge (keys-to-names direct-form-haut))
                    (update-in [:application :forms] merge (keys-to-names direct-form-haut))
                    (update :haut merge (keys-to-names haut))
                    (update :hakukohteet merge (keys-to-names hakukohteet))
                    (update :hakukohderyhmat merge (keys-to-names hakukohderyhmat))
                    (update :fetching-haut dec)
                    (update :fetching-hakukohteet dec))
    :dispatch-n dispatch-n-after}))

(reg-event-fx
 :application/refresh-haut-and-hakukohteet
 (fn [{:keys [db]} [_ haku-oid hakukohde-oid fetch-paattyneet-haut? dispatch-n-after]]
   {:db   (-> db
              (update :fetching-haut inc)
              (update :fetching-hakukohteet inc)
              (assoc :fetching-haut-and-hakukohteet-errored false))
    :http {:method              :get
           :path                (cond (some? haku-oid)
                                      (str "/lomake-editori/api/haku?haku-oid=" haku-oid)
                                      (some? hakukohde-oid)
                                      (str "/lomake-editori/api/haku?hakukohde-oid=" hakukohde-oid)
                                      :else
                                      (str "/lomake-editori/api/haut?show-hakukierros-paattynyt="
                                           (or fetch-paattyneet-haut? (boolean (:show-hakukierros-paattynyt db)))))
           :handler-or-dispatch :application/handle-refresh-haut-and-hakukohteet
           :handler-args        {:dispatch-n-after dispatch-n-after}
           :skip-parse-times?   true
           :cache-ttl           (* 1000 60 5)
           :override-args       {:error-handler (fn [response]
                                                  (when (= (count (str (:status response))) 3)
                                                    (dispatch [:add-toast-message (str "Haun ja hakukohteiden hakeminen epäonnistui, status: " (:status response))]))
                                                  (dispatch [:mark-refresh-haut-and-hakukohteet-error]))}}}))

(reg-event-fx
 :application/navigate
 (fn [{:keys [db]} [_ path]]
   {:db       db
    :navigate path}))

(reg-event-db
 :application/select-review-hakukohde
 (fn [db [_ selected-hakukohde-oid]]
   (update-in db [:application :selected-review-hakukohde-oids]
              (fn [hakukohde-oids]
                (if (contains? (set hakukohde-oids) selected-hakukohde-oid)
                  (filter #(not= selected-hakukohde-oid %) hakukohde-oids)
                  (cons selected-hakukohde-oid hakukohde-oids))))))

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
                                      (assoc :recipient-target "hakija")
                                      (assoc :application-key application-key)
                                      (assoc :add-update-link true))
             :handler-or-dispatch :application/handle-submit-information-request-response}})))

(reg-event-db
 :application/set-information-request-window-visibility
 (fn [db [_ visible?]]
   (assoc-in db [:application :information-request :visible?] visible?)))


(defn- assoc-in-excel [db k v]
  (assoc-in db (concat [:application :excel-request] (if (vector? k) k [k])) v))

(reg-event-db
 :application/set-excel-popup-visibility
 (fn [db [_ visible?]]
   (assoc-in-excel db :visible? visible?)))

(reg-event-db
 :application/excel-request-filter-changed
 (fn [db [_ id]]
   (js/console.log id)
   (let [filter (get-in db [:application :excel-request :filters id])
         parent-id (:parent-id filter)
         parent-filter (get-in db [:application :excel-request :filters (:parent-id filter)])
         child-ids (:child-ids filter)
         new-checked (not (:checked filter))]
     (as-> db x
       (assoc-in-excel x [:filters id :checked] new-checked)
       ; if checked, then select all children
       (loop [cids child-ids acc x]
         (if (not-empty cids)
           (recur (rest cids) (assoc-in-excel acc [:filters (first cids) :checked] new-checked))
           acc))
       (if parent-filter (let [sibling-checkeds (map (fn [sibling-id] (boolean (get-in x [:application :excel-request :filters sibling-id :checked]))) (:child-ids parent-filter))]
                           (cond (every? true? sibling-checkeds) (assoc-in-excel x [:filters parent-id :checked] true)
                                 (every? false? sibling-checkeds) (assoc-in-excel x [:filters parent-id :checked] false)
                                 :else (-> x
                                           (assoc-in-excel [:filters parent-id :checked] false)
                                           (assoc-in-excel [:filters parent-id :indeterminate] true))))
           x)))))

(reg-event-db
 :application/change-excel-download-mode
 (fn [db [_ selected-mode]]
   (assoc-in-excel db :selected-mode selected-mode)))

(defn download-blob [file-name blob]
  (let [object-url (js/URL.createObjectURL blob)
        anchor-element
        (doto (js/document.createElement "a")
          (-> .-href (set! object-url))
          (-> .-download (set! file-name)))]
    (.appendChild (.-body js/document) anchor-element)
    (.click anchor-element)
    (try
      (.removeChild (.-body js/document) anchor-element)
      (js/URL.revokeObjectURL object-url)
      (catch js/Error e (println e)))))

(reg-event-db
 :application/handle-excel-download-success
 (fn [db [_ response {filename :filename}]]
   (let [new-db (assoc-in-excel db :fetching? false)]
     (try (download-blob filename response)
          (assoc-in-excel new-db :error nil)
          (catch js/Error e (assoc-in-excel new-db :error e))))))

(reg-event-db
 :application/handle-excel-download-error
 (fn [db [_ error]]
   (-> db
       (assoc-in-excel :fetching? false)
       (assoc-in-excel :error error))))

(reg-event-fx
 :application/start-excel-download
 (fn [{:keys [db]} [_ params]]
   (when (not (get-in db [:application :excel-request :fetching?]))
     (let [application-keys (map :key (get-in db [:application :applications]))
           selected-mode (get-in db [:application :excel-request :selected-mode])
           written-ids (get-in db [:application :excel-request :included-ids])
           filtered-ids (->> (get-in db [:application :excel-request :filters])
                             (map second)
                             (filter :checked)
                             (map :id))]
       {:db   (-> db
                  (assoc-in-excel :error nil)
                  (assoc-in-excel :fetching? true))
        :http {:id                  :excel-download
               :method              :post
               :path                "/lomake-editori/api/applications/excel"
               :params              (-> params
                                        (assoc? :application-keys application-keys)
                                        (assoc? :CSRF (cljs-util/csrf-token))
                                        (assoc? :included-ids (->> (case selected-mode
                                                                     "kirjoita-tunnisteet" written-ids
                                                                     "valitse-tiedot" filtered-ids
                                                                     :else ""))))
               :skip-parse-times?   true
               :skip-flasher?       true
               :handler-or-dispatch :application/handle-excel-download-success
               :handler-args        {:filename (:filename params)}
               :override-args       {:response-format {:type :blob
                                                       :read pr/-body}
                                     :error-handler #(dispatch [:application/handle-excel-download-error %])}}}))))

(reg-event-db
 :application/excel-request-filters-init
 (fn [db [_ filter-defs]]
   (let [old-filters (get-in db [:application :excel-request :filters])]
     (assoc-in db [:application :excel-request :filters] (merge-with merge old-filters filter-defs)))))


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
 :application/get-virkailija-texts-from-server
 (fn [_ _]
   {:http {:method              :get
           :path                "/lomake-editori/api/applications/virkailija-texts"
           :handler-or-dispatch :application/handle-get-virkailija-texts-response}}))

(reg-event-fx
 :application/get-virkailija-texts
 (fn [{:keys [db]} _]
   (when-not (-> db :editor :virkailija-texts)
     {:dispatch [:application/get-virkailija-texts-from-server]})))

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
             :path                (str "/lomake-editori/api/applications/notes/" application-key)
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
   (when-let [current-idx (util/first-index-of #(= (:key %) (-> db :application :selected-key))
                                               (-> db :application :applications))]
     (let [applications         (-> db :application :applications)
           filtered-hakukohde   (hakukohde-oids-from-selected-hakukohde-or-hakukohderyhma db)
           next-idx             (mod (+ current-idx step) (count applications))
           next-application-key (-> applications (nth next-idx) :key)
           next-not-visible?    (= next-idx (-> db :application :applications-to-render))]
       (when next-application-key
         {:update-url-query-params {:application-key next-application-key}
          :dispatch-n              [[:application/select-application next-application-key @filtered-hakukohde false]
                                    (when next-not-visible?
                                      [:application/show-more-applications])]})))))

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
    :dispatch [:application/refresh-haut-and-hakukohteet nil nil false []]}))

(reg-event-fx
 :application/fetch-classes-of-school
 (fn fetch-classes-of-school [_ [_ oppilaitos-oid]]
   {:http {:method              :get
           :path                (str "/lomake-editori/api/applications/oppilaitos/" oppilaitos-oid "/luokat")
           :handler-or-dispatch :application/handle-fetch-classes-of-school-response}}))

(reg-event-db
 :application/handle-fetch-classes-of-school-response
 (fn handle-fetch-classes-of-school-response [db [_ classes]]
   (assoc-in db [:application :selected-school-classes] (sort classes))))

(reg-event-db
 :application/select-application-tab
 (fn select-application-tab [db [_ tab]]
   (assoc-in db [:application :tab] tab)))

