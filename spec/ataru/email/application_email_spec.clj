(ns ataru.email.application-email-spec
  (:require [ataru.email.application-email :as email]
            [ataru.email.email-fixtures :as fixtures]
            [speclj.core :refer [describe it should=]]
            [clojure.string :as str]))

(describe "application email"
  (it "creates email with hakutoiveet"
      (let [template-name-fn (constantly "templates/email_submit_confirmation_template_fi.html")
            application-attachment-reviews []
            get-attachment-type (fn [attachment-type] {:fi attachment-type
                                                       :sv attachment-type
                                                       :en attachment-type})
            [email] (email/create-emails email/edit-email-subjects
                                       template-name-fn
                                       fixtures/application
                                       fixtures/tarjonta-info
                                       fixtures/form
                                       application-attachment-reviews
                                       fixtures/email-template
                                       get-attachment-type
                                       false)
            body    (:body email)]
        (should= ["tiina@testaaja.fi"] (:recipients email))
        (should= (:fi email/edit-email-subjects) (:subject email))
        (should= true (str/includes? body "Hakutoiveesi ovat:"))
        (should= true (str/includes? body "Elintarvikealan perustutkinto - Stadin ammatti- ja aikuisopisto, Hattulantien toimipaikka"))
        (should= true (str/includes? body "Hammastekniikan perustutkinto - Stadin ammatti- ja aikuisopisto, Vilppulantien toimipaikka"))
        (should= true (str/includes? body "Hius- ja kauneudenhoitoalan perustutkinto - Stadin ammatti- ja aikuisopisto, Sturenkadun toimipaikka"))
        (should= "no-reply@opintopolku.fi" (:from email))))

  (it "creates email with attachment notifications"
      (let [template-name-fn (constantly "templates/email_submit_confirmation_template_fi.html")
            application-attachment-reviews []
            get-attachment-type (fn [attachment-type] {:fi attachment-type
                                                       :sv attachment-type
                                                       :en attachment-type})
            [email] (email/create-emails email/edit-email-subjects
                                         template-name-fn
                                         fixtures/application
                                         fixtures/tarjonta-info
                                         fixtures/form
                                         application-attachment-reviews
                                         fixtures/email-template
                                         get-attachment-type
                                         false)
            body    (:body email)]
        (should= true (str/includes? body "Lähetä liite osoitteeseen: Toimisto, Elintie 5, 00100 HELSINKI"))
        (should= true (str/includes? body "Palautettava viimeistään 28.2.2022 klo 00:00"))
        (should= true (str/includes? body "Lähetä liite osoitteeseen: Hiuskatu 2, 00500 HELSINKI"))
        (should= true (str/includes? body "Palautettava viimeistään 31.1.2022 klo 12:00"))
        (should= true (str/includes? body "Palautettava viimeistään 30.5.2022 klo 13:05"))
        (should= true (str/includes? body "Tai käytä: <a href='https://hampaat-liitteena.fi'>https://hampaat-liitteena.fi</a>"))
      )))