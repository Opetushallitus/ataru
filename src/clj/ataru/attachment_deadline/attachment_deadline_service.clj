(ns ataru.attachment-deadline.attachment-deadline-service
  (:require
   [clj-time.core :as t]
   [clj-time.coerce :as c]
   [clj-time.format :as f]
   [clojure.string :as s]
   [clojure.math :refer [signum]]
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

; TODO: this needs to get application create / modify time as input
;       because in case of application-specific deadline, it's calculated based on that
(defn- get-attachment-deadline-days
  [ohjausparametrit-service haku]
  (when haku
    (let [default-grace-days (-> config
                                 :public-config
                                 (get :attachment-modify-grace-period-days 14))
          ohjausparametrit   (when (and haku ohjausparametrit-service)
                               (ohjausparametrit/get-parametri ohjausparametrit-service (:oid haku)))
          custom-grace-days  (if (uses-per-application-deadline? ohjausparametrit)
                               (-> ohjausparametrit :liitteidenMuokkauksenHakemuskohtainenTakarajaPaivaa)
                               (-> ohjausparametrit :PH_LMT :value))]
      (or custom-grace-days default-grace-days))))

(defn- get-attachment-deadline-time
  [ohjausparametrit-service haku]
  (when haku
    (let [default-time      (-> config
                                :public-config
                                (get :attachment-modify-grace-period-time "15:00"))
          ohjausparametrit  (when (and haku ohjausparametrit-service)
                              (ohjausparametrit/get-parametri ohjausparametrit-service (:oid haku)))
          custom-grace-time (if (uses-per-application-deadline? ohjausparametrit)
                              (-> ohjausparametrit :liitteidenMuokkauksenHakemuskohtainenTakarajaKellonaika)
                              (-> ohjausparametrit :liitteidenMuokkauksenHakukohtainenTakarajaKellonaika))]
      (or custom-grace-time default-time))))

(defn- new-formatter [fmt-str]
  (f/formatter fmt-str (t/time-zone-for-id "Europe/Helsinki")))

; TODO: poista tämä häkki kun ihmiskunta on tullut järkiinsä ja tuhonnut kesäajan
(defn- winter-summertime-nullification-adjustment
  "Adjusts the end time of the deadline to take account of possible daylight saving time changes"
  [start end]
  (let [start-hour (->> start
                        (f/unparse (new-formatter "HH"))
                        Integer/parseInt)
        end-hour (->> end
                      (f/unparse (new-formatter "HH"))
                      Integer/parseInt)
        day       (t/day end)
        formatted-day (->> end
                           (f/unparse (new-formatter "d"))
                           Integer/parseInt)
        modifier (signum (- start-hour end-hour))]
    (cond
      (== start-hour end-hour)
      0
      (> day formatted-day)
      (* -1 modifier)
      (< day formatted-day)
      (* 1 modifier)
      :else
      modifier)))

(defn- ->local-time [value]
  (when (string? value)
    (->> (s/split value #":")
         (map #(int (parse-long %)))
         (apply (fn [h m]
                  (t/local-time h m))))))

(defn- attachment-deadline-for-hakuaika [ohjausparametrit-service application-submitted haku hakuaika]
  (let [default-modify-grace-period (-> config
                                        :public-config
                                        (get :attachment-modify-grace-period-days 14))
        haku-settings-based-grace-period (get-attachment-deadline-days ohjausparametrit-service haku)
        haku-settings-based-grace-period-time (get-attachment-deadline-time ohjausparametrit-service haku)
        ohjausparametrit   (when (and haku ohjausparametrit-service)
                             (ohjausparametrit/get-parametri ohjausparametrit-service (:oid haku)))
        modify-grace-period (or (:attachment-modify-grace-period-days hakuaika)
                                haku-settings-based-grace-period
                                default-modify-grace-period)
        attachment-grace-period-start (if (uses-per-application-deadline? ohjausparametrit)
                                        application-submitted
                                        (some-> hakuaika
                                                :end
                                                c/from-long))
        attachment-grace-period-end (some-> attachment-grace-period-start
                                            (t/plus (t/days modify-grace-period)))]
    (when attachment-grace-period-end
      (if haku-settings-based-grace-period-time
        (.withTime (.withZone attachment-grace-period-end (t/time-zone-for-id "Europe/Helsinki"))
                   (->local-time haku-settings-based-grace-period-time))
        (t/plus attachment-grace-period-end
                (t/hours (winter-summertime-nullification-adjustment attachment-grace-period-start attachment-grace-period-end)))))))

(defrecord AttachmentDeadlineService [ohjausparametrit-service]
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  AttachmentDeadlineServiceProtocol
  (get-field-deadlines-authorized [_ organization-service tarjonta-service audit-logger session application-key]
    (get-field-deadlines-authorized organization-service tarjonta-service audit-logger session application-key))
  (get-field-deadlines [_ application-key]
    (get-field-deadlines application-key))
  (get-attachment-deadline-days [_ haku]
    (get-attachment-deadline-days ohjausparametrit-service haku))
  (get-attachment-deadline-time [_ haku]
    (get-attachment-deadline-time ohjausparametrit-service haku))
  (attachment-deadline-for-hakuaika [_ application-submitted haku hakuaika]
    (attachment-deadline-for-hakuaika ohjausparametrit-service application-submitted haku hakuaika)))
