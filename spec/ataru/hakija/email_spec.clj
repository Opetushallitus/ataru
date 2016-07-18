(ns ataru.hakija.email-spec
  (:require [aleph.http :as http]
            [ataru.hakija.email :as email]
            [cheshire.core :as json]
            [speclj.core :refer :all]))

(def application {:form 44,
                  :lang "fi",
                  :state :received,
                  :answers
                  [{:key "b0dc66fc-b8fc-4c1d-bd8f-095f5890d3fc",
                    :value "010101-123n",
                    :fieldType "textField",
                    :label {:fi "Henkilötunnus", :sv "Personnummer"}}
                   {:key "f5de0137-d1e4-403f-bf66-e93e8032b3bc",
                    :value "050123123",
                    :fieldType "textField",
                    :label {:fi "Matkapuhelin", :sv "Mobiltelefonnummer"}}
                   {:key "d7698e7c-6b8f-4345-be8b-2ac9d3de277f",
                    :value "Aku",
                    :fieldType "textField",
                    :label {:fi "Kutsumanimi", :sv "Smeknamn"}}
                   {:key "6fa67b6a-7df4-4b70-8e43-9eaae171e6ab",
                    :value "Ankkalinna",
                    :fieldType "textField",
                    :label {:fi "Kotikunta", :sv "Bostadsort"}}
                   {:key "87dca441-f16c-424b-a521-b93314132bab",
                    :value "Ankka",
                    :fieldType "textField",
                    :label {:fi "Sukunimi", :sv "Efternamn"}}
                   {:key "9552180f-33c8-4bee-b782-da2a11ba6b9c",
                    :value "Aku Petteri",
                    :fieldType "textField",
                    :label {:fi "Etunimet", :sv "Förnamn"}}
                   {:key "7cda2f0e-930d-4274-8345-e6fee3de365a",
                    :value "Paratiisitie 13",
                    :fieldType "textField",
                    :label {:fi "Katuosoite", :sv "Adress"}}
                   {:key "a91b0915-a4b7-4b57-bc3d-1f3925a42367",
                    :value "fi",
                    :fieldType "dropdown",
                    :label {:fi "Kansalaisuus", :sv "Nationalitet"}}
                   {:key "09bd613f-285c-492a-82d7-e7a0c4d8b241",
                    :value "00013",
                    :fieldType "textField",
                    :label {:fi "Postinumero", :sv "Postnummer"}}
                   {:key "0e0b1636-ad29-45c1-8ba2-a558af45c0af",
                    :value "sv",
                    :fieldType "dropdown",
                    :label {:fi "Äidinkieli", :sv "Modersmål"}}
                   {:key "b5d9bd55-a3aa-494b-928e-7ca19dd8f502",
                    :value "female",
                    :fieldType "dropdown",
                    :label {:fi "Sukupuoli", :sv "Kön"}}
                   {:key "e7feb75b-1275-4049-a740-287ae7d5aa91",
                    :value "aku@ankkalinna.com",
                    :fieldType "textField",
                    :label {:fi "Sähköpostiosoite", :sv "E-postadress"}}]})

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
                     (should= "https://itest-virkailija.oph.ware.fi/ryhmasahkoposti-service/email/firewall" uri)
                     (should= "application/json" (get-in request [:headers "content-type"]))
                     (let [body (json/parse-string (:body request) true)]
                       (should= "no-reply@opintopolku.fi" (get-in body [:email :from]))
                       (should= "Hakemus vastaanotettu" (get-in body [:email :subject]))
                       (let [recipients (:recipient body)]
                         (should= 1 (count recipients))
                         (should= "aku@ankkalinna.com" (get-in recipients [0 :email])))))
      (email/send-email-verification application))))
