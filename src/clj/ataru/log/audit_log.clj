(ns ataru.log.audit-log
  (:require [ataru.util.app-utils :as app-utils]
            [clj-time.core :as c]
            [clj-time.format :as f]
            [clojure.core.match :as m]
            [cheshire.core :as json]
            [taoensso.timbre :as log])
  (:import [fi.vm.sade.auditlog Audit ApplicationType CommonLogMessageFields AbstractLogMessage]
           [org.joda.time DateTime]
           [com.fasterxml.jackson.databind ObjectMapper]
           [com.github.fge.jsonpatch.diff JsonDiff]))

(def operation-new "lisäys")
(def operation-modify "muutos")
(def operation-delete "poisto")
(def operation-login "kirjautuminen")

(def ^:private object-mapper (ObjectMapper.))

(defn- service-name []
  (case (app-utils/get-app-id)
    :virkailija "ataru-virkailija"
    :hakija "ataru-hakija"))

(defn- application-type []
  (case (app-utils/get-app-id)
    :virkailija ApplicationType/VIRKAILIJA
    :hakija ApplicationType/OPISKELIJA))

(def ^:private logger (atom nil))

(def ^:private date-time-formatter (f/formatter :date-time))

(defn- timestamp []
  (->> (c/now)
       (f/unparse date-time-formatter)))

(defn- map-or-vec? [x]
  (or (map? x)
      (vector? x)))

(def ^:private log-seq (atom 0))

(def ^:private not-blank? (comp not clojure.string/blank?))

(defn- diff [old new]
  (let [old-str (json/generate-string old)
        new-str (json/generate-string new)
        diff-node (JsonDiff/asJsonPatch (.readTree object-mapper old-str)
                                        (.readTree object-mapper new-str))]
    (.writeValueAsString object-mapper diff-node)))

(defn- get-message [new old]
  (m/match [new old]
           [(_ :guard map-or-vec?) (_ :guard map-or-vec?)]
           (diff old new)

           [(_ :guard map-or-vec?) (_ :guard nil?)]
           (json/generate-string new)

           [(_ :guard string?) _]
           new))

(defn- date->str [x]
  (cond->> x
    (instance? DateTime x)
    (f/unparse date-time-formatter)))

(defn- transform-values [t coll]
  (clojure.walk/prewalk (fn [x]
                          (cond->> x
                            (map? x)
                            (into {} (map (fn [[k v]] [k (t v)])))))
                        coll))

(defn- do-log [{:keys [new old id operation organization-oid]}]
  {:pre [(or (and (or (string? new)
                      (map-or-vec? new))
                  (nil? old))
             (and (map-or-vec? old)
                  (map-or-vec? new)))
         (not-blank? id)
         (some #{operation} [operation-new operation-modify operation-delete operation-login])]}
  (let [message (get-message (transform-values date->str new)
                             (transform-values date->str old))
        log-map (cond-> {CommonLogMessageFields/ID        id
                         CommonLogMessageFields/TIMESTAMP (timestamp)
                         CommonLogMessageFields/OPERAATIO operation
                         CommonLogMessageFields/MESSAGE   message
                         "logSeq"                         (str (swap! log-seq inc))}
                  (not-blank? organization-oid)
                  (assoc "organizationOid" organization-oid))]
    (->> (proxy [AbstractLogMessage] [log-map])
         (.log @logger))))

(defn log
  "Create an audit log entry. Provide map with :new and optional :old
   values to log.

   When both values are provided, both of them must be of same type and
   either a vector or a map. An RFC6902 compliant patch is logged.

   If only :new value is provided, it can also be a String."
  [params]
  (try
    (do-log params)
    (catch Throwable t
      (log/error t "Failed to create an audit log entry"))))

(defn init-audit-logging! []
  (reset! logger (Audit. (service-name) (application-type))))
