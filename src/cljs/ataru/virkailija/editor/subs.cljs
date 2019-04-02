(ns ataru.virkailija.editor.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [ataru.util :as util :refer [collect-ids]]
            [re-frame.core :as re-frame]
            [ataru.cljs-util :as cu]
            [taoensso.timbre :refer-macros [spy debug]]
            [markdown.core :as md]))

(re-frame/reg-sub
  :editor/ui
  (fn ui [db]
    (get-in db [:editor :ui])))

(re-frame/reg-sub
  :editor/form-keys
  (fn form-keys [db _]
    (get-in db [:editor :sorted-form-keys])))

(re-frame/reg-sub
  :editor/selected-form-key
  (fn selected-form-key [db _]
    (get-in db [:editor :selected-form-key])))

(re-frame/reg-sub
  :editor/form
  (fn form [db [_ key]]
    (get-in db [:editor :forms key])))

(re-frame/reg-sub
  :editor/form-name
  (fn [[_ key] _]
    [(re-frame/subscribe [:editor/form key])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn form-name [[form lang] _]
    (util/non-blank-val (:name form) [lang :fi :sv :en])))

(re-frame/reg-sub
  :editor/form-created-by
  (fn [[_ key] _]
    (re-frame/subscribe [:editor/form key]))
  (fn form-created-by [form _]
    (:created-by form)))

(re-frame/reg-sub
  :editor/form-created-time
  (fn [[_ key] _]
    (re-frame/subscribe [:editor/form key]))
  (fn form-created-time [form _]
    (:created-time form)))

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
  :editor/unique-ids-in-form
  (fn [db _]
    (set
      (remove cu/valid-uuid?
        (reduce collect-ids [] (get-in db [:editor :forms (-> db :editor :selected-form-key) :content]))))))

(re-frame/reg-sub
  :editor/can-copy-or-paste?
  (fn [_ _]
    [(re-frame/subscribe [:editor/copy-component])
     (re-frame/subscribe [:editor/selected-form-key])
     (re-frame/subscribe [:editor/unique-ids-in-form])])
  (fn [[copy-component selected-form-key unique-ids-in-form]]
    (let [{form-key   :copy-component-form-key
           cut?       :copy-component-cut?
           unique-ids :copy-component-unique-ids} copy-component
          same-form?        (= selected-form-key form-key)]
      (if same-form?
        (or cut?
            (and (not cut?) (empty? unique-ids)))
        (and (not cut?)
             (empty? (clojure.set/intersection unique-ids unique-ids-in-form)))))))

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
  (fn [_ _]
    (re-frame/subscribe [:editor/ui]))
  (fn [ui [_ id]]
    (get-in ui [id :belongs-to-hakukohteet :modal :show] false)))

(re-frame/reg-sub
  :editor/belongs-to-hakukohteet-modal-search-term-value
  (fn [_ _]
    (re-frame/subscribe [:editor/ui]))
  (fn [ui [_ id]]
    (get-in ui [id :belongs-to-hakukohteet :modal :search-term-value] "")))

(re-frame/reg-sub
  :editor/belongs-to-hakukohteet-modal-show-more-value
  (fn [_ _]
    (re-frame/subscribe [:editor/ui]))
  (fn [ui [_ id haku-oid]]
    (get-in ui [id :belongs-to-hakukohteet :modal haku-oid :show-more-value] 15)))

(re-frame/reg-sub
  :editor/folded?
  (fn [_ _]
    (re-frame/subscribe [:editor/ui]))
  (fn [ui [_ id]]
    (get-in ui [id :folded?] false)))

(re-frame/reg-sub
  :editor/path-folded?
  (fn [[_ path] _]
    [(re-frame/subscribe [:editor/ui])
     (re-frame/subscribe [:editor/get-component-value path])])
  (fn [[ui component] _]
    (get-in ui [(:id component) :folded?] false)))

(re-frame/reg-sub
  :editor/form-locked-info
  (fn [_ _]
    (re-frame/subscribe [:editor/selected-form]))
  (fn form-locked-info [form _]
    (when (some? (:locked form))
      (select-keys form [:locked :locked-by]))))

(re-frame/reg-sub
  :editor/form-locked?
  (fn [_ _]
    (re-frame/subscribe [:editor/selected-form]))
  (fn form-locked? [form _]
    (some? (:locked form))))

(re-frame/reg-sub
  :editor/this-form-locked?
  (fn [[_ key] _]
    (re-frame/subscribe [:editor/form key]))
  (fn this-form-locked? [form _]
    (some? (:locked form))))

(re-frame/reg-sub
  :editor/remove-form-button-state
  (fn [_ _]
    [(re-frame/subscribe [:editor/ui])
     (re-frame/subscribe [:editor/form-locked?])])
  (fn remove-form-button-state [[ui form-locked?] _]
    (if form-locked?
      :disabled
      (get ui :remove-form-button-state :active))))

(re-frame/reg-sub
  :editor/component-button-state
  (fn [_ _]
    [(re-frame/subscribe [:editor/ui])
     (re-frame/subscribe [:editor/form-locked?])])
  (fn component-button-state [[ui form-locked?] [_ component-type path]]
    (if form-locked?
      :disabled
      (get-in ui [:component-button-state component-type path] :active))))

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

(re-frame/reg-sub
  :editor/copy-component
  (fn copy-component-path [db _]
    (get-in db [:editor :copy-component])))
