(ns ataru.log.audit-log
  (:require [ataru.util.app-utils :as app-utils]
            [clj-json-patch.core :as p]
            [clj-time.core :as c]
            [clj-time.format :as f]
            [cheshire.core :as json])
  (:import [fi.vm.sade.auditlog Audit ApplicationType CommonLogMessageFields AbstractLogMessage]))

(def operation-new "lisäys")
(def operation-modify "muutos")
(def operation-delete "poisto")

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

(defn log
  ([new-object params]
   (log new-object nil params))
  ([new-object old-object {:keys [id operation organization-oid]}]
   {:pre [(or (and (string? new-object)
                   (nil? old-object))
              (and (map? new-object)
                   (or (nil? old-object)
                       (map? old-object))))
          (not (clojure.string/blank? id))
          (not (clojure.string/blank? operation))]}
   (let [old-object (or old-object {})
         message    (json/generate-string (p/diff old-object new-object))
         log-map    (cond-> {CommonLogMessageFields/ID        id
                             CommonLogMessageFields/TIMESTAMP (timestamp)
                             CommonLogMessageFields/OPERAATIO operation
                             CommonLogMessageFields/MESSAGE   message
                             "logSeq"                         (str (rand-int 1000000000))}
                      (some? organization-oid)
                      (assoc "organizationOid" organization-oid))
         logger     (get-logger)]
     (->> (proxy [AbstractLogMessage] [log-map])
          (.log logger)))))
