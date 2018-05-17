(ns ataru.virkailija.application.application-subs
  (:require [clojure.core.match :refer [match]]
            [cljs-time.core :as t]
            [re-frame.core :as re-frame]
            [ataru.util :as u]
            [ataru.application.review-states :as review-states]
            [ataru.cljs-util :as util]
            [ataru.application.application-states :as application-states]))

(defn- from-multi-lang [text lang]
  (some #(get text %) [lang :fi :sv :en]))

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

(re-frame/reg-sub
  :application/application-list-selected-by
  (fn [db]
    (let [db-application (:application db)]
      (cond
        (:selected-form-key db-application) :selected-form-key
        (:selected-haku db-application) :selected-haku
        (:selected-hakukohde db-application) :selected-hakukohde
        (:selected-hakukohderyhma db-application) :selected-hakukohderyhma))))

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
            (= :selected-hakukohderyhma selected-by)
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
 :application/show-mass-update-link?
 (fn [db]
   (and (not-empty @(re-frame/subscribe [:application/filtered-applications]))
        (contains? #{:selected-form-key :selected-haku :selected-hakukohde}
                   @(re-frame/subscribe [:application/application-list-selected-by])))))

(re-frame/reg-sub
 :application/show-excel-link?
 (fn [db]
   (and (not-empty @(re-frame/subscribe [:application/filtered-applications]))
        (contains? #{:selected-form-key :selected-haku :selected-hakukohde}
                   @(re-frame/subscribe [:application/application-list-selected-by])))))

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

(defn- haku-name [db haku-oid lang]
  (if-let [haku (get-in db [:haut haku-oid])]
    (or (from-multi-lang (:name haku) lang) haku-oid)
    (when (zero? (:fetching-haut db))
      haku-oid)))

(defn- hakukohde-name [db hakukohde-oid lang]
  (if-let [hakukohde (get-in db [:hakukohteet hakukohde-oid])]
    (or (from-multi-lang (:name hakukohde) lang) hakukohde-oid)
    (when (zero? (:fetching-hakukohteet db))
      hakukohde-oid)))

(defn- sort-by-haku-name [haut db]
  (sort-by (comp clojure.string/lower-case
                 #(or (haku-name db (:oid %) :fi) ""))
           haut))

(defn- sort-by-hakukohde-name [hakukohteet db]
  (sort-by (comp clojure.string/lower-case
                 #(or (hakukohde-name db (:oid %) :fi) ""))
           hakukohteet))

(defn- sort-by-form-name [direct-form-haut]
  (sort-by (comp clojure.string/lower-case
                 #(or (from-multi-lang (:name %) :fi) ""))
           direct-form-haut))

(defn- incomplete-haut [db]
  (when-let [haut (get-in db [:application :haut])]
    (-> (filter-haut-all-not-processed haut)
        (update :tarjonta-haut sort-by-unprocessed)
        (update :tarjonta-haut sort-hakukohteet sort-by-unprocessed)
        (update :direct-form-haut sort-by-unprocessed))))

(defn- complete-haut [db]
  (when-let [haut (get-in db [:application :haut])]
    (-> (filter-haut-all-processed haut)
        (update :tarjonta-haut sort-by-haku-name db)
        (update :tarjonta-haut sort-hakukohteet sort-by-hakukohde-name)
        (update :direct-form-haut sort-by-form-name))))

(re-frame/reg-sub
  :application/incomplete-haut
  incomplete-haut)

(re-frame/reg-sub
  :application/incomplete-haku-count
  (fn [db]
    (let [{:keys [tarjonta-haut direct-form-haut]} (incomplete-haut db)]
      (+ (count tarjonta-haut)
         (count direct-form-haut)))))

(re-frame/reg-sub
  :application/complete-haut
  complete-haut)

(re-frame/reg-sub
  :application/complete-haku-count
  (fn [db]
    (let [{:keys [tarjonta-haut direct-form-haut]} (complete-haut db)]
      (+ (count tarjonta-haut)
         (count direct-form-haut)))))

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
  (fn [db _]
    (first
     (filter #(= "hakukohteet" (:id %))
             (get-in db [:application
                         :selected-application-and-form
                         :form
                         :content])))))

(re-frame/reg-sub
  :application/hakukohde-options-by-oid
  (fn [db _]
    (->> @(re-frame/subscribe [:application/hakukohteet-field])
         :options
         (map (juxt :value identity))
         (into {}))))

(re-frame/reg-sub
  :application/hakukohde-name
  (fn [db [_ hakukohde-oid]] (hakukohde-name db hakukohde-oid :fi)))

(re-frame/reg-sub
  :application/hakukohde-and-tarjoaja-name
  (fn [db [_ hakukohde-oid]]
    (if-let [hakukohde (get-in db [:hakukohteet hakukohde-oid])]
      (str (or (from-multi-lang (:name hakukohde) :fi) hakukohde-oid)
           (when-let [tarjoaja-name (from-multi-lang (:tarjoaja-name hakukohde) :fi)]
             (str " - " tarjoaja-name)))
      (when (zero? (:fetching-hakukohteet db))
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
  (fn [db [_ haku-oid]] (haku-name db haku-oid :fi)))

(re-frame/reg-sub
  :application/hakukohteet-header
  (fn [db _]
    @(re-frame/subscribe [:application/get-i18n-text
                          (:label @(re-frame/subscribe [:application/hakukohteet-field]))])))
(re-frame/reg-sub
  :application/hakukohde-label
  (fn [db [_ hakukohde-oid]]
    @(re-frame/subscribe [:application/get-i18n-text
                          (get-in @(re-frame/subscribe [:application/hakukohde-options-by-oid])
                                  [hakukohde-oid :label])])))

(re-frame/reg-sub
  :application/hakukohde-description
  (fn [db [_ hakukohde-oid]]
    @(re-frame/subscribe [:application/get-i18n-text
                          (get-in @(re-frame/subscribe [:application/hakukohde-options-by-oid])
                                  [hakukohde-oid :description])])))

(re-frame/reg-sub
  :application/hakukohteet
  (fn [db _]
    (get-in db [:application
                :selected-application-and-form
                :application
                :answers
                :hakukohteet
                :value])))

(re-frame/reg-sub
  :application/selected-application-haku-name
  (fn [db _]
    (let [application      (get-in db [:application :selected-application-and-form :application])
          application-lang (keyword (:lang application "fi"))]
      (when-let [haku-oid (:haku application)]
        (haku-name db haku-oid application-lang)))))

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

(defn- filter-by-yksiloity
  [application identified? unidentified?]
  (match [identified? unidentified?]
         [true true] true
         [false false] false
         [false true] (-> application :person :yksiloity (not))
         [true false] (-> application :person :yksiloity)))

(defn- filter-by-hakukohde-review
  [application selected-hakukohde requirement-name states-to-include]
  (let [relevant-states (->> (:application-hakukohde-reviews application)
                             (filter #(and (= requirement-name (:requirement %))
                                           (or (not selected-hakukohde) (= selected-hakukohde (:hakukohde %)))))
                             (map :state)
                             (set))]
    (not-empty (clojure.set/intersection states-to-include relevant-states))))

(defn- filter-by-attachment-review
  [application selected-hakukohde states-to-include]
  (let [relevant-states (->> (:application-attachment-reviews application)
                             (filter #(or (not selected-hakukohde) (= selected-hakukohde (:hakukohde %))))
                             (map :state)
                             (set))]
    (or (empty? (:application-attachment-reviews application))
        (not-empty (clojure.set/intersection states-to-include relevant-states)))))

(defn- parse-enabled-filters
  [filters kw]
  (->> (get filters kw)
       (filter second)
       (map first)
       (map name)
       (set)))

(re-frame/reg-sub
  :application/filtered-applications
  (fn [db _]
    (let [applications                 (-> db :application :applications)
          selected-hakukohde           (cond
                                         (-> db :application :selected-hakukohde) (-> db :application :selected-hakukohde)
                                         (-> db :application :selected-haku) nil
                                         :else "form")
          attachment-states-to-include (-> db :application :attachment-state-filter set)
          processing-states-to-include (-> db :application :processing-state-filter set)
          selection-states-to-include  (-> db :application :selection-state-filter set)
          filters                      (-> db :application :filters)
          identified?                  (-> db :application :filters :only-identified :identified)
          unidentified?                (-> db :application :filters :only-identified :unidentified)]
      (filter
        (fn [application]
          (and
            (filter-by-yksiloity application identified? unidentified?)
            (filter-by-hakukohde-review application selected-hakukohde "processing-state" processing-states-to-include)
            (filter-by-hakukohde-review application selected-hakukohde "selection-state" selection-states-to-include)
            (filter-by-hakukohde-review application selected-hakukohde "language-requirement" (parse-enabled-filters filters :language-requirement))
            (filter-by-hakukohde-review application selected-hakukohde "degree-requirement" (parse-enabled-filters filters :degree-requirement))
            (filter-by-hakukohde-review application selected-hakukohde "eligibility-state" (parse-enabled-filters filters :eligibility-state))
            (filter-by-hakukohde-review application selected-hakukohde "payment-obligation" (parse-enabled-filters filters :payment-obligation))
            (filter-by-attachment-review application selected-hakukohde attachment-states-to-include)))
        applications))))

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
  :application/review-list-visible?
  (fn [db [_ list-kwd]]
    (-> db :application :ui/review list-kwd)))

(re-frame/reg-sub
  :application/review-notes-count
  (fn [db]
    (or (-> db :application :review-notes count)
        0)))

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
  [db event-id]
  (let [modify-events  (filter util/modify-event? (-> db :application :events))
        change-history (-> db :application :selected-application-and-form :application-change-history)]
    (some (fn [[event changes]]
            (when (= event-id (:id event))
              changes))
          (map vector modify-events change-history))))

(re-frame.core/reg-sub
  :application/current-history-items
  (fn [db _]
    (modify-event-changes db (-> db :application :selected-application-and-form :selected-event :id))))

(re-frame.core/reg-sub
  :application/selected-event
  (fn [db _]
    (-> db :application :selected-application-and-form :selected-event)))

(re-frame.core/reg-sub
  :application/changes-made-for-event
  (fn [db [_ event-id]]
    (modify-event-changes db event-id)))

(re-frame.core/reg-sub
  :application/field-highlighted?
  (fn [db [_ field-id]]
    (some #{field-id} (-> db :application :selected-application-and-form :highlighted-fields))))

(re-frame.core/reg-sub
  :application/show-info-request-ui?
  (fn [db _]
    (let [selected-hakukohde (get-in db [:application :selected-review-hakukohde])]
      (= "information-request" (get-in db [:application
                                           :review
                                           :hakukohde-reviews
                                           (keyword selected-hakukohde)
                                           :processing-state])))))

(re-frame.core/reg-sub
  :application/get-attachment-reviews-for-selected-hakukohde
  (fn [db [_ selected-hakukohde]]
    (let [attachment-reviews (get-in db [:application :review :attachment-reviews (keyword selected-hakukohde)])
          answers            (-> db :application :selected-application-and-form :application :answers)
          form-fields        (-> db :application :selected-application-and-form :form u/form-fields-by-id)]
      (for [[key state] attachment-reviews
            :let [answer ((keyword key) answers)
                  field  ((keyword key) form-fields)]]
        {:key    key
         :state  state
         :values (:values answer)
         :label  (:label field)}))))

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