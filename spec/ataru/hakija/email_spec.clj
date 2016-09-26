(ns ataru.hakija.email-spec
  (:require [org.httpkit.client :as http]
            [ataru.hakija.email :as email]
            [ataru.hakija.email-store :as store]
            [cheshire.core :as json]
            [speclj.core :refer :all]))

(def undelivered-email
  {:id                1
   :application_id    1
   :recipient         "applicant@example.com"
   :created_at        nil
   :delivery_attempts 0
   :delivered_at      nil})

(def delivered-email
  {:id                2
   :application_id    1
   :recipient         "other.applicant@example.com"
   :created_at        nil
   :delivery_attempts 0
   :delivered_at      "2016-08-10T12:00:00.000+03:00"})

(defmacro with-mock-api
  [eval-fn & body]
  `(let [api-called?# (atom false)]
     (with-redefs-fn {#'http/post (fn [& args#]
                                    (apply ~eval-fn args#)
                                    (reset! api-called?# true)
                                    (future {:status 200}))}
       (fn []
         ~@body
         (should @api-called?#)))))

(describe
  "sending application confirmation emails"
  (tags :unit)

  (it "works for failed and successful viestintäpalvelu API calls"
      (let [sent-emails (atom 0)
            failed-emails (atom 0)]
        (with-redefs [store/get-unsent-emails (constantly [undelivered-email delivered-email])
                      store/increment-delivery-attempt-count (fn [_ _] (swap! failed-emails inc))
                      store/increment-delivery-attempt-count-and-mark-delivered (fn [_ _] (swap! sent-emails inc))]
          (store/deliver-emails (fn [_] {:status 200}))
          (should= @sent-emails 1)
          (should= @failed-emails 0)
          (store/deliver-emails (fn [_] {:status 500 :error "fail!"}))
          (should= @sent-emails 1)
          (should= @failed-emails 1))))


  (it "sends email using the /ryhmasahkoposti-service/email/firewall API call"
      (with-mock-api (fn [uri request]
                       (should= "https://itest-virkailija.oph.ware.fi/ryhmasahkoposti-service/email/firewall" uri)
                       (should= "application/json" (get-in request [:headers "content-type"]))
                       (let [body (json/parse-string (:body request) true)]
                         (should= "no-reply@opintopolku.fi" (get-in body [:email :from]))
                         (let [recipients (:recipient body)]
                           (should= 1 (count recipients))
                           (should= "applicant@example.com" (get-in recipients [0 :email])))))
                     (#'email/send-email-verification undelivered-email))))

