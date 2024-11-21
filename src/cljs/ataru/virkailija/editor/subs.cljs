(ns ataru.virkailija.editor.subs
  (:require [ataru.cljs-util :as cu]
            [ataru.tarjonta.haku :as haku]
            [ataru.translations.translation-util :as translations]
            [ataru.util :as util :refer [collect-ids]]
            [ataru.virkailija.editor.editor-selectors :refer [get-all-organizations-have-only-opinto-ohjaaja-rights?
                                                              get-email-template
                                                              get-virkailija-lang]]
            [cljs-time.coerce :as time-coerce]
            [cljs.core.match :refer-macros [match]]
            [clojure.set :as cset]
            [clojure.string :as string]
            [re-frame.core :as re-frame]))

(re-frame/reg-sub
  :editor/virkailija-texts
  (fn [db _]
    (or (-> db :editor :virkailija-texts)
        {})))

(re-frame/reg-sub
  :editor/virkailija-translation
  (fn []
    [(re-frame/subscribe [:editor/virkailija-texts])
     (re-frame/subscribe [:editor/virkailija-lang])])
  (fn [[virkailija-texts lang] [_ translation-key & params]]
    (apply translations/get-translation
           translation-key
           lang
           virkailija-texts
           true
           params)))

(re-frame/reg-sub
  :editor/ui
  (fn ui [db]
    (get-in db [:editor :ui])))

(re-frame/reg-sub
  :editor/form-used-in-hakus
  (fn form-used-in-hakus [db [_ form-key]]
    (get-in db [:editor :form-used-in-hakus form-key])))

(re-frame/reg-sub
  :editor/is-selected-form-used-in-hakus
  (fn is-selected-form-used-in-hakus [db _]
    (let [form-key (get-in db [:editor :selected-form-key])]
      (boolean (seq (get-in db [:editor :form-used-in-hakus form-key] []))))))

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
    (util/non-blank-val (select-keys (:name form) (:languages form)) [lang :fi :sv :en])))

(re-frame/reg-sub
  :editor/form-loading?
  (fn form-loading? [db _]
    (get-in db [:editor :form-loading] false)))

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
  :editor/content-loaded?
  (fn [_ _]
    (re-frame/subscribe [:editor/selected-form]))
  (fn content-loaded? [form]
    (let [content (get form :content)]
      (and content (> (count content) 0)))))

(re-frame/reg-sub
  :editor/get-options-parent
  (fn [[_ & path] _]
    (re-frame/subscribe [:editor/top-level-content (first (flatten path))]))
  (fn get-options-parent [component [_ & path]]
    (get-in component (rest (drop-last 2 (flatten path))))))

(re-frame/reg-sub
  :editor/get-component-value
  (fn [[_ & path] _]
    (re-frame/subscribe [:editor/top-level-content (first (flatten path))]))
  (fn get-component-value [component [_ & path]]
    (get-in component (rest (flatten path)))))

(re-frame/reg-sub
  :editor/get-component-param
  (fn [[_ _ & path] _]
    (re-frame/subscribe [:editor/get-component-value path]))
  (fn [component [_ param & _]]
    (get-in component [:params param])))

(re-frame/reg-sub
  :editor/get-attachment-types-koodisto
  (fn [db]
    (get-in db [:editor :attachment-types-koodisto])))

(re-frame/reg-sub
  :editor/is-per-hakukohde-allowed
  (fn [[_ & path] _]
    (re-frame/subscribe [:editor/top-level-content (first (flatten path))]))
  (fn is-per-hakukohde-allowed [component [_ & path]]
    (let [flattened-path (flatten path)
          has-parent (string/includes? (str flattened-path) ":children")
          too-deep (> (count flattened-path) 3)]
      (and (not too-deep)
           (or (not has-parent)
               (= "wrapperElement" (:fieldClass component)))))))

(defn- followup-parent-path
  [followup-path]
  (match (vec (reverse followup-path))
    [_ :followups _ :options & rest]
    (vec (reverse rest))

    :else
    nil))

(re-frame/reg-sub
  :editor/has-parent-per-hakukohde
  (fn [_ [_ path]]
    (when-let [parent-path (followup-parent-path path)]
      (let [parent @(re-frame/subscribe [:editor/get-component-value parent-path])]
        (:per-hakukohde parent)))))

(re-frame/reg-sub
  :editor/get-range-value
  (fn [db [_ id range & path]]
    (or (get-in db [:editor :ui id range :value])
        @(re-frame/subscribe [:editor/get-component-value path :params range])
        "")))

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
  :editor/superuser?
  (fn [db _]
    (get-in db [:editor :user-info :superuser?])))

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
  (fn [_ [_ hakukohde]]
    (let [hakukohde-name (some #(get (:name hakukohde) %) [:fi :sv :en])
          tarjoaja-name (some #(get (:tarjoaja-name hakukohde) %) [:fi :sv :en])
          name (str hakukohde-name " - " tarjoaja-name)]
      name)))

(re-frame/reg-sub
  :editor/name-parts
  (fn [db [_ id name]]
    (if-let [search-term (get-in db [:editor :ui id :belongs-to-hakukohteet :modal :search-term])]
      (map-indexed (fn [i part] [part (= 1 (mod i 2))])
                   (string/split name
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
        (or cut? (empty? unique-ids))
        (and (not cut?)
             (empty? (cset/intersection unique-ids unique-ids-in-form)))))))

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
  :editor/yhteishaku?
  (fn [_ _]
    [(re-frame/subscribe [:editor/used-by-haut-haut])])
  (fn [[haut] [_]]
    (boolean (some :yhteishaku (vals haut)))))

(re-frame/reg-sub
  :editor/toisen-asteen-yhteishaku?
  (fn [_ _]
    [(re-frame/subscribe [:editor/used-by-haut-haut])])
  (fn [[haut] [_]]
    (some haku/toisen-asteen-yhteishaku? (vals haut))))

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
    (select-keys form [:locked :locked-by :lock-state])))

(re-frame/reg-sub
  :editor/form-locked?
  (fn [_ _]
    (re-frame/subscribe [:editor/selected-form]))
  (fn form-locked? [form _]
    (some? (:locked form))))

(re-frame/reg-sub
  :editor/form-locked-or-has-haku?
  (fn [_ _]
    [(re-frame/subscribe [:editor/form-locked?])
     (re-frame/subscribe [:editor/fetching-haut?])
     (re-frame/subscribe [:editor/is-selected-form-used-in-hakus])])
  (fn form-locked-or-has-haku? [[form-locked? fetching-haut? form-used-in-hakus] _]
    (or form-locked? fetching-haut? form-used-in-hakus)))

(re-frame/reg-sub
  :editor/form-contains-applications?
  (fn [_ _]
    (re-frame/subscribe [:editor/selected-form]))
  (fn form-contains-applications? [form _]
    (> (:application-count form) 0)))

(re-frame/reg-sub
  :editor/component-locked?
  (fn [[_ path] _]
    [(re-frame/subscribe [:editor/form-locked?])
     (re-frame/subscribe [:editor/get-component-value path])])
  (fn [[form-locked? field] _]
    (or form-locked?
        (get-in field [:metadata :locked] false))))

(re-frame/reg-sub
  :editor/dropdown-with-selection-limit?
  (fn [[_ & path] _]
    (re-frame/subscribe [:editor/get-component-value path]))
  (fn [component [_ & path]]
    (and
     (= (:fieldType component) "singleChoice")
     (> (count (flatten path)) 1)
     (not
       (loop [part (butlast (rest (flatten path)))]
         (when-let [parent (get-in component part)]
           (if (= (:fieldClass parent) "questionGroup")
             true
             (when part
               (recur (butlast part))))))))))

(re-frame/reg-sub
  :editor/has-validator?
  (fn [[_ _ & path] _]
    (re-frame/subscribe [:editor/get-component-value path]))
  (fn [component [_ validator & _]]
    (contains? (set (:validators component)) validator)))

(re-frame/reg-sub
  :editor/selection-limit?
  (fn [[_ & path] _]
    (re-frame/subscribe [:editor/has-validator? "selection-limit" path]))
  (fn [has-validator _]
    has-validator))

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
     (re-frame/subscribe [:editor/form-locked?])
     (re-frame/subscribe [:editor/form-contains-applications?])])
  (fn remove-form-button-state [[ui form-locked? form-contains-applications?] _]
    (if (or form-locked?
            form-contains-applications?)
      :disabled
      (get ui :remove-form-button-state :active))))

(re-frame/reg-sub
  :editor/copy-component
  (fn copy-component-path [db _]
    (get-in db [:editor :copy-component])))

(re-frame/reg-sub
  :editor/load-spinner-running?
  (fn load-spinner-running? [db _]
    (let [amount-in-spinner (get-in db [:editor :ui :spinner])]
      (and (some? amount-in-spinner) (> amount-in-spinner 0)))))

(re-frame/reg-sub
  :editor/component-button-state
  (fn [[_ path] _]
    [(re-frame/subscribe [:editor/ui])
     (re-frame/subscribe [:editor/component-locked? path])
     (re-frame/subscribe [:editor/copy-component])
     (re-frame/subscribe [:editor/get-component-value path])
     (re-frame/subscribe [:editor/selected-form-key])])
  (fn component-button-state [[ui component-locked? copy-component field selected-form-key] [_ path button-type]]
    (case button-type
      :copy
      (cond (nil? copy-component)
            :enabled
            (and (= selected-form-key (:copy-component-form-key copy-component))
                 (= path (:copy-component-path copy-component))
                 (not (:copy-component-cut? copy-component)))
            :active
            :else
            :disabled)
      :cut
      (cond component-locked?
            :disabled
            (nil? copy-component)
            :enabled
            (and (= selected-form-key (:copy-component-form-key copy-component))
                 (= path (:copy-component-path copy-component))
                 (:copy-component-cut? copy-component))
            :active
            :else
            :disabled)
      :remove
      (cond (or component-locked? (some? copy-component))
            :disabled
            (= :confirm (get-in ui [(:id field) :remove]))
            :confirm
            :else
            :enabled))))

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
  :editor/current-lomakeosiot
  (fn [db _]
    (let [content (get-selected-form-content db)]
      (filter #(= "wrapperElement" (:fieldClass %)) content))))

(re-frame/reg-sub
  :editor/base-education-module-exists?
  (fn [db _]
    (let [content (get-selected-form-content db)]
      (some #{"completed-base-education" "higher-base-education-module" "pohjakoulutus-2nd-wrapper"}
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
  :editor/tutkinnot-component-exists?
  (fn [db _]
    (->> (get-selected-form-content db)
         util/flatten-form-fields
         (some #(= "koski-tutkinnot-wrapper" (:id %))))))

(re-frame/reg-sub
  :editor/email-template
  (fn [db _]
    (get-email-template db)))

(re-frame/reg-sub
  :editor/virkailija-lang
  (fn [db _]
    (get-virkailija-lang db)))

(re-frame/reg-sub
  :editor/opinto-ohjaaja?
  (fn [db _]
    (let [user-info (-> db :editor :user-info)]
      (some (fn [org] (some #(= "opinto-ohjaaja" %) (:rights org))) (:organizations user-info)))))

(re-frame/reg-sub
  :editor/opinto-ohjaaja-or-admin?
  (fn [_ _]
    [(re-frame/subscribe [:editor/opinto-ohjaaja?])
     (re-frame/subscribe [:editor/superuser?])])
  (fn [[opinto-ohjaaja? superuser?] _]
    (or opinto-ohjaaja? superuser?)))

(re-frame/reg-sub
 :editor/edit-rights-for-any-organization?
 (fn [db _]
   (let [user-info (-> db :editor :user-info)]
     (some (fn [org] (some #(= "edit-applications" %) (:rights org))) (:organizations user-info)))))

(re-frame/reg-sub
  :editor/all-organizations-have-only-opinto-ohjaaja-rights?
  (fn [db _]
    (get-all-organizations-have-only-opinto-ohjaaja-rights? db)))

(re-frame/reg-sub
  :editor/autosave-enabled?
  (fn [db _]
    (some? (-> db :editor :autosave))))

(re-frame/reg-sub
  :editor/form-properties
  (fn [_ _]
    (re-frame/subscribe [:editor/selected-form]))
  (fn [selected-form]
    (get selected-form :properties)))

(re-frame/reg-sub
  :editor/auto-expand-hakukohteet
  (fn [_ _]
    (re-frame/subscribe [:editor/form-properties]))
  (fn [form-properties]
    (get form-properties :auto-expand-hakukohteet)))

(re-frame/reg-sub
 :editor/order-hakukohteet-by-opetuskieli
 (fn [_ _]
   (re-frame/subscribe [:editor/form-properties]))
 (fn [form-properties]
   (get form-properties :order-hakukohteet-by-opetuskieli)))

(re-frame/reg-sub
  :editor/today
  (fn [db _]
    (-> db
      (get-in [:editor :today])
      (time-coerce/from-date))))

(re-frame/reg-sub
  :editor/visibility-condition-value
  (fn [[_ path visibility-condition-index] _]
    (re-frame/subscribe [:editor/get-component-value path :section-visibility-conditions visibility-condition-index]))
  (fn [visibility-condition _]
    (some-> visibility-condition :condition :answer-compared-to)))

(re-frame/reg-sub
  :editor/visibility-condition-section-name
  (fn [[_ path visibility-condition-index] _]
    (re-frame/subscribe [:editor/get-component-value path :section-visibility-conditions visibility-condition-index]))
  (fn [visibility-condition _]
    (:section-name visibility-condition)))

(re-frame/reg-sub
  :editor/invalid-option-validator-present?
  (fn [[_ & option-path] _]
    (let [parent-path (pop (pop (into [] (flatten option-path))))]
      [(re-frame/subscribe [:editor/has-validator? "invalid-values" parent-path])
       (re-frame/subscribe [:editor/get-component-param :invalid-values parent-path])
       (re-frame/subscribe [:editor/get-component-value option-path])]))
  (fn [[parent-has-validator invalid-values option-component]]
    (and
      parent-has-validator
      (contains? (set invalid-values) (:value option-component)))))

(re-frame/reg-sub
  :editor/allow-only-yhteishaut?
  (fn [_ _]
    (re-frame/subscribe [:editor/form-properties]))
  (fn [form-properties]
    (get form-properties :allow-only-yhteishaut false)))

(re-frame/reg-sub
  :editor/allow-hakeminen-tunnistautuneena?
  (fn [_ _]
    (re-frame/subscribe [:editor/form-properties]))
  (fn [form-properties]
    (get form-properties :allow-hakeminen-tunnistautuneena false)))

(re-frame/reg-sub
  :editor/maksutiedot
  (fn [_ _]
    (re-frame/subscribe [:editor/form-properties]))
  (fn [form-properties]
    (get form-properties :payment)))

(re-frame/reg-sub
  :editor/form-closed?
  (fn [_ _]
    (re-frame/subscribe [:editor/form-properties]))
  (fn [form-properties]
    (get form-properties :closed false)))

(re-frame/reg-sub
  :editor/get-selected-property-options
  (fn [[_ _] _]
    (re-frame/subscribe [:editor/form-properties]))
  (fn [form-properties [_ category default]]
    (get-in form-properties [(keyword category) :selected-option-ids] default)))

(re-frame/reg-sub
  :editor/get-property-value
  (fn [[_ _] _]
    (re-frame/subscribe [:editor/form-properties]))
  (fn [form-properties [_ category property]]
    (get-in form-properties [(keyword category)(keyword property)])))
