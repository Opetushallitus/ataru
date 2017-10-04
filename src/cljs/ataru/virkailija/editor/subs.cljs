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
  (or (some (partial re-find pattern)
            (map second (:name hakukohde)))
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
  :editor/filtered-haut
  (fn [db [_ id]]
    (if-let [search-term (get-in db [:editor :ui id :belongs-to-hakukohteet :modal :search-term])]
      (filter-hakukohteet
         (get-in db [:editor :used-by-haut :haut] {})
         search-term)
      (get-in db [:editor :used-by-haut :haut] {}))))

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
  :editor/haku-name
  (fn [db [_ haku]]
    (some #(get (:name haku) %) [:fi :sv :en])))

(re-frame/reg-sub
  :editor/hakukohde-name-parts
  (fn [db [_ id hakukohde]]
    (let [hakukohde-name (some #(get (:name hakukohde) %) [:fi :sv :en])
          tarjoaja-name (some #(get (:tarjoaja-name hakukohde) %) [:fi :sv :en])
          name (str hakukohde-name " - " tarjoaja-name)]
      (if-let [search-term (get-in db [:editor :ui id :belongs-to-hakukohteet :modal :search-term])]
        (map-indexed (fn [i part] [part (= 1 (mod i 2))])
                     (clojure.string/split name
                                           (re-pattern (str "(?i)(" search-term ")"))))
        [[name false]]))))

(re-frame/reg-sub
  :editor/belongs-to-hakukohde-name
  (fn [db [_ oid]]
    (let [multiple-haku? (< 1 (count (get-in db [:editor :used-by-haut :haut] {})))
          [haku hakukohde] (find-haku-and-hakukohde
                            (map second (get-in db [:editor :used-by-haut :haut]))
                            oid)
          hakukohde-name (some #(get (:name hakukohde) %) [:fi :sv :en])
          tarjoaja-name (some #(get (:tarjoaja-name hakukohde) %) [:fi :sv :en])
          haku-name (some #(get (:name haku) %) [:fi :sv :en])]
      (str hakukohde-name
           " - "
           tarjoaja-name
           (when multiple-haku?
             (str " - " haku-name))))))

(re-frame/reg-sub
  :editor/show-belongs-to-hakukohteet-modal
  (fn [db [_ id]]
    (get-in db [:editor :ui id :belongs-to-hakukohteet :modal :show] false)))

(re-frame/reg-sub
  :editor/belongs-to-hakukohteet-modal-search-term-value
  (fn [db [_ id]]
    (get-in db [:editor :ui id :belongs-to-hakukohteet :modal :search-term-value] "")))
