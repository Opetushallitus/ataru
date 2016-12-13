(ns ataru.log.audit-log
  (:require [ataru.util.app-utils :as app-utils]
            [clj-json-patch.core :as p]
            [clj-time.core :as c]
            [clj-time.format :as f]
            [clojure.core.match :as m]
            [cheshire.core :as json]
            [taoensso.timbre :as log])
  (:import [fi.vm.sade.auditlog Audit ApplicationType CommonLogMessageFields AbstractLogMessage]))

(def operation-new "lisäys")
(def operation-modify "muutos")
(def operation-delete "poisto")
(def operation-login "kirjautuminen")

(defn- service-name []
  (case (app-utils/get-app-id)
    :virkailija "ataru-virkailija"
    :hakija "ataru-hakija"))

(defn- application-type []
  (case (app-utils/get-app-id)
    :virkailija ApplicationType/VIRKAILIJA
    :hakija ApplicationType/OPISKELIJA))

(def ^:private logger (atom nil))

(defn- get-logger []
  (or @logger (reset! logger (Audit. (service-name) (application-type)))))

(def ^:private date-time-formatter (f/formatter :date-time))

(defn- timestamp []
  (->> (c/now)
       (f/unparse date-time-formatter)))

(defn- map-or-vec? [x]
  (or (map? x)
      (vector? x)))

(def ^:private log-seq (atom 0))

(def ^:private not-blank? (comp not clojure.string/blank?))

(defn- do-log [{:keys [new old id operation organization-oid]}]
  {:pre [(or (and (or (string? new)
                      (map-or-vec? new))
                  (nil? old))
             (some? (p/diff new old)))
         (not-blank? id)
         (some #{operation} [operation-new operation-modify operation-delete operation-login])]}
  (let [message (m/match [new old]
                         [(_ :guard map-or-vec?) (_ :guard map-or-vec?)]
                         (json/generate-string (p/diff old new))

                         [(_ :guard map-or-vec?) (_ :guard nil?)]
                         (json/generate-string new)

                         [(_ :guard string?) _]
                         new)
        log-map (cond-> {CommonLogMessageFields/ID        id
                         CommonLogMessageFields/TIMESTAMP (timestamp)
                         CommonLogMessageFields/OPERAATIO operation
                         CommonLogMessageFields/MESSAGE   message
                         "logSeq"                         (str (swap! log-seq inc))}
                  (not-blank? organization-oid)
                  (assoc "organizationOid" organization-oid))
        logger  (get-logger)]
    (->> (proxy [AbstractLogMessage] [log-map])
         (.log logger))))

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
