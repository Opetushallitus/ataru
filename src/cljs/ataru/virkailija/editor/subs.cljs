(ns ataru.virkailija.editor.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]
            [taoensso.timbre :refer-macros [spy debug]]))

(re-frame/reg-sub
  :editor/selected-form
  (fn [db]
    (get-in db [:editor :forms (get-in db [:editor :selected-form-key])])))

(re-frame/reg-sub
  :editor/languages
  (fn [db]
    (let [lang-path [:editor :forms (get-in db [:editor :selected-form-key]) :languages]
          languages (map keyword
                         (or (get-in db lang-path) [:fi]))]
      languages)))

(defn- hakukohde-matches?
  [pattern hakukohde]
  (some (partial re-find pattern)
        (map second (:name hakukohde))))

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
  :editor/filtered-active-haut
  (fn [db [_ id]]
    (if-let [search-term (get-in db [:editor :ui id :belongs-to-hakukohteet :modal :search-term])]
      (filter-hakukohteet
         (get-in db [:editor :active-haut :haut] {})
         search-term)
      (get-in db [:editor :active-haut :haut] {}))))

(re-frame/reg-sub
  :editor/fetching-active-haut
  (fn [db]
    (get-in db [:editor :active-haut :fetching?] false)))

(re-frame/reg-sub
  :editor/has-active-haut
  (fn [db]
    (if-let [haut (get-in db [:editor :active-haut :haut])]
      (not-empty haut)
      true)))

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
  :editor/haku-name
  (fn [db [_ haku]]
    (some #(get (:name haku) %) [:fi :sv :en])))

(re-frame/reg-sub
  :editor/hakukohde-name-parts
  (fn [db [_ id hakukohde]]
    (let [name (some #(get (:name hakukohde) %) [:fi :sv :en])]
      (if-let [search-term (get-in db [:editor :ui id :belongs-to-hakukohteet :modal :search-term])]
        (map-indexed (fn [i part] [part (= 1 (mod i 2))])
                     (clojure.string/split name
                                           (re-pattern (str "(?i)(" search-term ")"))))
        [[name false]]))))

(re-frame/reg-sub
  :editor/belongs-to-hakukohde-name
  (fn [db [_ oid]]
    (let [[haku hakukohde] (find-haku-and-hakukohde
                            (map second (get-in db [:editor :active-haut :haut]))
                            oid)]
      (str (get-in hakukohde [:name :fi])
           " - "
           (get-in haku [:name :fi])))))

(re-frame/reg-sub
  :editor/show-belongs-to-hakukohteet-modal
  (fn [db [_ id]]
    (get-in db [:editor :ui id :belongs-to-hakukohteet :modal :show] false)))

(re-frame/reg-sub
  :editor/belongs-to-hakukohteet-modal-search-term-value
  (fn [db [_ id]]
    (get-in db [:editor :ui id :belongs-to-hakukohteet :modal :search-term-value] "")))
