(ns clj-access-logging
  (:require [clojure.string :as s])
  (:import [java.time
            ZoneId
            ZonedDateTime]
           java.time.format.DateTimeFormatter
           java.time.temporal.ChronoUnit))

(defn- extract-header
  [request header]
  (get-in request [:headers header] "-"))

(defn- extract-content-length
  [response]
  (Integer/valueOf (get-in response [:headers "Content-Length"] "-1")))

(defn session-access-logging-response
  [request response]
  (if-let [session (:session/key request)]
    (assoc-in response [:access-log-data :session] session)
    response))

(defn access-logging-response
  [request response start end]
  (try
    (update response :access-log-data
            #(merge
              {:timestamp       (.format end DateTimeFormatter/ISO_OFFSET_DATE_TIME)
               :responseCode    (:status response)
               :request         (str (:uri request)
                                     (when (:query-string request)
                                       (str "?" (:query-string request))))
               :responseTime    (.between ChronoUnit/MILLIS start end)
               :requestMethod   (s/upper-case (name (:request-method request)))
               :user-agent      (extract-header request "user-agent")
               :caller-id       (extract-header request "caller-id")
               :x-forwarded-for (extract-header request "x-forwarded-for")
               :x-real-ip       (extract-header request "x-real-ip")
               :remoteIp        (:remote-addr request)
               :session         "-"
               :responseSize    (extract-content-length response)
               :referer         (extract-header request "referer")}
              %))
    (catch Exception e
      (throw (new RuntimeException "Failed to create access log data" e)))))

(defn wrap-session-access-logging
  [handler]
  (fn
    ([request]
     (session-access-logging-response request (handler request)))
    ([request respond raise]
     (handler request
              #(respond (session-access-logging-response request %))
              raise))))

(defn wrap-access-logging
  [handler]
  (fn
    ([request]
     (let [start    (ZonedDateTime/now (ZoneId/of "Europe/Helsinki"))
           response (handler request)
           end      (ZonedDateTime/now (ZoneId/of "Europe/Helsinki"))]
       (access-logging-response request response start end)))
    ([request respond raise]
     (let [start (ZonedDateTime/now (ZoneId/of "Europe/Helsinki"))]
       (handler request
                #(let [end (ZonedDateTime/now (ZoneId/of "Europe/Helsinki"))]
                   (try
                     (respond (access-logging-response request % start end))
                     (catch Exception e
                       (raise e))))
                raise)))))
