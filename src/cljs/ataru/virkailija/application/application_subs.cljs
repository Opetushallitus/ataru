(ns ataru.virkailija.application.application-subs
  (:require [clojure.core.match :refer [match]]
            [cljs-time.core :as t]
            [re-frame.core :as re-frame]
            [medley.core :refer [find-first]]
            [ataru.application-common.application-field-common :as common]
            [ataru.util :as u]
            [ataru.cljs-util :as util]))

(defn- from-multi-lang [text lang]
  (u/non-blank-val text [lang :fi :sv :en]))

(re-frame/reg-sub
  :application/selected-form
  (fn selected-form [db _]
    (get-in db [:application :selected-application-and-form :form])))

(re-frame/reg-sub
  :application/selected-application
  (fn selected-application [db _]
    (get-in db [:application :selected-application-and-form :application])))

(re-frame/reg-sub
  :application/hakukohde-attachment-reviews
  (fn hakukohde-attachment-reviews [db [_ hakukohde]]
    (get-in db [:application :review :attachment-reviews (keyword hakukohde)])))

(re-frame/reg-sub
  :application/selected-form-fields-by-id
  (fn [_ _]
    (re-frame/subscribe [:application/selected-form]))
  (fn selected-form-fields-by-id [form _]
    (u/form-fields-by-id form)))

(re-frame/reg-sub
  :application/events
  (fn [db _]
    (get-in db [:application :events])))

(re-frame/reg-sub
  :application/selected-event
  (fn [db _]
    (get-in db [:application :selected-application-and-form :selected-event])))

(re-frame/reg-sub
  :application/change-history
  (fn [db _]
    (get-in db [:application :selected-application-and-form :application-change-history])))

(re-frame/reg-sub
  :application/application-haut
  (fn [db _]
    (get-in db [:application :haut])))

(re-frame/reg-sub
  :application/haut
  (fn [db _]
    (get db :haut)))

(re-frame/reg-sub
  :application/fetching-haut
  (fn [db _]
    (get db :fetching-haut)))

(re-frame/reg-sub
  :application/hakukohteet
  (fn [db _]
    (get db :hakukohteet)))

(re-frame/reg-sub
  :application/fetching-hakukohteet
  (fn [db _]
    (get db :fetching-hakukohteet)))

(re-frame/reg-sub
 :application/list-heading
 (fn [db]
   (let [selected-haku      (get-in db [:haut (get-in db [:application :selected-haku])])
         selected-hakukohde (get-in db [:hakukohteet (get-in db [:application :selected-hakukohde])])
         selected-form-key  (get-in db [:application :selected-form-key])
         forms              (get-in db [:application :forms])
         applications       (get-in db [:application :applications])]
     (or (from-multi-lang (:name (get forms selected-form-key)) :fi)
         (from-multi-lang (:name selected-hakukohde) :fi)
         (from-multi-lang (:name selected-haku) :fi)
         (when (sequential? applications)
           (str "Löytyi " (count applications) " hakemusta"))))))

(re-frame/reg-sub
  :application/list-heading-data-for-haku
  (fn [db]
    (let [selected-hakukohde-oid  (get-in db [:application :selected-hakukohde])
          selected-hakukohderyhma (get-in db [:application :selected-hakukohderyhma])
          selected-haku-oid       (cond (some? selected-hakukohde-oid)
                                        (->> (get-in db [:application :haut :tarjonta-haut])
                                             (filter (fn [[_ {:keys [hakukohteet]}]]
                                                       (some (fn [{:keys [oid]}]
                                                               (= selected-hakukohde-oid oid))
                                                             hakukohteet)))
                                             ffirst)
                                        (some? selected-hakukohderyhma)
                                        (first selected-hakukohderyhma)
                                        :else
                                        (get-in db [:application :selected-haku]))
          haun-hakukohteet        (map :oid (get-in db [:application
                                                        :haut
                                                        :tarjonta-haut
                                                        selected-haku-oid
                                                        :hakukohteet]))
          haun-hakukohderyhmat    (distinct (mapcat (fn [hakukohde-oid]
                                                      (get-in db [:hakukohteet
                                                                  hakukohde-oid
                                                                  :ryhmaliitokset]))
                                                    haun-hakukohteet))]
      (when selected-haku-oid
        [selected-haku-oid
         selected-hakukohde-oid
         (second selected-hakukohderyhma)
         haun-hakukohteet
         haun-hakukohderyhmat]))))

(defn- application-list-selected-by
  [db]
  (let [db-application (:application db)]
    (cond
      (:selected-form-key db-application)                      :selected-form-key
      (:selected-haku db-application)                          :selected-haku
      (:selected-hakukohde db-application)                     :selected-hakukohde
      (:selected-ryhman-ensisijainen-hakukohde db-application) :selected-ryhman-ensisijainen-hakukohde
      (:selected-hakukohderyhma db-application)                :selected-hakukohderyhma)))

(re-frame/reg-sub
  :application/application-list-selected-by
  application-list-selected-by)

(defn- selected-hakukohderyhma-hakukohteet
  [db]
  (if-let [[_ hakukohderyhma-oid] (get-in db [:application :selected-hakukohderyhma])]
    (map first
         (filter
           (fn [[_ hakukohde]]
             (if-let [ryhmaliitokset (:ryhmaliitokset hakukohde)]
               (some #(= hakukohderyhma-oid %) ryhmaliitokset)))
           (get-in db [:hakukohteet])))))

(re-frame/reg-sub
  :application/hakukohde-oids-from-selected-hakukohde-or-hakukohderyhma
  (fn [db]
    (let [selected-by (application-list-selected-by db)
          oid-or-oids (when selected-by (-> db :application selected-by))]
      (case selected-by
        :selected-hakukohde #{oid-or-oids}
        :selected-ryhman-ensisijainen-hakukohde #{oid-or-oids}
        :selected-hakukohderyhma (set (selected-hakukohderyhma-hakukohteet db))
        nil))))

(re-frame/reg-sub
  :application/show-ensisijaisesti?
  (fn [db]
    (let [selected-by @(re-frame/subscribe [:application/application-list-selected-by])]
      (cond (= :selected-hakukohde selected-by)
            (some->> (get-in db [:application :selected-hakukohde])
                     (get (get-in db [:hakukohteet]))
                     :haku-oid
                     (get (get-in db [:haut]))
                     :prioritize-hakukohteet)
            (or (= :selected-hakukohderyhma selected-by)
                (= :selected-ryhman-ensisijainen-hakukohde selected-by))
            (some->> (get-in db [:application :selected-hakukohderyhma])
                     first
                     (get (get-in db [:haut]))
                     :prioritize-hakukohteet)
            :else
            false))))

(re-frame/reg-sub
  :application/ensisijaisesti?
  (fn [db]
    (get-in db [:application :ensisijaisesti?] false)))

(re-frame/reg-sub
  :application/show-rajaa-hakukohteella?
  (fn [db]
    (if @(re-frame/subscribe [:application/ensisijaisesti?])
      (let [selected-by @(re-frame/subscribe [:application/application-list-selected-by])]
        (cond (or (= :selected-hakukohderyhma selected-by)
                  (= :selected-ryhman-ensisijainen-hakukohde selected-by))
              (some->> (get-in db [:application :selected-hakukohderyhma])
                first
                (get (get-in db [:haut]))
                :prioritize-hakukohteet)
              :else
              false)))))

(re-frame/reg-sub
  :application/selected-hakukohderyhma-hakukohteet
  selected-hakukohderyhma-hakukohteet)

(re-frame/reg-sub
 :application/show-mass-update-link?
 (fn [db]
   (and (not-empty @(re-frame/subscribe [:application/filtered-applications]))
        (contains? #{:selected-form-key :selected-haku :selected-hakukohde}
                   @(re-frame/subscribe [:application/application-list-selected-by])))))

(re-frame/reg-sub
 :application/show-excel-link?
 (fn [db]
   (and (not-empty @(re-frame/subscribe [:application/filtered-applications]))
        (contains? #{:selected-form-key :selected-haku :selected-hakukohde :selected-hakukohderyhma}
                   @(re-frame/subscribe [:application/application-list-selected-by])))))

(defn- mass-information-request-button-enabled?
  [db]
  (and
    (-> db :application :mass-information-request :subject u/not-blank?)
    (-> db :application :mass-information-request :message u/not-blank?)))

(re-frame/reg-sub
  :application/mass-information-request-button-enabled?
  mass-information-request-button-enabled?)

(re-frame/reg-sub
  :application/mass-information-request-form-status
  (fn [db]
    (if (not (mass-information-request-button-enabled? db))
      :disabled
      (get-in db [:application :mass-information-request :form-status]))))

(defn- haku-completely-processed?
  [haku]
  (= (:processed haku) (:application-count haku)))

(defn- filter-haut-all-not-processed [haut]
  {:direct-form-haut (remove haku-completely-processed? (-> haut :direct-form-haut (vals)))
   :tarjonta-haut    (remove haku-completely-processed? (-> haut :tarjonta-haut (vals)))})

(defn- filter-haut-all-processed [haut]
  {:direct-form-haut (filter haku-completely-processed? (-> haut :direct-form-haut (vals)))
   :tarjonta-haut    (filter haku-completely-processed? (-> haut :tarjonta-haut (vals)))})

(defn sort-by-unprocessed [xs]
  (->> xs (sort-by :application-count >) (sort-by :unprocessed >)))

(defn sort-hakukohteet [tarjonta-haut sort]
  (map #(update % :hakukohteet sort) tarjonta-haut))

(defn- haku-name [haut fetching-haut haku-oid lang]
  (if-let [haku (get haut haku-oid)]
    (or (from-multi-lang (:name haku) lang) haku-oid)
    (when (zero? fetching-haut)
      haku-oid)))

(defn- hakukohde-name [hakukohteet fetching-hakukohteet hakukohde-oid lang]
  (if-let [hakukohde (get hakukohteet hakukohde-oid)]
    (or (from-multi-lang (:name hakukohde) lang) hakukohde-oid)
    (when (zero? fetching-hakukohteet)
      hakukohde-oid)))

(defn- sort-by-haku-name
  [application-haut haut fetching-haut lang]
  (sort-by (comp clojure.string/lower-case
                 #(or (haku-name haut fetching-haut (:oid %) lang) ""))
           application-haut))

(defn- sort-by-hakukohde-name
  [hakukohteet fetching-hakukohteet lang application-hakukohteet]
  (sort-by (comp clojure.string/lower-case
                 #(or (hakukohde-name hakukohteet fetching-hakukohteet (:oid %) lang) ""))
           application-hakukohteet))

(defn- sort-by-form-name [direct-form-haut lang]
  (sort-by (comp clojure.string/lower-case
                 #(or (from-multi-lang (:name %) lang) ""))
           direct-form-haut))

(defn- incomplete-haut [application-haut]
  (when (some? application-haut)
    (-> (filter-haut-all-not-processed application-haut)
        (update :tarjonta-haut sort-by-unprocessed)
        (update :tarjonta-haut sort-hakukohteet sort-by-unprocessed)
        (update :direct-form-haut sort-by-unprocessed))))

(defn- complete-haut
  [application-haut haut fetching-haut hakukohteet fetching-hakukohteet lang]
  (when (some? application-haut)
    (-> (filter-haut-all-processed application-haut)
        (update :tarjonta-haut sort-by-haku-name haut fetching-haut lang)
        (update :tarjonta-haut sort-hakukohteet (partial sort-by-hakukohde-name
                                                         hakukohteet
                                                         fetching-hakukohteet
                                                         lang))
        (update :direct-form-haut sort-by-form-name lang))))

(re-frame/reg-sub
  :application/incomplete-haut
  (fn [_ _]
    (re-frame/subscribe [:application/application-haut]))
  (fn [application-haut _]
    (incomplete-haut application-haut)))

(re-frame/reg-sub
  :application/incomplete-haku-count
  (fn [_ _]
    (re-frame/subscribe [:application/incomplete-haut]))
  (fn [{:keys [tarjonta-haut direct-form-haut]} _]
    (+ (count tarjonta-haut)
       (count direct-form-haut))))

(re-frame/reg-sub
  :application/complete-haut
  (fn [_ _]
    [(re-frame/subscribe [:application/application-haut])
     (re-frame/subscribe [:application/haut])
     (re-frame/subscribe [:application/fetching-haut])
     (re-frame/subscribe [:application/hakukohteet])
     (re-frame/subscribe [:application/fetching-hakukohteet])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn [[application-haut haut fetching-haut hakukohteet fetching-hakukohteet lang] _]
    (complete-haut application-haut haut fetching-haut hakukohteet fetching-hakukohteet lang)))

(re-frame/reg-sub
  :application/complete-haku-count
  (fn [_ _]
    (re-frame/subscribe [:application/complete-haut]))
  (fn [{:keys [tarjonta-haut direct-form-haut]} _]
    (+ (count tarjonta-haut)
       (count direct-form-haut))))

(re-frame/reg-sub
 :application/search-control-all-page-view?
 (fn [db]
   (let [show-search-control (get-in db [:application :search-control :show])]
     (boolean (some #{show-search-control} [:complete :incomplete])))))

(re-frame/reg-sub
  :application/get-i18n-text
  (fn [db [_ translations]]
    (get translations (keyword (get-in db [:application
                                           :selected-application-and-form
                                           :application
                                           :lang]
                                       "fi")))))

(re-frame/reg-sub
  :application/hakukohteet-field
  (fn [_ _]
    (re-frame/subscribe [:application/selected-form]))
  (fn [form _]
    (->> (:content form)
         u/flatten-form-fields
         (filter #(= "hakukohteet" (:id %)))
         first)))

(re-frame/reg-sub
  :application/hakukohde-options-by-oid
  (fn [_ _]
    (re-frame/subscribe [:application/hakukohteet-field]))
  (fn hakukohde-options-by-oid [hakukohteet-field _]
    (->> hakukohteet-field
         :options
         (map (juxt :value identity))
         (into {}))))

(re-frame/reg-sub
  :application/hakukohde-name
  (fn [_ _]
    [(re-frame/subscribe [:application/hakukohteet])
     (re-frame/subscribe [:application/fetching-hakukohteet])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn [[hakukohteet fetching-hakukohteet lang] [_ hakukohde-oid]]
    (hakukohde-name hakukohteet fetching-hakukohteet hakukohde-oid lang)))

(re-frame/reg-sub
  :application/hakukohde-and-tarjoaja-name
  (fn [_ _]
    [(re-frame/subscribe [:application/hakukohteet])
     (re-frame/subscribe [:application/fetching-hakukohteet])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn hakukohde-and-tarjoaja-name
    [[hakukohteet fetching-hakukohteet lang] [_ hakukohde-oid]]
    (if-let [hakukohde (get hakukohteet hakukohde-oid)]
      (str (or (from-multi-lang (:name hakukohde) lang) hakukohde-oid)
           (when-let [tarjoaja-name (from-multi-lang (:tarjoaja-name hakukohde) lang)]
             (str " - " tarjoaja-name)))
      (when (zero? fetching-hakukohteet)
        hakukohde-oid))))

(re-frame/reg-sub
  :application/tarjoaja-name
  (fn [db [_ hakukohde-oid]]
    (if-let [hakukohde (get-in db [:hakukohteet hakukohde-oid])]
      (from-multi-lang (:tarjoaja-name hakukohde) :fi))))

(re-frame/reg-sub
  :application/hakukohderyhma-name
  (fn [db [_ hakukohderyhma-oid]]
    (when-let [hakukohderyhma (get-in db [:hakukohderyhmat hakukohderyhma-oid])]
      (or (from-multi-lang (:name hakukohderyhma) :fi) hakukohderyhma-oid))))

(re-frame/reg-sub
  :application/haku-name
  (fn [_ _]
    [(re-frame/subscribe [:application/haut])
     (re-frame/subscribe [:application/fetching-haut])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn [[haut fetching-haut lang] [_ haku-oid]]
    (haku-name haut fetching-haut haku-oid lang)))

(re-frame/reg-sub
  :application/hakukohteet-header
  (fn [_ _]
    [(re-frame/subscribe [:application/hakukohteet-field])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn hakukohteet-header [[hakukohteet-field lang] _]
    (from-multi-lang (:label hakukohteet-field) lang)))

(re-frame/reg-sub
  :application/hakukohde-label
  (fn [_ _]
    [(re-frame/subscribe [:application/hakukohde-options-by-oid])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn hakukohde-label [[hakukohde-options lang] [_ hakukohde-oid]]
    (from-multi-lang (get-in hakukohde-options [hakukohde-oid :label]) lang)))

(re-frame/reg-sub
  :application/hakukohde-description
  (fn [_ _]
    [(re-frame/subscribe [:application/hakukohde-options-by-oid])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn hakukohde-description [[hakukohde-options lang] [_ hakukohde-oid]]
    (from-multi-lang (get-in hakukohde-options [hakukohde-oid :description]) lang)))

(re-frame/reg-sub
  :application/hakutoiveet
  (fn [db _]
    (get-in db [:application
                :selected-application-and-form
                :application
                :answers
                :hakukohteet
                :value])))

(re-frame/reg-sub
  :application/selected-application-haku-name
  (fn [_ _]
    [(re-frame/subscribe [:application/selected-application])
     (re-frame/subscribe [:application/haut])
     (re-frame/subscribe [:application/fetching-haut])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn [[application haut fetching-haut lang] _]
    (when-let [haku-oid (:haku application)]
      (haku-name haut fetching-haut haku-oid lang))))

(re-frame/reg-sub
  :application/information-request-submit-enabled?
  (fn [db _]
    (let [request-state (-> db :application :information-request :state)]
      (and (-> db :application :information-request :subject u/not-blank?)
           (-> db :application :information-request :message u/not-blank?)
           (nil? request-state)))))

(defn- event-and-information-request-comparator [a b]
  (let [time-a (or (:time a) (:created-time a))
        time-b (or (:time b) (:created-time b))]
    (if (t/before? time-a time-b)
      1
      -1)))

(defn- mark-last-modify-event [events]
  (let [last-modify-event-id (-> (filter util/modify-event? events)
                                 last
                                 :id)]
    (map #(if (= (:id %) last-modify-event-id)
            (assoc % :last-modify-event? true)
            %) events)))

(re-frame/reg-sub
  :application/events-and-information-requests
  (fn [db _]
    (->> (concat (-> db :application :events mark-last-modify-event)
                 (-> db :application :information-requests))
         (sort event-and-information-request-comparator))))

(re-frame/reg-sub
  :application/resend-modify-application-link-enabled?
  (fn [db _]
    (-> db :application :modify-application-link :state nil?)))

(re-frame/reg-sub
  :application/massamuutos-enabled?
  (fn [db _]
    (let [applications             (-> db :application :applications)
          yhteishaku?              (get-in db [:haut (-> db :application :selected-haku) :yhteishaku])
          hakukohde-selected?      (-> db :application :selected-hakukohde)
          hakukohderyhma-selected? (-> db :application :selected-hakukohderyhma)]
      (or (not yhteishaku?)
          hakukohde-selected?
          hakukohderyhma-selected?))))

(re-frame/reg-sub
  :application/filtered-applications
  (fn [db _]
    (-> db :application :filtered-applications)))

(re-frame/reg-sub
  :application/filtered-applications-count
  (fn [_ _]
    (count @(re-frame/subscribe [:application/filtered-applications]))))

(re-frame/reg-sub
  :application/review-state-setting-enabled?
  (fn [db [_ setting-kwd]]
    (if-some [enabled-in-state? (-> db :application :review-settings :config setting-kwd)]
      enabled-in-state?
      true)))

(re-frame/reg-sub
  :application/review-state-setting-disabled?
  (fn [db [_ setting-kwd]]
    (-> db :application :review-settings :config setting-kwd (= :updating))))

(re-frame/reg-sub
  :application/review-note-indexes-on-eligibility
  (fn [db [_]]
    (let [selected-hakukohde-oids (set (get-in db [:application :selected-review-hakukohde-oids]))]
      (->> (-> db :application :review-notes)
           (keep-indexed (fn [index {:keys [state-name hakukohde]}]
                           (when (and (= "eligibility-state" state-name)
                                      (contains? selected-hakukohde-oids (str hakukohde)))
                             index)))))))

(re-frame/reg-sub
  :application/review-note-indexes-excluding-eligibility
  (fn [db]
    (->> (-> db :application :review-notes)
         (keep-indexed (fn [index {:keys [state-name hakukohde]}]
                         (when (not= "eligibility-state" state-name)
                           index))))))

(re-frame/reg-sub
  :application/prioritize-hakukohteet?
  (fn [db _]
    (-> db :application :selected-application-and-form :application :tarjonta :prioritize-hakukohteet)))

(re-frame/reg-sub
  :application/hakukohde-priority-number
  (fn [db [_ hakukohde-oid]]
    (->> (-> db :application :selected-application-and-form :application :answers :hakukohteet :value)
         (keep-indexed #(when (= hakukohde-oid %2) (inc %1)))
         first)))

(re-frame.core/reg-sub
  :application/selected-application-key
  (fn [db _]
    (-> db :application :selected-application-and-form :application :key)))

(defn- modify-event-changes
  [events change-history event-id]
  (let [modify-events (filter util/modify-event? events)]
    (some (fn [[event changes]]
            (when (= event-id (:id event))
              changes))
          (map vector modify-events change-history))))

(defn- replace-change-value-with-label
  [change field lang]
  (match field
    {:options options}
    (-> change
        (update :old common/replace-with-option-label options lang)
        (update :new common/replace-with-option-label options lang))
    :else
    change))

(defn- breadcrumb-label
  [field form-fields answers lang]
  (conj (if-let [parent-field (some-> (:followup-of field) keyword form-fields)]
          (let [parent-breadcrumb (breadcrumb-label parent-field form-fields answers lang)
                value             (common/replace-with-option-label (:option-value field)
                                                                    (:options parent-field)
                                                                    lang)]
            (conj (vec (butlast parent-breadcrumb))
                  (conj (last parent-breadcrumb) value)))
          [])
        [(from-multi-lang (:label field) lang)]))

(re-frame.core/reg-sub
  :application/current-history-items
  (fn [_ _]
    [(re-frame/subscribe [:application/selected-form-fields-by-id])
     (re-frame/subscribe [:application/selected-application])
     (re-frame/subscribe [:application/events])
     (re-frame/subscribe [:application/selected-event])
     (re-frame/subscribe [:application/change-history])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn current-history-items
    [[form-fields application events selected-event change-history lang] _]
    (when-let [changes (modify-event-changes events change-history (:id selected-event))]
      (->> changes
           (map (fn [[id change]]
                  (let [field (get form-fields id)]
                    [id (-> change
                            (replace-change-value-with-label field lang)
                            (assoc :label (breadcrumb-label field
                                                            form-fields
                                                            (:answers application)
                                                            lang)))])))
           (into {})))))

(re-frame.core/reg-sub
  :application/changes-made-for-event
  (fn [_ _]
    [(re-frame/subscribe [:application/events])
     (re-frame/subscribe [:application/change-history])])
  (fn [[events change-history] [_ event-id]]
    (modify-event-changes events change-history event-id)))

(re-frame.core/reg-sub
  :application/field-highlighted?
  (fn [db [_ field-id]]
    (some #{field-id} (-> db :application :selected-application-and-form :highlighted-fields))))

(re-frame.core/reg-sub
  :application/show-info-request-ui?
  (fn [db _]
    (let [selected-hakukohde-oids (seq (get-in db [:application :selected-review-hakukohde-oids]))
          get-processing-state (fn [oid] (get-in db [:application
                                                     :review
                                                     :hakukohde-reviews
                                                     (keyword oid)
                                                     :processing-state]))]
      (and selected-hakukohde-oids
           (every? #(= "information-request" %) (map get-processing-state selected-hakukohde-oids))))))

(re-frame.core/reg-sub
  :application/get-attachment-reviews-for-selected-hakukohde
  (fn [[_ hakukohde] _]
    [(re-frame/subscribe [:application/selected-form-fields-by-id])
     (re-frame/subscribe [:application/selected-application])
     (re-frame/subscribe [:application/hakukohde-attachment-reviews hakukohde])])
  (fn get-attachment-reviews-for-selected-hakukohde
    [[form-fields application attachment-reviews] _]
    (for [[key state] attachment-reviews
          :let        [answer (get-in application [:answers (keyword key)])
                       field  ((keyword key) form-fields)]]
      {:key    key
       :state  state
       :values (:values answer)
       :label  (:label field)})))

(re-frame.core/reg-sub
  :application/lang
  (fn [db _]
    (or (-> db :application :selected-application-and-form :form :selected-language keyword)
        (-> db :application :selected-application-and-form :application :lang keyword)
        :fi)))

(re-frame.core/reg-sub
  :application/enabled-filter-count
  (fn [db _]
    (reduce-kv
      (fn [acc _ filters]
        (+ acc
           (reduce-kv
             (fn [acc2 _ enabled?] (if (false? enabled?) (inc acc2) acc2))
             0
             filters)))
      0
      (get-in db [:application :filters]))))

(re-frame.core/reg-sub
  :application/loaded-application-count
  (fn [db _]
    (-> db :application :applications (count))))

(re-frame.core/reg-sub
  :application/eligibility-automatically-checked?
  (fn [db _]
    (let [hakukohde-oids (get-in db [:application :selected-review-hakukohde-oids])
          newest-event-automatically-changed (fn [hakukohde-oid] (->> (get-in db [:application :events])
                                                                      (filter #(and (= "eligibility-state" (:review-key %))
                                                                                    (= hakukohde-oid (:hakukohde %))))
                                                                      (sort-by :id >)
                                                                      first
                                                                      :event-type
                                                                      (= "eligibility-state-automatically-changed")))]
      (every? newest-event-automatically-changed hakukohde-oids))))

(re-frame/reg-sub
  :application/all-pohjakoulutus-filters-selected?
  (fn [db _]
    (->> (-> db :application :filters :base-education)
         (vals)
         (every? true?))))

(re-frame/reg-sub
  :application/applications-have-base-education-answers
  (fn [db _]
    (->> (-> db :application :applications)
         (some #(not-empty (:base-education %)))
         (boolean))))
