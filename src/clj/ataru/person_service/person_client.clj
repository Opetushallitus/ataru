(ns ataru.person-service.person-client
  (:require
   [taoensso.timbre :as log]
   [cheshire.core :as json]
   [ataru.config.core :refer [config]]
   [schema.core :as s]
   [clojure.core.match :refer [match]]
   [ataru.cas.client :as cas]
   [ataru.config.url-helper :refer [resolve-url]]
   [ataru.person-service.person-schema :as person-schema])
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
                (resolve-url :oppijanumerorekisteri-service.person-create) person)]
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
                        (resolve-url :oppijanumerorekisteri-service.get-persons) %)
                     partitions)]
    (reduce
      (fn [acc result]
        (match result
               {:status 200 :body body}
               (merge acc (json/parse-string body))

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
  [data query-oid]
  (let [gs (seq (group-by :masterOid data))]
    (cond (empty? gs)
          {:master-oid  query-oid
           :linked-oids #{query-oid}}
          (empty? (rest gs))
          (let [[[master-oid links] & _] gs]
            {:master-oid  master-oid
             :linked-oids (->> links
                               (map :henkiloOid)
                               (cons master-oid)
                               set)})
          :else
          (throw (new RuntimeException
                      (str "Person oid " query-oid
                           " linked to multiple master oids"))))))

(defn linked-oids [cas-client oid]
  (let [result (cas/cas-authenticated-post
                cas-client
                (resolve-url :oppijanumerorekisteri-service.duplicate-henkilos)
                {:henkiloOids [oid]})]
    (match result
      {:status 200 :body body}
      (parse-duplicate-henkilos (json/parse-string body true) oid)
      :else (throw-error (str "Could not get linked oids for oid " oid ", "
                              "status: " (:status result) ", "
                              "response body: " (:body result))))))
