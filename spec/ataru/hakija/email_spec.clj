(ns ataru.hakija.email-spec
  (:require [aleph.http :as http]
            [manifold.deferred :as d]
            [ataru.hakija.email :as email]
            [cheshire.core :as json]
            [speclj.core :refer :all]))

(def application {:form 44,
                  :lang "fi",
                  :state :received,
                  :answers
                  [{:key "ssn",
                    :value "010101-123n",
                    :fieldType "textField",
                    :label {:fi "Henkilötunnus", :sv "Personnummer"}}
                   {:key "phone",
                    :value "050123123",
                    :fieldType "textField",
                    :label {:fi "Matkapuhelin", :sv "Mobiltelefonnummer"}}
                   {:key "preferred-name",
                    :value "Aku",
                    :fieldType "textField",
                    :label {:fi "Kutsumanimi", :sv "Smeknamn"}}
                   {:key "home-town",
                    :value "Ankkalinna",
                    :fieldType "textField",
                    :label {:fi "Kotikunta", :sv "Bostadsort"}}
                   {:key "last-name",
                    :value "Ankka",
                    :fieldType "textField",
                    :label {:fi "Sukunimi", :sv "Efternamn"}}
                   {:key "first-name",
                    :value "Aku Petteri",
                    :fieldType "textField",
                    :label {:fi "Etunimet", :sv "Förnamn"}}
                   {:key "address",
                    :value "Paratiisitie 13",
                    :fieldType "textField",
                    :label {:fi "Katuosoite", :sv "Adress"}}
                   {:key "nationality",
                    :value "fi",
                    :fieldType "dropdown",
                    :label {:fi "Kansalaisuus", :sv "Nationalitet"}}
                   {:key "postal-code",
                    :value "00013",
                    :fieldType "textField",
                    :label {:fi "Postinumero", :sv "Postnummer"}}
                   {:key "language",
                    :value "sv",
                    :fieldType "dropdown",
                    :label {:fi "Äidinkieli", :sv "Modersmål"}}
                   {:key "gender",
                    :value "female",
                    :fieldType "dropdown",
                    :label {:fi "Sukupuoli", :sv "Kön"}}
                   {:key "email",
                    :value "aku@ankkalinna.com",
                    :fieldType "textField",
                    :label {:fi "Sähköpostiosoite", :sv "E-postadress"}}]})

(defmacro with-mock-api
  [eval-fn & body]
  `(let [api-called?# (atom false)]
     (with-redefs-fn {#'http/post (fn [& args#]
                                    (apply ~eval-fn args#)
                                    (reset! api-called?# true)
                                    (d/success-deferred nil))}
       (fn []
         ~@body
         (should @api-called?#)))))

(describe "sending email"
  (tags :unit)

  (it "sends email using the /ryhmasahkoposti-service/email/firewall API call"
    (tags :unit)

    (with-mock-api (fn [uri request]
                     (should= "https://itest-virkailija.oph.ware.fi/ryhmasahkoposti-service/email/firewall" uri)
                     (should= "application/json" (get-in request [:headers "content-type"]))
                     (let [body (json/parse-string (:body request) true)]
                       (should= "no-reply@opintopolku.fi" (get-in body [:email :from]))
                       (should= "Opintopolku.fi - Hakemuksesi on vastaanotettu" (get-in body [:email :subject]))
                       (let [recipients (:recipient body)]
                         (should= 1 (count recipients))
                         (should= "aku@ankkalinna.com" (get-in recipients [0 :email])))))
      (email/send-email-verification application 1))))
