(ns ataru.log.audit-log
  (:require [ataru.util.app-utils :as app-utils]
            [clj-json-patch.core :as p]
            [clj-time.core :as c]
            [clj-time.format :as f]
            [cheshire.core :as json])
  (:import [fi.vm.sade.auditlog Audit ApplicationType CommonLogMessageFields AbstractLogMessage]))

(def ^:private app-id (app-utils/get-app-id))

(def ^:private service-name (case app-id
                              :virkailija "ataru-virkailija"
                              :hakija "ataru-hakija"))

(def ^:private application-type (case app-id
                                  :virkailija ApplicationType/VIRKAILIJA
                                  :hakija ApplicationType/OPISKELIJA))
(def ^:private logger (Audit. service-name application-type))

(def ^:private date-time-formatter (f/formatter :date-time))

(defn- timestamp []
  (->> (c/now)
       (f/unparse date-time-formatter)))

(defn- operation [new-object old-object]
  (cond
    (empty? old-object)
    "lisäys"

    (:deleted new-object)
    "poisto"

    :else "muutos"))

(defn log
  ([new-object id organization-oid]
   (log new-object nil id organization-oid))
  ([new-object old-object id organization-oid]
   {:pre [(some? new-object)
          (some? id)
          (some? organization-oid)]}
   (let [old-object (or old-object {})
         message    (json/generate-string (p/diff old-object new-object))
         log-map    {CommonLogMessageFields/ID        id
                     CommonLogMessageFields/TIMESTAMP (timestamp)
                     CommonLogMessageFields/OPERAATIO (operation new-object old-object)
                     CommonLogMessageFields/MESSAGE   message
                     "organizationOid"                organization-oid
                     "logSeq"                         (str (rand-int 1000000000))}]
     (->> (proxy [AbstractLogMessage] [log-map])
          (.log logger)))))
