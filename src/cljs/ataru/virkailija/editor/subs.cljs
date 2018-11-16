(ns ataru.virkailija.editor.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [ataru.util :as util]
            [re-frame.core :as re-frame]
            [taoensso.timbre :refer-macros [spy debug]]
            [markdown.core :as md]))

(re-frame/reg-sub
  :editor/selected-form
  (fn [db]
    (get-in db [:editor :forms (get-in db [:editor :selected-form-key])])))

(re-frame/reg-sub
  :editor/top-level-content
  (fn [_ _]
    (re-frame/subscribe [:editor/selected-form]))
  (fn top-level-content [form [_ i]]
    (get-in form [:content i])))

(re-frame/reg-sub
  :editor/get-component-value
  (fn [[_ & path] _]
    (re-frame/subscribe [:editor/top-level-content (first (flatten path))]))
  (fn get-component-value [component [_ & path]]
    (get-in component (rest (flatten path)))))

(re-frame/reg-sub
  :editor/languages
  (fn [db]
    (let [lang-path [:editor :forms (get-in db [:editor :selected-form-key]) :languages]
          languages (map keyword
                         (or (get-in db lang-path) [:fi]))]
      languages)))

(defn- name-matches?
  [pattern named]
  (boolean (re-find pattern (some (:name named) [:fi :sv :en]))))

(defn- hakukohde-matches?
  [pattern hakukohde]
  (or (name-matches? pattern hakukohde)
      (some (partial re-find pattern)
            (map second (:tarjoaja-name hakukohde)))))

(defn- filter-hakukohteet
  [haut search-term]
  (let [pattern (re-pattern (str "(?i)" search-term))
        filter-f (partial hakukohde-matches? pattern)]
    (reduce-kv (fn [haut oid haku]
                 (let [hakukohteet (filter filter-f (:hakukohteet haku))]
                   (if (empty? hakukohteet)
                     haut
                     (assoc haut oid (assoc haku :hakukohteet hakukohteet)))))
               {}
               haut)))

(re-frame/reg-sub
  :editor/used-by-haut-haut
  (fn [db _]
    (get-in db [:editor :used-by-haut :haut])))

(re-frame/reg-sub
  :editor/used-by-haut-hakukohderyhmat
  (fn [db _]
    (get-in db [:editor :used-by-haut :hakukohderyhmat])))

(re-frame/reg-sub
  :editor/filtered-haut
  (fn [db [_ id]]
    (if-let [search-term (get-in db [:editor :ui id :belongs-to-hakukohteet :modal :search-term])]
      (filter-hakukohteet
        (get-in db [:editor :used-by-haut :haut] {})
        search-term)
      (get-in db [:editor :used-by-haut :haut] {}))))

(re-frame/reg-sub
  :editor/filtered-hakukohderyhmat
  (fn [db [_ id]]
    (if-let [search-term (get-in db [:editor :ui id :belongs-to-hakukohteet :modal :search-term])]
      (let [pattern (re-pattern (str "(?i)" search-term))]
        (filter (partial name-matches? pattern)
                (get-in db [:editor :used-by-haut :hakukohderyhmat])))
      (get-in db [:editor :used-by-haut :hakukohderyhmat] []))))

(re-frame/reg-sub
  :editor/fetching-haut?
  (fn [db]
    (get-in db [:editor :used-by-haut :fetching?] false)))

(re-frame/reg-sub
  :editor/used-by-haku?
  (fn [db]
    (or (get-in db [:editor :used-by-haut :error?])
        (get-in db [:editor :used-by-haut :fetching?])
        (not-empty (get-in db [:editor :used-by-haut :haut])))))

(defn- find-hakukohde
  [haku hakukohde-oid]
  (some #(when (= hakukohde-oid (:oid %)) %)
        (:hakukohteet haku)))

(defn- find-haku-and-hakukohde
  [haut hakukohde-oid]
  (some #(when-let [hakukohde (find-hakukohde % hakukohde-oid)]
           [% hakukohde])
        haut))

(re-frame/reg-sub
  :editor/get-some-name
  (fn [db [_ {:keys [name]}]]
    (some #(get name %) [:fi :sv :en])))

(re-frame/reg-sub
  :editor/get-hakukohde-name
  (fn [db [_ hakukohde]]
    (let [hakukohde-name (some #(get (:name hakukohde) %) [:fi :sv :en])
          tarjoaja-name (some #(get (:tarjoaja-name hakukohde) %) [:fi :sv :en])
          name (str hakukohde-name " - " tarjoaja-name)]
      name)))

(re-frame/reg-sub
  :editor/name-parts
  (fn [db [_ id name]]
    (if-let [search-term (get-in db [:editor :ui id :belongs-to-hakukohteet :modal :search-term])]
      (map-indexed (fn [i part] [part (= 1 (mod i 2))])
                   (clojure.string/split name
                                         (re-pattern (str "(?i)(" search-term ")"))))
      [[name false]])))

(re-frame/reg-sub
  :editor/belongs-to-hakukohderyhma-name
  (fn [_ _]
    [(re-frame/subscribe [:editor/used-by-haut-hakukohderyhmat])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn [[hakukohderyhmat lang] [_ oid]]
    (when-let [hakukohderyhma (some #(when (= oid (:oid %)) %)
                                    hakukohderyhmat)]
      (str (util/non-blank-val (:name hakukohderyhma) [lang :fi :sv :en])))))

(re-frame/reg-sub
  :editor/belongs-to-hakukohde-name
  (fn [_ _]
    [(re-frame/subscribe [:editor/used-by-haut-haut])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn [[haut lang] [_ oid]]
    (let [multiple-haku?   (< 1 (count haut))
          [haku hakukohde] (find-haku-and-hakukohde (map second haut) oid)]
      (str (util/non-blank-val (:name hakukohde) [lang :fi :sv :en])
           " - "
           (util/non-blank-val (:tarjoaja-name hakukohde) [lang :fi :sv :en])
           (when multiple-haku?
             (str " - " (util/non-blank-val (:name haku) [lang :fi :sv :en])))))))

(re-frame/reg-sub
  :editor/show-belongs-to-hakukohteet-modal
  (fn [db [_ id]]
    (get-in db [:editor :ui id :belongs-to-hakukohteet :modal :show] false)))

(re-frame/reg-sub
  :editor/belongs-to-hakukohteet-modal-search-term-value
  (fn [db [_ id]]
    (get-in db [:editor :ui id :belongs-to-hakukohteet :modal :search-term-value] "")))

(re-frame/reg-sub
  :editor/belongs-to-hakukohteet-modal-show-more-value
  (fn [db [_ id haku-oid]]
    (get-in db [:editor :ui id :belongs-to-hakukohteet :modal haku-oid :show-more-value] 15)))

(re-frame/reg-sub
  :editor/belongs-to-other-organization?
  (fn [db [_ {:keys [belongs-to-hakukohteet belongs-to-hakukohderyhma]}]]
    (let [my-hakukohteet (->> (get-in db [:editor :used-by-haut :haut])
                              vals
                              (mapcat :hakukohteet)
                              (map :oid)
                              set)
          my-hakukohderyhmat (->> (get-in db [:editor :used-by-haut :hakukohderyhmat])
                                  (map :oid)
                                  set)]
      (and (or (not-empty belongs-to-hakukohteet)
               (not-empty belongs-to-hakukohderyhma))
           (empty? (clojure.set/intersection (set belongs-to-hakukohteet)
                                             my-hakukohteet))
           (empty? (clojure.set/intersection (set belongs-to-hakukohderyhma)
                                             my-hakukohderyhmat))))))

(re-frame/reg-sub
  :editor/folded?
  (fn [db [_ id]]
    (get-in db [:editor :ui id :folded?] false)))

(defn- current-form-locked? [db]
  (let [current-form (get-in db [:editor :forms (get-in db [:editor :selected-form-key])])]
    (when (some? (:locked current-form))
      (select-keys current-form [:locked :locked-by]))))

(re-frame/reg-sub
  :editor/current-form-locked
  (fn [db _]
    (current-form-locked? db)))

(re-frame/reg-sub
  :editor/remove-form-button-state
  (fn [db _]
    (get-in db [:editor :ui :remove-form-button-state]
            (if (and (some? (get-in db [:editor :selected-form-key]))
                     (nil? (current-form-locked? db)))
              :active
              :disabled))))

(re-frame/reg-sub
  :editor/remove-component-button-state
  (fn [db [_ path]]
    (if (current-form-locked? db)
      :disabled
      (get-in db [:editor :ui :remove-component-button-state path] :active))))

(re-frame/reg-sub
  :editor/email-templates-altered
  (fn [db _]
    (let [form-key (get-in db [:editor :selected-form-key])
          templates (get-in db [:editor :email-template form-key])]
      (into
        {}
        (map
          (fn [[lang template]]
            (let [any-changes? (not= (dissoc template :stored-content) (get template :stored-content))]
              [lang any-changes?]))
          templates)))))

(defn- get-selected-form-content [db]
  (let [selected-form-key (-> db :editor :selected-form-key)]
    (-> db
        :editor
        :forms
        (get selected-form-key)
        :content)))

(re-frame/reg-sub
  :editor/base-education-module-exists?
  (fn [db _]
    (let [content (get-selected-form-content db)]
      (some #{"completed-base-education" "higher-base-education-module"}
            (->> content
                 (mapcat :children)
                 (map :id)
                 (concat (map :id content))
                 set)))))

(re-frame/reg-sub
  :editor/pohjakoulutusristiriita-exists?
  (fn [db _]
    (->> (get-selected-form-content db)
         util/flatten-form-fields
         (some #(= "pohjakoulutusristiriita" (:id %))))))

(re-frame/reg-sub
  :editor/email-template
  (fn [db _]
    (get-in db [:editor :email-template (get-in db [:editor :selected-form-key])])))

(re-frame/reg-sub
  :editor/virkailija-lang
  (fn [db _]
    (or (-> db :editor :user-info :lang keyword) :fi)))

(re-frame/reg-sub
  :editor/autosave-enabled?
  (fn [db _]
    (some? (-> db :editor :autosave))))