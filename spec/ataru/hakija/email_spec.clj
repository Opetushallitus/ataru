(ns ataru.hakija.email-spec
  (:require [aleph.http :as http]
            [ataru.hakija.email :as email]
            [cheshire.core :as json]
            [speclj.core :refer :all]))

(defmacro with-mock-api
  [eval-fn & body]
  `(let [api-called?# (atom false)]
     (with-redefs-fn {#'http/post (fn [& args#]
                                    (apply ~eval-fn args#)
                                    (reset! api-called?# true))}
       (fn []
         ~@body
         (should @api-called?#)))))

(describe "sending email"
  (tags :unit)

  (it "sends email using the /ryhmasahkoposti-service/email/firewall API call"
    (tags :unit)

    (with-mock-api (fn [uri request]
                     (should= uri "https://itest-virkailija.oph.ware.fi/ryhmasahkoposti-service/email/firewall")
                     (should= (get-in request [:headers "content-type"]) "application/json")
                     (let [body (json/parse-string (:body request) true)]
                       (should= (get-in body [:email :from]) "no-reply@opintopolku.fi")
                       (should= (get-in body [:email :subject]) "Hakemus vastaanotettu")))
      (email/send-email-verification {}))))
