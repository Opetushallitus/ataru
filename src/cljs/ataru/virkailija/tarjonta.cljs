(ns ataru.virkailija.tarjonta
  (:require-macros [ataru.async-macros :as asyncm])
  (:require [cljs.core.async :as async]
            [ajax.core :refer [GET]]))

(defn- fetch-haku
  [haku-oid]
  (let [c (async/chan 1)]
    (GET (str "/lomake-editori/api/tarjonta/haku/" haku-oid)
         {:handler #(async/put! c %
                                (fn [_] (async/close! c)))
          :error-handler #(async/put! c (new js/Error %)
                                      (fn [_] (async/close! c)))
          :response-format :json
          :keywords? true
          :timeout 15000})
    c))

(defn- fetch-haut
  [haku-oids]
  (asyncm/go-try
   (loop [cs (map fetch-haku haku-oids)
          haut []]
     (if (empty? cs)
       haut
       (recur (rest cs)
              (conj haut (asyncm/<? (first cs))))))))

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
          :timeout 15000})
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
  [organization-oids haku-oids]
  (let [haut-c (fetch-haut haku-oids)
        hakukohteet-c (hakukohteet-by-organizations organization-oids)]
    (asyncm/go-try
      (->> (asyncm/<? hakukohteet-c)
           (reduce add-hakukohde (haut-by-oid (asyncm/<? haut-c)))
           (reduce add-if-hakukohteet {})))))
