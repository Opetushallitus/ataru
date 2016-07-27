(ns ataru.hakija.hakija-routes-spec
  (:require [ataru.applications.application-store :as store]
            [ataru.fixtures.db.unit-test-db :as db]
            [ataru.hakija.email :as email]
            [ataru.hakija.hakija-routes :as routes]
            [cheshire.core :as json]
            [oph.soresu.common.db :as soresu-db]
            [ring.mock.request :as mock]
            [speclj.core :refer :all]
            [yesql.core :as sql]))

(sql/defqueries "sql/application-queries.sql")

(def person-info-form-application {:form 15, :lang "fi", :answers [{:key "address", :value "Paratiisitie 13", :fieldType "textField", :label {:fi "Katuosoite", :sv "Adress"}} {:key "email", :value "aku@ankkalinna.com", :fieldType "textField", :label {:fi "Sähköpostiosoite", :sv "E-postadress"}} {:key "preferred-name", :value "Aku", :fieldType "textField", :label {:fi "Kutsumanimi", :sv "Smeknamn"}} {:key "last-name", :value "Ankka", :fieldType "textField", :label {:fi "Sukunimi", :sv "Efternamn"}} {:key "phone", :value "050123", :fieldType "textField", :label {:fi "Matkapuhelin", :sv "Mobiltelefonnummer"}} {:key "nationality", :value "Suomi", :fieldType "dropdown", :label {:fi "Kansalaisuus", :sv "Nationalitet"}} {:key "ssn", :value "010101-123N", :fieldType "textField", :label {:fi "Henkilötunnus", :sv "Personnummer"}} {:key "municipality", :value "Ankkalinna", :fieldType "textField", :label {:fi "Kotikunta", :sv "Bostadsort"}} {:key "first-name", :value "Aku Petteri", :fieldType "textField", :label {:fi "Etunimet", :sv "Förnamn"}} {:key "postal-code", :value "00013", :fieldType "textField", :label {:fi "Postinumero", :sv "Postnummer"}} {:key "language", :value "suomi", :fieldType "dropdown", :label {:fi "Äidinkieli", :sv "Modersmål"}} {:key "gender", :value "Mies", :fieldType "dropdown", :label {:fi "Sukupuoli", :sv "Kön"}}]})
(def handler (-> (routes/new-handler) .start :routes))

(defmacro with-response
  [resp application & body]
  `(let [~resp (-> (mock/request :post "/hakemus/api/application" (json/generate-string ~application))
                   (mock/content-type "application/json")
                   handler
                   (update :body (comp #(json/parse-string % true) slurp)))]
     ~@body))

(defn- have-application-in-db
  [application-id]
  (when-let [actual (first (soresu-db/exec :db yesql-get-application-by-id {:application_id application-id}))]
    (= (:form person-info-form-application) (:form actual))))

(describe "POST /application"
  (tags :hakija)

  (around [spec]
    (with-redefs [email/send-email-verification (fn [_])]
      (spec)))

  (before-all (db/init-db-fixture))

  (after-all (db/clear-database))

  (it "should validate application"
    (with-response resp person-info-form-application
      (should= 200 (:status resp))
      (should (have-application-in-db (get-in resp [:body :id]))))))
