(ns ataru.virkailija.application.application-subs
  (:require [ataru.application-common.application-field-common :as common]
            [ataru.component-data.person-info-module :as person-info-module]
            [ataru.tarjonta.haku :as haku]
            [ataru.util :as u]
            [ataru.tutkinto.tutkinto-util :as tutkinto-util]
            [ataru.virkailija.application.application-selectors :refer [hakukohde-oids-from-selected-hakukohde-or-hakukohderyhma
                                                                        selected-application-answers
                                                                        selected-hakukohde-oid-set
                                                                        selected-hakukohderyhma-hakukohteet]]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-subs]
            [ataru.virkailija.db :as initial-db]
            [cljs-time.core :as t]
            [clojure.core.match :refer [match]]
            [clojure.set :as set]
            [clojure.string :as string]
            [re-frame.core :as re-frame]))

(re-frame/reg-sub
  :application/selected-form
  (fn selected-form [db _]
    (get-in db [:application :selected-application-and-form :form])))

(re-frame/reg-sub
  :application/selected-application
  (fn selected-application [db _]
    (get-in db [:application :selected-application-and-form :application])))

(re-frame/reg-sub
  :application/selected-application-answers
  (fn [db _] (selected-application-answers db)))

(re-frame/reg-sub
  :application/selected-form-fields-by-id
  (fn [_ _]
    (re-frame/subscribe [:application/selected-form]))
  (fn selected-form-fields-by-id [form _]
    (u/form-fields-by-id form)))

(re-frame/reg-sub
  :application/selected-form-key
  (fn [db _]
    (let [selected-haku (or (get-in db [:application :selected-haku])
                            (get-in db [:hakukohteet (get-in db [:application :selected-hakukohde]) :haku-oid])
                            (get-in db [:application :selected-hakukohderyhma 0]))
          selected-form (or (get-in db [:application :selected-form-key])
                            (get-in db [:haut selected-haku :ataru-form-key]))]
      selected-form)))

(re-frame/reg-sub
  :application/selected-form-key-for-search
  (fn [db _]
    (let [selected-haku (or (get-in db [:application :selected-haku])
                            (get-in db [:hakukohteet (get-in db [:application :selected-hakukohde]) :haku-oid])
                            (get-in db [:application :selected-hakukohderyhma 0]))
          selected-form (or (get-in db [:application :selected-form-key])
                            (get-in db [:haut selected-haku :ataru-form-key]))
          form-for-haku (when-let [[haku key] (get-in db [:application :form-key-for-haku])]
                          (when (= haku selected-haku)
                            key))]
      (or selected-form form-for-haku))))

(re-frame/reg-sub
  :application/selected-form-attachment-fields
  (fn [_ _]
    (re-frame/subscribe [:application/selected-form]))
  (fn selected-form-attachment-fields [form _]
    (u/form-attachment-fields form)))

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
  :application/fetching-haut-and-hakukohteet-errored?
  (fn [db _]
    (get db :fetching-haut-and-hakukohteet-errored false)))

(re-frame/reg-sub
  :application/fetching-haut
  (fn [db _]
    (get db :fetching-haut)))

(re-frame/reg-sub
  :application/rajaus-hakukohteella-value
  (fn [db _]
    (get-in db [:application :rajaus-hakukohteella-value])))

(re-frame/reg-sub
  :application/hakukohteet
  (fn [db _]
    (get db :hakukohteet)))

(re-frame/reg-sub
  :application/hakukohderyhmat
  (fn [db _]
    (get db :hakukohderyhmat)))

(re-frame/reg-sub
  :application/fetching-hakukohteet
  (fn [db _]
    (get db :fetching-hakukohteet)))

(re-frame/reg-sub
  :application/path-to-haku-search
  (fn [_ [_ haku-oid]]
    (when haku-oid
      (str "/lomake-editori/applications/haku/" haku-oid))))

(re-frame/reg-sub
  :application/path-to-hakukohderyhma-search
  (fn [_ [_ haku-oid hakukohderyhma-oid]]
    (when (and haku-oid
               hakukohderyhma-oid)
      (str "/lomake-editori/applications/haku/" haku-oid "/hakukohderyhma/" hakukohderyhma-oid))))

(re-frame/reg-sub
  :application/path-to-hakukohde-search
  (fn [_ [_ hakukohde-oid]]
    (when hakukohde-oid
      (str "/lomake-editori/applications/hakukohde/" hakukohde-oid))))

(re-frame/reg-sub
 :application/list-heading
 (fn [db]
   (let [selected-haku      (get-in db [:haut (get-in db [:application :selected-haku])])
         selected-hakukohde (get-in db [:hakukohteet (get-in db [:application :selected-hakukohde])])
         selected-form-key  (get-in db [:application :selected-form-key])
         forms              (get-in db [:application :forms])
         applications       (get-in db [:application :applications])]
     (or (u/from-multi-lang (:name (get forms selected-form-key)) :fi)
         (u/from-multi-lang (:name selected-hakukohde) :fi)
         (u/from-multi-lang (:name selected-haku) :fi)
         (when (sequential? applications)
           (str "Löytyi " (count applications) " hakemusta"))))))

(re-frame/reg-sub
  :application/selected-hakukohde
  (fn [db _]
    (get-in db [:application :selected-hakukohde])))

(re-frame/reg-sub
  :application/selected-hakukohderyhma
  (fn [db _]
    (get-in db [:application :selected-hakukohderyhma])))

(re-frame/reg-sub
  :application/selected-haku-oid
  (fn [db]
    (let [selected-hakukohde-oid  (get-in db [:application :selected-hakukohde])
          selected-hakukohderyhma (get-in db [:application :selected-hakukohderyhma])]
      (cond (some? selected-hakukohde-oid)
        (get-in db [:hakukohteet selected-hakukohde-oid :haku-oid])
        (some? selected-hakukohderyhma)
        (first selected-hakukohderyhma)
        :else
        (get-in db [:application :selected-haku])))))

(re-frame/reg-sub
  :application/list-heading-data-for-haku
  (fn [_ _]
    [(re-frame/subscribe [:application/selected-hakukohde])
     (re-frame/subscribe [:application/selected-hakukohderyhma])
     (re-frame/subscribe [:application/selected-haku-oid])
     (re-frame/subscribe [:application/haut])
     (re-frame/subscribe [:application/hakukohteet])
     (re-frame/subscribe [:application/hakukohderyhmat])])
  (fn [[selected-hakukohde-oid selected-hakukohderyhma selected-haku-oid haut hakukohteet hakukohderyhmat] _]
    (let [haun-hakukohteet     (keep (or hakukohteet {}) (get-in haut [selected-haku-oid :hakukohteet]))
          haun-hakukohderyhmat (->> haun-hakukohteet
                                    (mapcat :ryhmaliitokset)
                                    distinct
                                    (keep (or hakukohderyhmat {})))]
      (when selected-haku-oid
        [selected-haku-oid
         selected-hakukohde-oid
         (second selected-hakukohderyhma)
         haun-hakukohteet
         haun-hakukohderyhmat]))))

(defn application-list-selected-by
  [db]
  (let [db-application (:application db)]
    (cond
      (:selected-form-key db-application)       :selected-form-key
      (:selected-haku db-application)           :selected-haku
      (:selected-hakukohde db-application)      :selected-hakukohde
      (:selected-hakukohderyhma db-application) :selected-hakukohderyhma)))

(re-frame/reg-sub
  :application/application-list-selected-by
  application-list-selected-by)

(re-frame/reg-sub
  :application/hakukohde-oids-from-selected-hakukohde-or-hakukohderyhma
  hakukohde-oids-from-selected-hakukohde-or-hakukohderyhma)

(re-frame/reg-sub
  :application/selected-hakukohde-oid-set
  selected-hakukohde-oid-set)

(re-frame/reg-sub
  :application/show-ensisijaisesti?
  (fn [db]
    (let [selected-by (application-list-selected-by db)]
      (cond (= :selected-hakukohde selected-by)
            true
            (= :selected-hakukohderyhma selected-by)
            true
            :else
            false))))

(re-frame/reg-sub
  :application/ensisijaisesti?
  (fn [db]
    (get-in db [:application :ensisijaisesti?-checkbox] false)))

(re-frame/reg-sub
  :application/send-update-link?
  (fn [db]
    (get-in db [:application :send-update-link?-checkbox] false)))

(re-frame/reg-sub
  :application/show-rajaa-hakukohteella?
  (fn [db]
    (= :selected-hakukohderyhma (application-list-selected-by db))))

(re-frame/reg-sub
  :application/filters-changed?
  (fn [db]
    (or (not= (get-in db [:application :filters])
              (get-in db [:application :filters-checkboxes]))
        (not= (get-in db [:application :ensisijaisesti?])
              (get-in db [:application :ensisijaisesti?-checkbox]))
        (not= (get-in db [:application :rajaus-hakukohteella])
              (get-in db [:application :rajaus-hakukohteella-value]))
        (not= (get-in db [:application :school-filter])
              (get-in db [:application :school-filter-pending-value]))
        (not= (get-in db [:application :classes-of-school])
              (get-in db [:application :classes-of-school-pending-value])))))

(re-frame/reg-sub
  :application/selected-hakukohderyhma-hakukohteet
  selected-hakukohderyhma-hakukohteet)

(re-frame/reg-sub
  :application/selected-haku
  (fn [_ _]
    [(re-frame/subscribe [:application/selected-haku-oid])
     (re-frame/subscribe [:application/haut])])
  (fn [[selected-haku-oid haut] _]
    (get haut selected-haku-oid)))

(re-frame/reg-sub
  :application/toisen-asteen-yhteishaku?
  (fn [_ _]
    (re-frame/subscribe [:application/selected-application]))
  (fn [selected-application _]
    (haku/toisen-asteen-yhteishaku? (-> selected-application :tarjonta))))


(defn- mass-information-request-button-enabled?
  [db]
  (and
    (-> db :application :mass-information-request :subject u/not-blank?)
    (-> db :application :mass-information-request :message u/not-blank?)))

(defn- single-information-request-button-enabled?
  [db]
  (and
    (-> db :application :single-information-request :subject u/not-blank?)
    (-> db :application :single-information-request :message u/not-blank?)
    (<= (count (-> db :application :single-information-request :subject)) 120))
  )

(defn- mass-review-notes-button-enabled?
  [db]
  (-> db :application :mass-review-notes :review-notes u/not-blank?))

(re-frame/reg-sub
 :application/mass-review-notes-button-enabled?
 mass-review-notes-button-enabled?)

(re-frame/reg-sub
  :application/mass-information-request-button-enabled?
  mass-information-request-button-enabled?)

(re-frame/reg-sub
  :application/single-information-request-button-enabled?
  single-information-request-button-enabled?)


(re-frame/reg-sub
  :application/mass-information-request-form-status
  (fn [db]
    (cond (get-in db [:application :fetching-applications?])
          :loading-applications
          (not (mass-information-request-button-enabled? db))
          :disabled
          :else
          (get-in db [:application :mass-information-request :form-status]))))

(re-frame/reg-sub
:application/single-information-request-form-status
(fn [db]
  (cond (get-in db [:application :fetching-applications?])
        :loading-applications
        (not (single-information-request-button-enabled? db))
        :disabled
        :else
        (get-in db [:application :single-information-request :form-status]))))

(re-frame/reg-sub
 :application/mass-review-notes-form-status
 (fn [db]
   (cond (get-in db [:application :fetching-applications?])
     :loading-applications
     (not (mass-review-notes-button-enabled? db))
     :disabled
     :else
     (get-in db [:application :mass-review-notes :form-status]))))

(re-frame/reg-sub
  :application/mass-information-request-only-guardian-enabled?
  (fn [_ _]
    [(re-frame/subscribe [:application/selected-haku-oid])
     (re-frame/subscribe [:application/haut])])
  (fn [[haku-oid haut] _]
    (if-let [haku (get-in haut [haku-oid])]
      (string/starts-with? (:kohdejoukko-uri haku) "haunkohdejoukko_11")
      false)))
(re-frame/reg-sub
  :application/single-information-request-only-guardian-enabled?
  (fn [_ _]
    [(re-frame/subscribe [:application/selected-haku-oid])
     (re-frame/subscribe [:application/haut])])
  (fn [[haku-oid haut] _]
    (if-let [haku (get-in haut [haku-oid])]
      (string/starts-with? (:kohdejoukko-uri haku) "haunkohdejoukko_11")
      false)))

(re-frame/reg-sub
  :application/mass-information-request-popup-visible?
  (fn [db]
    (get-in db [:application :mass-information-request :visible?])))
(re-frame/reg-sub
 :application/is-mass-information-link-checkbox-set?
 (fn [db]
   (get-in db [:application :mass-send-update-link?-checkbox])))
(re-frame/reg-sub
  :application/single-information-request-popup-visible?
  (fn [db]
    (get-in db [:application :single-information-request :visible?])))
(re-frame/reg-sub
  :application/is-single-information-link-checkbox-set?
  (fn [db]
    (get-in db [:application :send-update-link?-checkbox])))

(re-frame/reg-sub
  :application/information-request-send-reminder
  (fn [db]
    (get-in db [:application :information-request :send-reminder?])))

(re-frame/reg-sub
  :application/information-request-reminder-days
  (fn [db]
    (get-in db [:application :information-request :reminder-days])))

(defn- haku-completely-processed?
  [haku]
  (= (:processed haku) (:application-count haku)))

(defn- filter-haut-all-not-processed [haut]
  {:direct-form-haut (remove haku-completely-processed? (-> haut :direct-form-haut vals))
   :tarjonta-haut    (remove haku-completely-processed? (-> haut :tarjonta-haut vals))})

(defn- filter-haut-all-processed [haut]
  {:direct-form-haut (filter haku-completely-processed? (-> haut :direct-form-haut vals))
   :tarjonta-haut    (filter haku-completely-processed? (-> haut :tarjonta-haut vals))})

(defn sort-by-unprocessed [xs]
  (->> xs (sort-by :application-count >) (sort-by :unprocessed >)))

(defn sort-hakukohteet [tarjonta-haut sort]
  (map #(update % :hakukohteet sort) tarjonta-haut))

(defn- haku-name [haut fetching-haut haku-oid lang]
  (if-let [haku (get haut haku-oid)]
    (or (u/from-multi-lang (:name haku) lang) haku-oid)
    (when (zero? fetching-haut)
      haku-oid)))

(defn- hakukohde-name [hakukohteet fetching-hakukohteet hakukohde-oid lang]
  (if-let [hakukohde (get hakukohteet hakukohde-oid)]
    (or (u/from-multi-lang (:name hakukohde) lang) hakukohde-oid)
    (when (zero? fetching-hakukohteet)
      hakukohde-oid)))

(defn- sort-by-haku-name
  [application-haut haut fetching-haut lang]
  (sort-by (comp string/lower-case
                 #(or (haku-name haut fetching-haut (:oid %) lang) ""))
           application-haut))

(defn- sort-by-hakukohde-name
  [hakukohteet fetching-hakukohteet lang application-hakukohteet]
  (sort-by (comp string/lower-case
                 #(or (hakukohde-name hakukohteet fetching-hakukohteet (:oid %) lang) ""))
           application-hakukohteet))

(defn- sort-by-form-name [direct-form-haut lang]
  (sort-by (comp string/lower-case
                 #(or (u/from-multi-lang (:name %) lang) ""))
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
  :application/field-label
  (fn [_ _]
    [(re-frame/subscribe [:application/selected-form])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn [[form lang] [_ field-id]]
    (u/non-blank-val
     (->> (:content form)
          u/flatten-form-fields
          (filter #(= field-id (:id %)))
          first
          :label)
     [lang :fi :sv :en])))

(re-frame/reg-sub
  :application/form
  (fn [db [_ form-key]]
    (get-in db [:forms form-key])))

(re-frame/reg-sub
  :application/form-fields-by-id
  (fn [[_ form-key] _]
    (re-frame/subscribe [:application/form form-key]))
  (fn [form _]
    (:form-fields-by-id form)))

(re-frame/reg-sub
  :application/form-field
  (fn [[_ form-key _] _]
    (re-frame/subscribe [:application/form-fields-by-id form-key]))
  (fn [fields-by-id [_ _ field-id]]
    (get fields-by-id (keyword field-id))))

(re-frame/reg-sub
  :application/form-field-label
  (fn [[_ form-key field-id] _]
    [(re-frame/subscribe [:application/form-field form-key field-id])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn [[field lang] _]
    (u/from-multi-lang (:label field) lang)))

(re-frame/reg-sub
  :application/form-field-options-labels
  (fn [[_ form-key field-id] _]
    [(re-frame/subscribe [:application/form-field form-key field-id])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn [[field lang] _]
    (cond->> (mapv (fn [{:keys [value label]}]
                     {:label (u/from-multi-lang label lang)
                      :value value})
                   (:options field))
             (and (:koodisto-source field)
                  (not (:koodisto-ordered-by-user field)))
             (sort-by :label))))

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
  :application/hakukohteen-tiedot-url
  (fn [_ _] (re-frame/subscribe [:application/hakukohteet]))
  (fn [hakukohteet [_ hakukohde-oid]]
    (get-in hakukohteet [hakukohde-oid :hakukohteen-tiedot-url])))

(re-frame/reg-sub
  :application/hakutoive-nro
  (fn [_ _]
    [(re-frame/subscribe [:application/selected-application])])
  (fn [[selected-application] [_ hakukohde-oid]]
    (when (and hakukohde-oid selected-application)
      (let [hakukohde-oid (if (keyword? hakukohde-oid)
                            (name hakukohde-oid)
                            hakukohde-oid)]
        (-> selected-application
            :hakukohde
            (->> (map-indexed (fn [index oid]
                                (when (= oid hakukohde-oid)
                                  index)))
                 (remove nil?)
                 (first))
            (some-> inc))))))

(re-frame/reg-sub
  :application/hakukohde-and-tarjoaja-name
  (fn [_ _]
    [(re-frame/subscribe [:application/hakukohteet])
     (re-frame/subscribe [:application/fetching-hakukohteet])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn hakukohde-and-tarjoaja-name
    [[hakukohteet fetching-hakukohteet lang] [_ hakukohde-oid]]
    (if-let [hakukohde (get hakukohteet hakukohde-oid)]
      (str (or (u/from-multi-lang (:name hakukohde) lang) hakukohde-oid)
           (when-let [tarjoaja-name (u/from-multi-lang (:tarjoaja-name hakukohde) lang)]
             (str " - " tarjoaja-name)))
      (when (zero? fetching-hakukohteet)
        hakukohde-oid))))

(re-frame/reg-sub
  :application/hakukohde-archived?
  (fn [_ _]
    [(re-frame/subscribe [:application/hakukohteet])])
  (fn hakukohde-archived?
    [[hakukohteet] [_ hakukohde-oid]]
    (boolean (:archived (get hakukohteet hakukohde-oid)))))

(re-frame/reg-sub
  :application/tarjoaja-name
  (fn [db [_ hakukohde-oid]]
    (when-let [hakukohde (get-in db [:hakukohteet hakukohde-oid])]
      (u/from-multi-lang (:tarjoaja-name hakukohde) :fi))))

(re-frame/reg-sub
  :application/hakukohderyhma-name
  (fn [db [_ hakukohderyhma-oid]]
    (when-let [hakukohderyhma (get-in db [:hakukohderyhmat hakukohderyhma-oid])]
      (or (u/from-multi-lang (:name hakukohderyhma) :fi) hakukohderyhma-oid))))

(re-frame/reg-sub
  :application/haku-name
  (fn [_ _]
    [(re-frame/subscribe [:application/haut])
     (re-frame/subscribe [:application/fetching-haut])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn [[haut fetching-haut lang] [_ haku-oid]]
    (haku-name haut fetching-haut haku-oid lang)))

(re-frame/reg-sub
  :application/haun-tiedot-url
  (fn [_ _] (re-frame/subscribe [:application/haut]))
  (fn [haut [_ haku-oid]]
    (get-in haut [haku-oid :haun-tiedot-url])))

(re-frame/reg-sub
  :application/hakukohteet-header
  (fn [_ _]
    [(re-frame/subscribe [:application/hakukohteet-field])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn hakukohteet-header [[hakukohteet-field lang] _]
    (u/from-multi-lang (:label hakukohteet-field) lang)))

(re-frame/reg-sub
  :application/hakukohde-label
  (fn [_ _]
    [(re-frame/subscribe [:application/hakukohde-options-by-oid])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn hakukohde-label [[hakukohde-options lang] [_ hakukohde-oid]]
    (u/from-multi-lang (get-in hakukohde-options [hakukohde-oid :label]) lang)))

(re-frame/reg-sub
  :application/hakukohde-description
  (fn [_ _]
    [(re-frame/subscribe [:application/hakukohde-options-by-oid])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn hakukohde-description [[hakukohde-options lang] [_ hakukohde-oid]]
    (u/from-multi-lang (get-in hakukohde-options [hakukohde-oid :description]) lang)))

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
  (let [last-modify-event-id (-> (filter u/modify-event? events)
                                 last
                                 :id)]
    (map #(if (= (:id %) last-modify-event-id)
            (assoc % :last-modify-event? true)
            %) events)))

(defn- tila-historia->information-request [tila-historia]
  (-> tila-historia
      (select-keys [:luotu :tila])
      (set/rename-keys {:luotu :created-time
                        :tila  :valinnan-tila})
      (assoc :event-type "kevyt-valinta-valinnan-tila-change")))

(defn- valinnan-tulos->information-request [valinnan-tulos]
  (-> valinnan-tulos
      (select-keys [:valinnantilanViimeisinMuutos :valinnantila])
      (set/rename-keys {:valinnantilanViimeisinMuutos :created-time
                        :valinnantila                 :valinnan-tila})
      (assoc :event-type "kevyt-valinta-valinnan-tila-change")))

(re-frame/reg-sub
  :application/events-and-information-requests
  (fn [[_ application-key]]
    [(re-frame/subscribe [:state-query [:application :events]])
     (re-frame/subscribe [:state-query [:application :information-requests]])
     (re-frame/subscribe [:state-query [:hyvaksynnan-ehto application-key]])
     (re-frame/subscribe [:virkailija-kevyt-valinta/show-kevyt-valinta? application-key])
     (re-frame/subscribe [:virkailija-kevyt-valinta/tila-historia-for-application application-key])
     (re-frame/subscribe [:virkailija-kevyt-valinta/valinnan-tulos-for-application application-key])])
  (fn [[events
        information-requests
        hyvaksynnan-ehto
        show-kevyt-valinta?
        tila-historia
        valinnan-tulos]]
    (as-> [] requests
          (->> events mark-last-modify-event (into requests))
          (into requests information-requests)
          (->> hyvaksynnan-ehto
               vals
               (mapcat :events)
               (into requests))
          (cond-> requests
                  show-kevyt-valinta?
                  (into (comp (filter (comp not nil? :tila))
                              (map tila-historia->information-request))
                        tila-historia)
                  (and show-kevyt-valinta? valinnan-tulos)
                  (conj (valinnan-tulos->information-request valinnan-tulos)))
          (sort event-and-information-request-comparator requests))))

(re-frame/reg-sub
  :application/resend-modify-application-link-enabled?
  (fn [db _]
    (-> db :application :modify-application-link :state nil?)))

(re-frame/reg-sub
  :application/applications-to-render
  (fn [db _]
    (take (get-in db [:application :applications-to-render])
          (get-in db [:application :applications]))))

(re-frame/reg-sub
  :application/has-more-applications?
  (fn [db _]
    (contains? (get-in db [:application :sort]) :offset)))

(re-frame/reg-sub
  :application/fetching-applications?
  (fn [db _]
    (and (get-in db [:application :fetching-applications?])
         (not (get-in db [:application :fetching-applications-errored?])))))

(re-frame/reg-sub
 :application/fetching-form-content?
 (fn [db _]
   (get-in db [:application :fetching-form-content?])))

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
  :application/review-settings-visible?
  (fn [db _]
    (get-in db [:application :review-settings :visible?])))

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
  :application/review-notes
  (fn [db]
    (-> db :application :review-notes)))

(re-frame/reg-sub
  :application/selected-review-hakukohde-oids
  (fn [db]
    (set (-> db :application :selected-review-hakukohde-oids))))

(re-frame/reg-sub
 :application/review-note-indexes-excluding-eligibility-for-selected-hakukohteet
 (fn [_ _]
   [(re-frame/subscribe [:application/review-notes])
    (re-frame/subscribe [:application/selected-review-hakukohde-oids])])
 (fn [[notes selected-review-hakukohde-oids] _]
   (->> notes
        (keep-indexed
          (fn [index {:keys [state-name hakukohde]}]
            (when
              (and (not= "eligibility-state" state-name)
                   (or (not hakukohde)
                       (contains? selected-review-hakukohde-oids (str hakukohde))))
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
        [(u/from-multi-lang (:label field) lang)]))

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
    (when-let [changes (u/modify-event-changes events change-history (:id selected-event))]
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
    (u/modify-event-changes events change-history event-id)))

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
  :application/lang
  (fn [db _]
    (or (-> db :application :selected-application-and-form :form :selected-language keyword)
        (-> db :application :selected-application-and-form :application :lang keyword)
        :fi)))

(re-frame.core/reg-sub
  :application/enabled-filter-count
  (fn [db _]
    (reduce (fn [n [category filters]]
              (reduce (fn [n [filter state]]
                        (if (= state (get-in db [:application :filters category filter]))
                          n
                          (inc n)))
                      n
                      filters))
            0
            initial-db/default-filters)))

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

(re-frame.core/reg-sub
  :application/payment-obligation-automatically-checked?
  (fn [db _]
    (let [hakukohde-oids                     (get-in db [:application :selected-review-hakukohde-oids])
          newest-event-automatically-changed (fn [hakukohde-oid] (->> (get-in db [:application :events])
                                                                      (filter #(and (= "payment-obligation" (:review-key %))
                                                                                    (= hakukohde-oid (:hakukohde %))))
                                                                      (sort-by :id >)
                                                                      first
                                                                      :event-type
                                                                      (= "payment-obligation-automatically-changed")))]
      (every? newest-event-automatically-changed hakukohde-oids))))

(re-frame/reg-sub
  :application/all-pohjakoulutus-filters-selected?
  (fn [db _]
    (->> (-> db :application :filters-checkboxes :base-education)
         vals
         (every? true?))))

(re-frame/reg-sub
  :application/applications-have-base-education-answers
  (fn [db _]
    (if-let [applications (-> db :application :applications seq)]
      (some #(not-empty (:base-education %)) applications)
      true)))

(re-frame/reg-sub
  :application/show-eligibility-set-automatically-filter
  (fn [_ _]
    [(re-frame/subscribe [:application/selected-haku-oid])
     (re-frame/subscribe [:application/selected-hakukohde-oid-set])
     (re-frame/subscribe [:application/haut])
     (re-frame/subscribe [:application/hakukohteet])])
  (fn [[haku-oid hakukohde-oids haut hakukohteet] _]
    (if-let [oids (or (seq hakukohde-oids) (seq (get-in haut [haku-oid :hakukohteet])))]
      (or
        (some #(get-in hakukohteet [% :ylioppilastutkinto-antaa-hakukelpoisuuden?]) oids)
        (some #(get-in hakukohteet [% :yo-amm-autom-hakukelpoisuus]) oids))
      (not (contains? hakukohde-oids "form")))))

(re-frame/reg-sub
  :application/loaded-applications-count
  (fn [db _]
    (-> db :application :applications (count))))

(re-frame/reg-sub
  :application/previous-application-fetch-params
  (fn [db _]
    (let [previous-fetch (-> db :application :previous-fetch)]
      (merge
        {:states-and-filters
         {:attachment-states-to-include (:attachment-states previous-fetch)
          :processing-states-to-include (:processing-states previous-fetch)
          :selected-hakukohteet         (selected-hakukohde-oid-set db)
          :filters                      (:filters previous-fetch)}}
        (:params previous-fetch)))))

(re-frame/reg-sub
  :application/hakukohde-selected-for-review?
  (fn [db [_ hakukohde-oid]]
    (contains? (set (get-in db [:application :selected-review-hakukohde-oids]))
               hakukohde-oid)))

(re-frame/reg-sub
  :application/show-hakukierros-paattynyt?
  (fn show-hakukierros-paattynyt? [db _]
    (boolean (:show-hakukierros-paattynyt db))))

(re-frame/reg-sub
  :application/show-creating-henkilo-failed?
  (fn [_ _]
    [(re-frame/subscribe [:application/selected-application])
     (re-frame/subscribe [:application/selected-form])])
  (fn show-creating-henkilo-failed? [[application form] _]
    (and (not (person-info-module/muu-person-info-module? form))
         (nil? (get-in application [:person :oid])))))

(re-frame/reg-sub
  :application/show-tutkinto-fetch-failed?
  (fn [_ _]
    [(re-frame/subscribe [:application/selected-application])])
  (fn show-tutkinto-fetch-failed? [[application] _]
    (and (tutkinto-util/koski-tutkinnot-in-application? application)
         (not (seq (:koski-tutkinnot application))))))

(re-frame/reg-sub
  :application/filter-questions
  (fn [db _]
    (merge
      (get-in db [:application :filters-checkboxes :attachment-review-states])
      (get-in db [:application :filters-checkboxes :question-answer-filtering-options]))))

(re-frame/reg-sub
  :application/filter-attachment-review-states
  (fn [db [_ field-id]]
    (get-in db [:application :filters-checkboxes :attachment-review-states field-id])))

(re-frame/reg-sub
  :application/filter-question-answers-filtering-options
  (fn [db [_ field-id]]
    (get-in db [:application :filters-checkboxes :question-answer-filtering-options field-id :options])))

(re-frame/reg-sub
  :application/pending-selected-school
  (fn [db _]
    (get-in db [:application :school-filter-pending-value])))

(re-frame/reg-sub
  :application/classes-of-selected-school
  (fn [db _]
    (get-in db [:application :selected-school-classes])))

(re-frame/reg-sub
  :application/pending-classes-of-school
  (fn [db _]
    (get-in db [:application :classes-of-school-pending-value])))

(re-frame/reg-sub
  :application/schools-of-departure
  (fn [db _]
    (get-in db [:editor :organizations :schools-of-departure])))

(re-frame/reg-sub
  :application/schools-of-departure-filtered
  (fn [db _]
    (get-in db [:editor :organizations :schools-of-departure-filtered])))

(re-frame/reg-sub
  :application/toisen-asteen-yhteishaku-selected?
  (fn [_]
    [(re-frame/subscribe [:application/selected-haku-oid])
     (re-frame/subscribe [:application/haut])])
  (fn [[selected-haku-oid haut]]
    (and
      (not (nil? selected-haku-oid))
      (haku/toisen-asteen-yhteishaku? (get haut selected-haku-oid)))))

(re-frame/reg-sub
  :application/kk-application-payment-haku-selected?
  (fn [_]
    [(re-frame/subscribe [:application/selected-haku-oid])
     (re-frame/subscribe [:application/haut])])
  (fn [[selected-haku-oid haut]]
    (and
      (not (nil? selected-haku-oid))
      (:maksullinen-kk-haku? (get haut selected-haku-oid)))))

(re-frame/reg-sub
  :application/toisen-asteen-yhteishaku-oid?
  (fn [db [_ haku-oid]]
    (haku/toisen-asteen-yhteishaku? (get-in db [:haut haku-oid]))))

(re-frame/reg-sub
  :application/selected-application-tab
  (fn selected-application-tab [db _]
    (or (get-in db [:application :tab]) "application")))

(re-frame/reg-sub
  :application/forms
  (fn forms [db _]
    (get-in db [:forms])))


