(ns ataru.log.audit-log
  (:require [ataru.util.app-utils :as app-utils]
            [ataru.config.core :refer [config]]
            [clj-time.core :as c]
            [clj-time.format :as f]
            [clojure.core.match :as m]
            [cheshire.core :as json]
            [taoensso.timbre :as timbre]
            [environ.core :refer [env]]
            [clojure.data :refer [diff]]
            [taoensso.timbre.appenders.3rd-party.rolling :refer [rolling-appender]])
  (:import [fi.vm.sade.auditlog
            Operation
            Changes$Builder
            Target$Builder
            Target
            Changes
            Logger
            Audit
            ApplicationType
            User]
           [org.joda.time DateTime]
           [com.fasterxml.jackson.databind ObjectMapper]
           [com.github.fge.jsonpatch.diff JsonDiff]
           (org.slf4j LoggerFactory)
           (java.util Date)
           (java.net InetAddress)
           (org.ietf.jgss Oid)))

(defn- create-operation [op]
  (proxy [Operation] [] (name [] op)))

(def operation-failed (create-operation "epäonnistunut"))
(def operation-read (create-operation "luku"))
(def operation-new (create-operation "lisäys"))
(def operation-modify (create-operation "muutos"))
(def operation-delete (create-operation "poisto"))
(def operation-login (create-operation "kirjautuminen"))

(defn- create-audit-logger []
  (let [service-name     (case (app-utils/get-app-id)
                           :virkailija "ataru-editori"
                           :hakija     "ataru-hakija"
                           nil)
        audit-log-config (assoc timbre/example-config
                                :appenders {:file-appender
                                            (assoc (rolling-appender {:path    (str (-> config :log :virkailija-base-path)
                                                                                    "/auditlog_" service-name
                                                                                    ;; Hostname will differentiate files in actual environments
                                                                                    (when (:hostname env) (str "_" (:hostname env)))
                                                                                    ".log")
                                                                      :pattern :daily})
                                                   :output-fn (fn [{:keys [msg_]}] (force msg_)))})
        logger           (proxy [Logger] [] (log [str]
                                              (timbre/log* audit-log-config :info str)))
        application-type (case (app-utils/get-app-id)
                           :virkailija ApplicationType/VIRKAILIJA
                           :hakija     ApplicationType/OPPIJA
                           ApplicationType/BACKEND)]
    (new Audit logger service-name application-type)))

(def ^:private logger (create-audit-logger))

(def ^:private date-time-formatter (f/formatter :date-time))

(defn- timestamp []
  (->> (c/now)
       (f/unparse date-time-formatter)))

(defn- map-or-vec? [x]
  (or (map? x)
      (vector? x)))

(def ^:private not-blank? (comp not clojure.string/blank?))

(defn- path-> [p k]
  (str (when p (str p "."))
    (if (keyword? k)
      (name k)
      k)))

(defn unnest
  ([m]
   (unnest m nil))
  ([mm prefix]
   (reduce-kv (fn [m key val]
                  (cond
                    (nil? val)
                    (assoc m (path-> prefix key) "")

                    (map-or-vec? val)
                    (merge m (unnest val (path-> prefix key)))

                    :else
                    (assoc m (path-> prefix key) val))) {} mm)))

(defn ->changes [new old]
  (let [[new-diff old-diff _] (diff (unnest new) (unnest old))
        added-kw   (clojure.set/difference (set (keys new-diff)) (set (keys old-diff)))
        removed-kw (clojure.set/difference (set (keys old-diff)) (set (keys new-diff)))
        updated-kw (clojure.set/intersection (set (keys old-diff)) (set (keys new-diff)))]
    [(select-keys new-diff added-kw)
     (select-keys old-diff removed-kw)
     (into {} (for [kw updated-kw]
                [kw [(get new-diff kw) (get old-diff kw)]]))]))

(defn- do-log [{:keys [new old id operation session]}]
  {:pre [(or (and (or (string? new)
                      (map-or-vec? new))
                  (nil? old))
             (and (map-or-vec? old)
                  (map-or-vec? new)))
         (some #{operation} [operation-failed
                             operation-new
                             operation-read
                             operation-modify
                             operation-delete
                             operation-login])]}
  (let [user      (User.
                   (when-let [oid (-> :session :identity :oid)]
                     (Oid. oid))
                   (if-let [ip (:client-ip session)]
                     (InetAddress/getByName ip)
                     (InetAddress/getLocalHost))
                   (or (-> :session :identity :ticket) "no session")
                   (or (:user-agent session) "no user agent"))
        [added
         removed
         updated] (->changes new old)
        changes   (doto (Changes$Builder.)
                    (#(doseq [[path val] added]
                        (.added % path (str val))))
                    (#(doseq [[path val] removed]
                        (.removed % path (str val))))
                    (#(doseq [[path [n o]] updated]
                        (.updated % (str path) (str n) (str o)))))]
    (.log logger user operation
          (let [tb (Target$Builder.)]
            (doseq [[field value] id
                    :when (some? value)]
              (.setField tb (name field) value))
            (.build tb))
          (.build changes))))

(defn log
  "Create an audit log entry. Provide map with :new and optional :old
   values to log.

   When both values are provided, both of them must be of same type and
   either a vector or a map.

   If only :new value is provided, it can also be a String."
  [params]
  (try
    (do-log params)
    (catch Throwable t
      (throw (new RuntimeException "Failed to create an audit log entry" t)))))
