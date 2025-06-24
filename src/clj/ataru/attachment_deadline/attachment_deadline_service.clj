(ns ataru.attachment-deadline.attachment-deadline-service
  (:require
   [clj-time.core :as t]
   [clj-time.coerce :as c]
   [clojure.string :as s]
   [ataru.config.core :refer [config]]
   [ataru.ohjausparametrit.ohjausparametrit-protocol :as ohjausparametrit]
   [ataru.log.audit-log :as audit-log]
   [ataru.applications.application-access-control :as aac]
   [ataru.db.db :as db]
   [yesql.core :as yesql]
   [com.stuartsierra.component :as component]
   [ataru.attachment-deadline.attachment-deadline-protocol :refer [AttachmentDeadlineServiceProtocol]]))

(declare yesql-get-field-deadlines)
(yesql/defqueries "sql/field-deadline-queries.sql")

(defn- get-field-deadlines
  "Returns all of the individually set field deadlines for an application."
  [application-key]
   (db/exec :db yesql-get-field-deadlines {:application_key application-key}))

(defn- get-field-deadlines-authorized
  [organization-service tarjonta-service audit-logger session application-key]
  (if (aac/applications-access-authorized?
       organization-service
       tarjonta-service
       session
       [application-key]
       [:view-applications :edit-applications])
    (let [r (vec (get-field-deadlines application-key))]
      (audit-log/log audit-logger
                     {:new       r
                      :id        {:applicationOid application-key}
                      :session   session
                      :operation audit-log/operation-read})
      r)
    :unauthorized))

(defn- ->boolean
  [value]
  (if (string? value)
    (parse-boolean value)
    value))

(defn uses-per-application-deadline?
  "Returns true if the application uses per-application deadline, false for per-admission deadline"
  [ohjausparametrit]
  (-> ohjausparametrit
      :liitteidenMuokkauksenHakemuskohtainenTakarajaKaytossa
      ->boolean
      true?))

(defn- get-attachment-deadline-days
  [ohjausparametrit]
  (let [default-grace-days (-> config
                               :public-config
                               (get :attachment-modify-grace-period-days 14))
        custom-grace-days  (if (uses-per-application-deadline? ohjausparametrit)
                             (-> ohjausparametrit :liitteidenMuokkauksenHakemuskohtainenTakarajaPaivaa)
                             (-> ohjausparametrit :PH_LMT :value))]
    (or custom-grace-days default-grace-days)))

(defn- get-attachment-deadline-time
  [ohjausparametrit]
  (let [default-time      (-> config
                              :public-config
                              (get :attachment-modify-grace-period-time "15:00"))]
    (if (uses-per-application-deadline? ohjausparametrit)
      (or (-> ohjausparametrit :liitteidenMuokkauksenHakemuskohtainenTakarajaKellonaika)
          default-time)
      (-> ohjausparametrit :liitteidenMuokkauksenHakukohtainenTakarajaKellonaika))))

; TODO: poista tämä häkki kun ihmiskunta on tullut järkiinsä ja tuhonnut kesäajan
(defn- winter-summertime-nullification-adjustment
  "Adjusts the end time of the deadline to take account of possible daylight saving time changes"
  [end start]
  (let [end-fin (.withZone end (t/time-zone-for-id "Europe/Helsinki"))
        start-fin (.withZone start (t/time-zone-for-id "Europe/Helsinki"))]
    (t/plus end
            (t/hours (cond
                       (= (t/hour start-fin) (t/hour end-fin)) 0
                       (> (t/day end) (t/day end-fin)) 1
                       (< (t/day end) (t/day end-fin)) -1
                       (and (= 23 (t/hour end-fin)) (= 0 (t/hour start-fin))) 1
                       (and (= 23 (t/hour start-fin)) (= 0 (t/hour end-fin))) -1
                       :else (- (t/hour start-fin) (t/hour end-fin)))))))

(defn- parse-local-time [value]
  (when (string? value)
    (->> (s/split value #":")
         (map #(int (parse-long %)))
         (apply (fn [h m]
                  (t/local-time h m))))))

(defn- set-local-time
  [datetime haku-settings-based-grace-period-time]
  (if haku-settings-based-grace-period-time
    (.withTime datetime (parse-local-time haku-settings-based-grace-period-time))
    datetime))

(defn- attachment-deadline-for-hakuaika [ohjausparametrit-service application-submitted haku hakuaika]
  (let [ohjausparametrit   (when (and haku ohjausparametrit-service)
                             (ohjausparametrit/get-parametri ohjausparametrit-service (:oid haku)))
        haku-settings-based-grace-period (get-attachment-deadline-days ohjausparametrit)
        haku-settings-based-grace-period-time (get-attachment-deadline-time ohjausparametrit)
        modify-grace-period (if (uses-per-application-deadline? ohjausparametrit)
                              haku-settings-based-grace-period
                              (or (:attachment-modify-grace-period-days hakuaika)
                                  haku-settings-based-grace-period))
        attachment-grace-period-start (some-> (if (uses-per-application-deadline? ohjausparametrit)
                                                application-submitted
                                                (some-> hakuaika
                                                        :end
                                                        c/from-long))
                                              (.withZone (t/time-zone-for-id "UTC")))
        attachment-grace-period-end (some-> attachment-grace-period-start
                                            (t/plus (t/days modify-grace-period)))]
    (some-> attachment-grace-period-end
            (winter-summertime-nullification-adjustment attachment-grace-period-start)
            (.withZone (t/time-zone-for-id "Europe/Helsinki"))
            (set-local-time haku-settings-based-grace-period-time))))

(defrecord AttachmentDeadlineService [ohjausparametrit-service]
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  AttachmentDeadlineServiceProtocol
  (get-field-deadlines-authorized [_ organization-service tarjonta-service audit-logger session application-key]
    (get-field-deadlines-authorized organization-service tarjonta-service audit-logger session application-key))
  (get-field-deadlines [_ application-key]
    (get-field-deadlines application-key))
  (attachment-deadline-for-hakuaika [_ application-submitted haku hakuaika]
    (attachment-deadline-for-hakuaika ohjausparametrit-service application-submitted haku hakuaika)))
