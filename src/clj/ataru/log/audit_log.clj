(ns ataru.log.audit-log
  (:require [ataru.config.core :refer [config]]
            [clojure.data :refer [diff]]
            [clojure.set]
            [taoensso.timbre :as timbre]
            [environ.core :refer [env]]
            [taoensso.timbre.appenders.community.rolling :refer [rolling-appender]])
  (:import [fi.vm.sade.auditlog
            Operation
            Changes$Builder
            Target$Builder
            Logger
            ApplicationType
            User
            Audit
            DummyAuditLog]
           [org.ietf.jgss Oid]
           java.net.InetAddress
           java.util.TimeZone))

(defn- create-operation [op]
  (proxy [Operation] [] (name [] op)))

(def operation-failed (create-operation "epäonnistunut"))
(def operation-read (create-operation "luku"))
(def operation-new (create-operation "lisäys"))
(def operation-modify (create-operation "muutos"))
(def operation-delete (create-operation "poisto"))
(def operation-login (create-operation "kirjautuminen"))
(def operation-oppija-login (create-operation "oppija-kirjautuminen"))
(def operation-oppija-logout (create-operation "oppija-uloskirjautuminen"))

;; Huom: tätä funktiota tulee kutsua tuotantosovelluksessa vain kerran, jotta ei synny useampia Audit-instansseja.
(defn new-audit-logger [service-name]
  (let [base-path        (case service-name
                           "ataru-editori" (-> config :log :virkailija-base-path)
                           "ataru-hakija"  (-> config :log :hakija-base-path))
        application-type (case service-name
                           "ataru-editori" ApplicationType/VIRKAILIJA
                           "ataru-hakija"  ApplicationType/OPPIJA)
        audit-log-config (assoc timbre/default-config
                           :appenders {:standard-out     {:enabled? false}
                                       :file-appender   (-> (rolling-appender
                                                              {:path    (str base-path
                                                                             "/audit_" service-name
                                                                             ;; Hostname will differentiate files in actual environments
                                                                             (when (:hostname env) (str "_" (:hostname env))))
                                                               :pattern :daily})
                                                            (assoc :output-fn (fn [data] (force (:msg_ data)))))}
                           :timestamp-opts {:pattern  "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
                                            :timezone (TimeZone/getTimeZone "Europe/Helsinki")})
        logger           (proxy [Logger] [] (log [s]
                                              (timbre/log* audit-log-config :info s)))]
    (new Audit logger service-name application-type)))

(defn new-dummy-audit-logger []
  (new DummyAuditLog))

(defn- map-or-vec? [x]
  (or (map? x)
      (vector? x)))

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

(defn- do-log [^Audit audit-logger {:keys [new old id operation session oid]}]
  {:pre [(or (and (or (string? new)
                      (map-or-vec? new))
                  (nil? old))
             (and (or (string? old)
                      (map-or-vec? old))
                  (nil? new))
             (and (map-or-vec? old)
                  (map-or-vec? new)))
         (some #{operation} [operation-failed
                             operation-new
                             operation-read
                             operation-modify
                             operation-delete
                             operation-login
                             operation-oppija-login
                             operation-oppija-logout])]}
  (let [user      (User.
                   (when-let [logged-oid (or (-> session :identity :oid) oid)]
                     (Oid. logged-oid))
                   (if-let [ip (:client-ip session)]
                     (InetAddress/getByName ip)
                     (InetAddress/getLocalHost))
                   (or (:key session) "no session")
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
                        (.updated % (str path) (str o) (str n)))))]
    (.log audit-logger user operation
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
  ([audit-logger params]
    (try
      (do-log audit-logger params)
      (catch Throwable t
        (throw (new RuntimeException "Failed to create an audit log entry" t))))))
