(ns ataru.virkailija.tarjonta
  (:require-macros [ataru.async-macros :as asyncm])
  (:require [cljs.core.async :as async]
            [ajax.core :refer [GET]]
            [cljs-time.core :refer [now after?]]
            [cljs-time.coerce :refer [from-date]]))

(defn- parse-hakuaika
  [hakuaika]
  (cond-> {}
    (contains? hakuaika :start)
    (assoc :alku (from-date (new js/Date (:start hakuaika))))
    (contains? hakuaika :end)
    (assoc :loppu (from-date (new js/Date (:end hakuaika))))))

(defn- parse-haku
  [haku]
  (update haku :hakuajat (partial map parse-hakuaika)))

(defn- active?
  [now haku]
  (some #(or (not (contains? % :loppu))
             (after? (:loppu %) now))
        (:hakuajat haku)))

(defn- fetch-active-haut
  []
  (let [c (async/chan 1)]
    (GET "/lomake-editori/api/tarjonta/haku"
         {:handler (comp #(async/put! c %
                                      (fn [_] (async/close! c)))
                         (partial filter (partial active? (now)))
                         (partial map parse-haku))
          :error-handler #(async/put! c (new js/Error %)
                                      (fn [_] (async/close! c)))
          :response-format :json
          :keywords? true
          :timeout 10000})
    c))

(defn- hakukohteet-by-organization
  [organization-oid]
  (let [c (async/chan 1)]
    (GET (str "/lomake-editori/api/tarjonta/hakukohde?organizationOid="
              organization-oid)
         {:handler #(async/put! c %
                                (fn [_] (async/close! c)))
          :error-handler #(async/put! c (new js/Error %)
                                      (fn [_] (async/close! c)))
          :response-format :json
          :keywords? true
          :timeout 60000})
    c))

(defn- hakukohteet-by-organizations
  [organization-oids]
  (asyncm/go-try
   (loop [cs (map hakukohteet-by-organization organization-oids)
          hakukohteet #{}]
     (if (empty? cs)
       hakukohteet
       (recur (rest cs)
              (clojure.set/union hakukohteet
                                 (set (asyncm/<? (first cs)))))))))

(defn- add-hakukohde
  [haut hakukohde]
  (if (contains? haut (:haku-oid hakukohde))
    (update-in haut [(:haku-oid hakukohde) :hakukohteet]
               (fnil conj []) hakukohde)
    haut))

(defn- add-if-hakukohteet
  [haut [haku-oid haku]]
  (if (contains? haku :hakukohteet)
    (assoc haut haku-oid haku)
    haut))

(defn- haut-by-oid
  [haut]
  (reduce (fn [m haku] (assoc m (:oid haku) haku))
          {}
          haut))

(defn active-haut
  [organization-oids]
  (let [haut-c (fetch-active-haut)
        hakukohteet-c (hakukohteet-by-organizations organization-oids)]
    (asyncm/go-try
      (->> (asyncm/<? hakukohteet-c)
           (reduce add-hakukohde (haut-by-oid (asyncm/<? haut-c)))
           (reduce add-if-hakukohteet {})))))
