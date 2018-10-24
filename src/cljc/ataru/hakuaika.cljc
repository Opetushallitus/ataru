(ns ataru.hakuaika
  (:require  #?(:clj [clj-time.core :as t]
                :cljs [cljs-time.core :as t])
             #?(:clj [clj-time.coerce :as c]
                :cljs [cljs-time.coerce :as c])))

(defn- select-first-ongoing-hakuaika-or-hakuaika-with-last-ending [hakuajat]
  (if-let [ongoing-hakuaika (first (filter :on hakuajat))]
    ongoing-hakuaika
    (first (sort-by :end > (filter :end hakuajat)))))

(defn select-hakuaika-for-haku [hakuajat]
  (let [longest-open (->> hakuajat
                          (filter :on)
                          (sort-by :end >)
                          first)
        next-open    (->> hakuajat
                          (remove :on)
                          (filter #(t/after? (c/from-long (:start %)) (t/now)))
                          (sort-by :start <)
                          first)
        last-open    (->> hakuajat
                          (sort-by :end >)
                          first)]
    (or longest-open
        next-open
        last-open)))

(defn select-hakuaika-for-field [field hakukohteet]
  (let [field-hakukohde-and-group-oids (set (concat (:belongs-to-hakukohteet field)
                                                    (:belongs-to-hakukohderyhma field)))
        relevant-hakukohteet (cond->> hakukohteet
                                      (not-empty field-hakukohde-and-group-oids)
                                      (filter #(not-empty (clojure.set/intersection field-hakukohde-and-group-oids
                                                                                    (set (cons (:oid %) (:hakukohderyhmat %)))))))]
    (or (select-first-ongoing-hakuaika-or-hakuaika-with-last-ending
          (map :hakuaika relevant-hakukohteet))
        (select-hakuaika-for-haku (map :hakuaika hakukohteet)))))

(defn attachment-edit-end [hakuaika default-modify-grace-period]
  (let [hakuaika-end (some-> hakuaika :end c/from-long)]
    (some-> hakuaika-end (t/plus (t/days (or (:attachment-modify-grace-period-days hakuaika)
                                             default-modify-grace-period))))))
