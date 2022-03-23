(ns ataru.person-service.person-client
  (:require
   [ataru.cache.cache-service :as cache]
   [ataru.cas.client :as cas]
   [ataru.config.core :refer [config]]
   [ataru.config.url-helper :refer [resolve-url]]
   [ataru.person-service.person-schema :as person-schema]
   [cheshire.core :as json]
   [clojure.core.match :refer [match]]
   [schema.core :as s]
   [taoensso.timbre :as log])
  (:import
   [java.net URLEncoder]))

(defn throw-error [msg]
  (throw (Exception. msg)))

(s/defschema CreateOrFindResponse
  {:status (s/enum :created :exists)
   :oid    s/Str})

(s/defn ^:always-validate create-or-find-person :- CreateOrFindResponse
  [cas-client :- s/Any
   person     :- person-schema/HenkiloPerustieto]
  (let [result (cas/cas-authenticated-post
                cas-client
                (resolve-url :oppijanumerorekisteri-service.person-create) person nil)]
    (match result
      {:status 201 :body body}
      {:status :created :oid (:oidHenkilo (json/parse-string body true))}

      {:status 200 :body body}
      {:status :exists :oid (:oidHenkilo (json/parse-string body true))}

      {:status 400 :body body}
      (throw (new IllegalArgumentException
                  (str "Could not create person, status: " 400
                       " response body: " body)))

      :else
      (throw (new RuntimeException
                  (str "Could not create person, status: " (:status result)
                       "response body: " (:body result)))))))

(defn get-persons [cas-client oids]
  (log/info "Fetching" (count oids) "persons")
  (let [partitions (partition 5000 5000 nil oids)
        results    (map
                     #(cas/cas-authenticated-post
                        cas-client
                        (resolve-url :oppijanumerorekisteri-service.get-persons) % nil)
                     partitions)]
    (reduce
      (fn [acc result]
        (match result
               {:status 200 :body body}
               (->> (json/parse-string body true)
                    (reduce-kv #(assoc %1 (name %2) %3) {})
                    (merge acc))

               :else (throw-error (str "Could not get persons by oids, status: "
                                       (:status result)
                                       "response body: "
                                       (:body result)))))
      {}
      results)))

(defn get-person [cas-client oid]
  (let [result (cas/cas-authenticated-get
                 cas-client
                 (resolve-url :oppijanumerorekisteri-service.get-person oid))]
    (match result
      {:status 200 :body body}
      (json/parse-string body true)

      :else (throw-error (str "Could not get person by oid " oid ", "
                              "status: " (:status result)
                              "response body: "
                              (:body result))))))

(defn- parse-duplicate-henkilos
  [data query-oids]
  (let [link-infos (reduce (fn [acc [master-oid links]]
                             (let [link-info {:master-oid  master-oid
                                              :linked-oids (set (map :henkiloOid links))}]
                               (reduce #(assoc %1 %2 link-info)
                                       (assoc acc master-oid link-info)
                                       (:linked-oids link-info))))
                           {}
                           (group-by :masterOid data))]
    (->> query-oids
         (map #(vector % (update (or (get link-infos %)
                                     {:master-oid  %
                                      :linked-oids #{}})
                                 :linked-oids conj %)))
         (into {}))))

(defn linked-oids [cas-client oids]
  (let [result (cas/cas-authenticated-post
                cas-client
                (resolve-url :oppijanumerorekisteri-service.duplicate-henkilos)
                {:henkiloOids oids}
                nil)]
    (match result
      {:status 200 :body body}
      (parse-duplicate-henkilos (json/parse-string body true) oids)
      :else (throw-error (str "Could not get linked oids for oids " (clojure.string/join ", " oids) ", "
                              "status: " (:status result) ", "
                              "response body: " (:body result))))))

(defrecord PersonCacheLoader [oppijanumerorekisteri-cas-client]
  cache/CacheLoader
  (load [_ key]
    (get-person oppijanumerorekisteri-cas-client key))
  (load-many [_ keys]
    (get-persons oppijanumerorekisteri-cas-client keys))
  (load-many-size [_] 5000)
  (check-schema [_ _] nil))
