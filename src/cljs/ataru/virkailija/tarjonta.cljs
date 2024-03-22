(ns ataru.virkailija.tarjonta
  (:require-macros [ataru.async-macros :as asyncm])
  (:require [cljs.core.async :as async]
            [ajax.core :refer [GET]]))

(defn- try-reduce
  [f init channel]
  (asyncm/go-try
   (loop [acc init]
     (if-let [x (asyncm/<? channel)]
       (recur (f acc x))
       acc))))

(defn- fetch-hakukohteet
  [haku-oid organization-oid c]
  (GET (str "/lomake-editori/api/tarjonta/hakukohde"
            "?hakuOid=" haku-oid
            "&organizationOid=" organization-oid)
       {:handler #(async/put! c %
                              (fn [_] (async/close! c)))
        :error-handler #(async/put! c (new js/Error %)
                                    (fn [_] (async/close! c)))
        :response-format :json
        :keywords? true
        :timeout 15000
        :headers {"Caller-Id" (aget js/config "virkailija-caller-id")}}))

(defn- fetch-haku
  [haku-oid c]
  (GET (str "/lomake-editori/api/tarjonta/haku/" haku-oid)
       {:handler #(async/put! c %
                              (fn [_] (async/close! c)))
        :error-handler #(async/put! c (new js/Error %)
                                    (fn [_] (async/close! c)))
        :response-format :json
        :keywords? true
        :timeout 15000
        :headers {"Caller-Id" (aget js/config "virkailija-caller-id")}}))

(defn- fetch-haku-with-hakukohteet
  [organization-oids haku-oid c]
  (let [haku-c (async/chan 1)
        organization-oids-c (async/chan (count organization-oids))
        hakukohteet-c (async/chan (count organization-oids))]
    (fetch-haku haku-oid haku-c)
    (async/onto-chan! organization-oids-c organization-oids)
    (async/pipeline-async 1
                          hakukohteet-c
                          (partial fetch-hakukohteet haku-oid)
                          organization-oids-c)
    (async/pipe
     (asyncm/go-try
      (assoc (asyncm/<? haku-c)
             :hakukohteet
             (vec (distinct (asyncm/<? (try-reduce concat [] hakukohteet-c))))))
     c)))

(defn fetch-haut-with-hakukohteet
  [haku-oids organization-oids]
  (let [haku-oids-c (async/chan (count haku-oids))
        haut-c      (async/chan (count haku-oids))]
    (async/onto-chan! haku-oids-c haku-oids)
    (async/pipeline-async 1
                          haut-c
                          (partial fetch-haku-with-hakukohteet organization-oids)
                          haku-oids-c)
    (try-reduce (fn [m haku] (assoc m (:oid haku) haku))
      {}
      haut-c)))
