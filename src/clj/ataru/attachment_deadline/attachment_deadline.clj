(ns ataru.attachment-deadline.attachment-deadline
  (:require
    [clj-time.core :as t]
    [clj-time.coerce :as c]
    [clj-time.format :as f]
    [clojure.math :refer [signum]]
    [ataru.config.core :refer [config]]
    [ataru.applications.field-deadline :as field-deadline]))

; TODO
; (defn uses-per-application-deadline?
;  "Returns true if the application uses per-application deadline, false for per-admission deadline"
;  [_]
;  false)

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

; TODO: this needs to get application create / modify time as input
;       because in case of application-specific deadline, it's calculated based on that
(defn attachment-deadline-for-hakuaika [hakuaika]
  (let [default-modify-grace-period (-> config
                                        :public-config
                                        (get :attachment-modify-grace-period-days 14))
        modify-grace-period (or (:attachment-modify-grace-period-days hakuaika) default-modify-grace-period)
        hakuaika-end (some-> hakuaika
                             :end
                             c/from-long)
        attachment-end (some-> hakuaika-end
                               (t/plus (t/days modify-grace-period)))]
    (when attachment-end
      (t/plus attachment-end (t/hours (winter-summertime-nullification-adjustment hakuaika-end attachment-end))))))

(defn get-field-deadlines
  ([application-key]
   (field-deadline/get-field-deadlines application-key))
  ([organization-service tarjonta-service audit-logger session application-key]
   (field-deadline/get-field-deadlines organization-service tarjonta-service audit-logger session application-key)))
