(ns ataru.log.audit-log
  (:require [ataru.util.app-utils :as app-utils]
            [clj-json-patch.core :as p]
            [clj-time.core :as c]
            [clj-time.format :as f]
            [clojure.core.match :as m]
            [cheshire.core :as json])
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

(defn log
  ([new-object params]
   (log new-object nil params))
  ([new-object old-object {:keys [id operation organization-oid]}]
   {:pre [(or (and (or (string? new-object)
                       (map-or-vec? new-object))
                   (nil? old-object))
              (some? (p/diff new-object old-object)))
          (not (clojure.string/blank? id))
          (not (clojure.string/blank? operation))]}
   (let [message (m/match [new-object old-object]
                          [(_ :guard map-or-vec?) (_ :guard map-or-vec?)]
                          (json/generate-string (p/diff old-object new-object))

                          [(_ :guard map-or-vec?) (_ :guard nil?)]
                          (json/generate-string new-object)

                          [(_ :guard string?) _]
                          new-object)
         log-map (cond-> {CommonLogMessageFields/ID        id
                          CommonLogMessageFields/TIMESTAMP (timestamp)
                          CommonLogMessageFields/OPERAATIO operation
                          CommonLogMessageFields/MESSAGE   message
                          "logSeq"                         (str (rand-int 1000000000))}
                   (some? organization-oid)
                   (assoc "organizationOid" organization-oid))
         logger  (get-logger)]
     (->> (proxy [AbstractLogMessage] [log-map])
          (.log logger)))))
