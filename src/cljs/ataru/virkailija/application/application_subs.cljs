(ns ataru.virkailija.application.application-subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :application/list-heading
 (fn [db]
   (let [selected-haku       (get-in db [:application :selected-haku])
         selected-hakukohde  (get-in db [:application :selected-hakukohde])
         selected-form-key   (get-in db [:application :selected-form-key])
         forms               (get-in db [:application :forms])]
    (or (:name (get forms selected-form-key))
        (:name selected-hakukohde)
        (:name selected-haku)
        "Valitse haku/hakukohde"))))

(defn filter-haku-seq [haku-seq incomplete-eq]
  (filter #(incomplete-eq (:incomplete %) 0) haku-seq))

(defn filter-haut [haut incomplete-eq]
  (-> haut
      (assoc :direct-form-haut (filter-haku-seq (:direct-form-haut haut) incomplete-eq))
      (assoc :tarjonta-haut (filter-haku-seq (:tarjonta-haut haut) incomplete-eq))))

(defn sort-haku-seq-by-unprocessed [haku-seq]
  (sort-by :unprocessed #(compare %2 %1) haku-seq))

(defn sort-haku-seq-by-name [haku-seq]
  (sort-by :name
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

(re-frame/reg-sub
 :application/incomplete-haut
 (fn [db]
   (when-let [haut (get-in db [:application :haut])]
     (-> haut
         (filter-haut >)
         (sort-haut sort-haku-seq-by-unprocessed)))))

(re-frame/reg-sub
 :application/complete-haut
 (fn [db]
   (when-let [haut (get-in db [:application :haut])]
     (->
      haut
      (filter-haut =)
      (sort-haut sort-haku-seq-by-name)))))
