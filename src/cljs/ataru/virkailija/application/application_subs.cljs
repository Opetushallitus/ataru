(ns ataru.virkailija.application.application-subs
  (:require [cljs-time.core :as t]
            [re-frame.core :as re-frame]
            [ataru.util :as u]
            [ataru.application.review-states :as review-states]
            [ataru.cljs-util :as util]))

(defn- from-multi-lang [text]
  (some #(get text %) [:fi :sv :en]))

(re-frame/reg-sub
 :application/list-heading
 (fn [db]
   (let [selected-haku       (get-in db [:application :selected-haku])
         selected-hakukohde  (get-in db [:application :selected-hakukohde])
         selected-form-key   (get-in db [:application :selected-form-key])
         forms               (get-in db [:application :forms])
         applications        (get-in db [:application :applications])]
     (or (from-multi-lang (:name (get forms selected-form-key)))
         (from-multi-lang (:name selected-hakukohde))
         (from-multi-lang (:name selected-haku))
         (if (sequential? applications) (str "Löytyi " (count applications) " hakemusta"))))))

(re-frame/reg-sub
  :application/list-heading-data-for-haku
  (fn [db]
    (let [selected-haku      (get-in db [:application :selected-haku])
          selected-hakukohde (get-in db [:application :selected-hakukohde])]
      (cond
        selected-haku [selected-haku
                       nil
                       (:hakukohteet selected-haku)]
        selected-hakukohde (let [selected-haku (get-in db [:application :haut :tarjonta-haut (:haku selected-hakukohde)])]
                             [selected-haku
                              selected-hakukohde
                              (:hakukohteet selected-haku)])))))

(re-frame/reg-sub
  :application/application-list-selected-by
  (fn [db]
    (let [db-application (:application db)]
      (cond
        (:selected-form-key db-application) :selected-form-key
        (:selected-haku db-application) :selected-haku
        (:selected-hakukohde db-application) :selected-hakukohde))))

(re-frame/reg-sub
 :application/application-list-belongs-to-haku?
 (fn [db]
   (boolean
    (or
     (get-in db [:application :selected-haku])
     (get-in db [:application :selected-hakukohde])
     (get-in db [:application :selected-form-key])))))

(defn- haku-completely-processed?
  [haku]
  (= (:processed haku) (:application-count haku)))

(defn- filter-haut-all-not-processed [haut]
  {:direct-form-haut (remove haku-completely-processed? (-> haut :direct-form-haut (vals)))
   :tarjonta-haut    (remove haku-completely-processed? (-> haut :tarjonta-haut (vals)))})

(defn- filter-haut-all-processed [haut]
  {:direct-form-haut (filter haku-completely-processed? (-> haut :direct-form-haut (vals)))
   :tarjonta-haut    (filter haku-completely-processed? (-> haut :tarjonta-haut (vals)))})

(defn sort-haku-seq-by-unprocessed [haku-seq]
  (->> haku-seq (sort-by :application-count >) (sort-by :unprocessed >)))

(defn sort-haku-seq-by-name [haku-seq]
  (sort-by (fn [haku]
             (if (string? (:name haku))
               (:name haku)
               (from-multi-lang (:name haku))))
           #(compare (clojure.string/lower-case %1) (clojure.string/lower-case %2))
           haku-seq))

(defn sort-hakukohteet [tarjonta-haut sort-haku-seq-fn]
  (map #(update % :hakukohteet sort-haku-seq-fn) tarjonta-haut))

(defn sort-haut [haut sort-haku-seq-fn]
  (-> haut
      (assoc :direct-form-haut (sort-haku-seq-fn (:direct-form-haut haut)))
      (assoc :tarjonta-haut (->
                             (:tarjonta-haut haut)
                             sort-haku-seq-fn
                             (sort-hakukohteet sort-haku-seq-fn)))))

(defn when-haut [db handle-haut-fn]
  (when-let [haut (get-in db [:application :haut])]
     (handle-haut-fn haut)))

(re-frame/reg-sub
 :application/incomplete-haut
 (fn [db]
   (when-haut
       db
       #(-> %
            (filter-haut-all-not-processed)
            (sort-haut sort-haku-seq-by-unprocessed)))))

(re-frame/reg-sub
 :application/incomplete-haku-count
 (fn [_]
   (count @(re-frame/subscribe [:application/incomplete-haut]))))

(re-frame/reg-sub
 :application/complete-haut
 (fn [db]
   (when-haut
       db
       #(->
         %
         (filter-haut-all-processed)
         (sort-haut sort-haku-seq-by-name)))))

(re-frame/reg-sub
 :application/complete-haku-count
 (fn [_]
   (count @(re-frame/subscribe [:application/complete-haut]))))

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
    (let [application       (get-in db [:application :selected-application-and-form :application])
          application-lang  (keyword (:lang application "fi"))]
      (when-let [haku-oid (:haku application)]
        (get-in db [:application :haut :tarjonta-haut haku-oid :name application-lang])))))

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
  [application only-identified?]
  (or (not only-identified?)
      (not (-> application :person :yksiloity))))

(defn- filter-by-hakukohde-review
  [application requirement-name default-state-name states-to-include]
  (let [states (->> (:application-hakukohde-reviews application)
                    (filter #(= requirement-name (:requirement %)))
                    (map :state))]
    (or
      (not (empty? (clojure.set/intersection
                     states-to-include
                     (set states))))
      (and
        (contains? states-to-include default-state-name)
        (or
          (empty? states)
          (< (count states)
             (count (:hakukohde application))))))))

(re-frame/reg-sub
  :application/filtered-applications
  (fn [db _]
    (let [applications                 (-> db :application :applications)
          processing-states-to-include (-> db :application :processing-state-filter set)
          selection-states-to-include  (-> db :application :selection-state-filter set)
          only-identified? (-> db :application :only-identified?)]
      (filter
        (fn [application]
          (and
            (filter-by-yksiloity application only-identified?)
            (filter-by-hakukohde-review application "processing-state" review-states/initial-application-hakukohde-processing-state processing-states-to-include)
            (filter-by-hakukohde-review application "selection-state" "incomplete" selection-states-to-include)))
        applications))))

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
