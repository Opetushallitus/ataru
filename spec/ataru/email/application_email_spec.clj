(ns ataru.email.application-email-spec
  (:require [ataru.email.application-email :as email]
            [ataru.email.email-fixtures :as fixtures]
            [ataru.attachment-deadline.attachment-deadline-service :as attachment-deadline-service]
            [speclj.core :refer [describe it should=]]
            [ataru.ohjausparametrit.mock-ohjausparametrit-service :refer [->MockOhjausparametritService]]
            [clojure.string :as str]))

(def test-attachment-deadline-service (attachment-deadline-service/->AttachmentDeadlineService ->MockOhjausparametritService))

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
                                         false
                                         nil
                                         test-attachment-deadline-service)
            body    (:body email)]
        (should= ["tiina@testaaja.fi"] (:recipients email))
        (should= (str (:fi email/edit-email-subjects) " (Hakemusnumero: " (:key fixtures/application) ")") (:subject email))
        (should= true (str/includes? body "Hakutoiveesi ovat:"))
        (should= true (str/includes? body "Elintarvikealan perustutkinto - Stadin ammatti- ja aikuisopisto, Hattulantien toimipaikka"))
        (should= true (str/includes? body "Hammastekniikan perustutkinto - Stadin ammatti- ja aikuisopisto, Vilppulantien toimipaikka"))
        (should= true (str/includes? body "Hius- ja kauneudenhoitoalan perustutkinto - Stadin ammatti- ja aikuisopisto, Sturenkadun toimipaikka"))
        (should= "no-reply@opintopolku.fi" (:from email))))

  (it "creates email with per-hakukohde attachment notifications"
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
                                         false
                                         nil
                                         test-attachment-deadline-service)
            body    (:body email)]
        (should= (str (:fi email/edit-email-subjects) " (Hakemusnumero: " (:key fixtures/application) ")") (:subject email))
        (should= true (str/includes? body "Lähetä liite osoitteeseen: Toimisto, Elintie 5, 00100 HELSINKI"))
        (should= true (str/includes? body "Palautettava viimeistään 28.2.2022 klo 00:00"))
        (should= true (str/includes? body "Lähetä liite osoitteeseen: Hiuskatu 2, 00500 HELSINKI"))
        (should= true (str/includes? body "Palautettava viimeistään 31.1.2022 klo 12:00"))
        (should= true (str/includes? body "Palautettava viimeistään 30.5.2022 klo 13:05"))
        (should= true (str/includes? body "Tai käytä: <a href=\"https://elintie-liite.fi\" target=\"_blank\" style=\"color: #0093C4;\" rel=\"noopener noreferrer\">https://elintie-liite.fi</a>"))
        ))

  (it "creates email with payment-url"
      (let [template-name-fn (constantly "templates/email_submit_confirmation_template_fi.html")
            application-attachment-reviews []
            payment-url "https://localhost/maksut?secret=foobar"
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
                                         false
                                         payment-url
                                         test-attachment-deadline-service)
            body    (:body email)]
        (should= true (str/includes? body (str "href=\"" payment-url "\"")))
      ))

  (it "creates email with regular attachment notifications"
      (let [template-name-fn (constantly "templates/email_submit_confirmation_template_fi.html")
            application-attachment-reviews [{:attachment-key "9b00783c-5f4e-4ef9-bca4-c2e57b443d3c"}
                                            {:attachment-key "98655824-bb9d-4f4a-a1e5-4e39bd0f61f0"}]
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
                                         false
                                         nil
                                         test-attachment-deadline-service)
            body    (:body email)]
          (should= (str (:fi email/edit-email-subjects) " (Hakemusnumero: " (:key fixtures/application) ")") (:subject email))
          (should= true (str/includes? body "Upload liite"))
          (should= true (str/includes? body "Perinteinen liitepyyntö"))))

  (it "does not ask for attachment notification if attachment key is provided"
    (let [template-name-fn (constantly "templates/email_submit_confirmation_template_fi.html")
          application-attachment-reviews [{:attachment-key "9b00783c-5f4e-4ef9-bca4-c2e57b443d3c"}
                                          {:attachment-key "98655824-bb9d-4f4a-a1e5-4e39bd0f61f0"}]
          get-attachment-type (fn [attachment-type] {:fi attachment-type
                                                     :sv attachment-type
                                                     :en attachment-type})
          application (assoc-in fixtures/application [:answers 31 :value] "liite upattu")
          [email] (email/create-emails email/edit-email-subjects
                                       template-name-fn
                                       application
                                       fixtures/tarjonta-info
                                       fixtures/form
                                       application-attachment-reviews
                                       fixtures/email-template
                                       get-attachment-type
                                       false
                                       nil
                                       test-attachment-deadline-service)
          body    (:body email)]
      (should= (str (:fi email/edit-email-subjects) " (Hakemusnumero: " (:key fixtures/application) ")") (:subject email))
      (should= false (str/includes? body "Upload liite"))
      (should= true (str/includes? body "Perinteinen liitepyyntö"))))

  (it "creates email without transforming link text using markdown"
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
                                         false
                                         nil
                                         test-attachment-deadline-service)
            body    (:body email)]
        (should= (str (:fi email/edit-email-subjects) " (Hakemusnumero: " (:key fixtures/application) ")") (:subject email))
        (should= true (str/includes? body "Tai käytä: <a href=\"https://kauniit_puhtaat_hampaat-liitteena.fi\" target=\"_blank\" style=\"color: #0093C4;\" rel=\"noopener noreferrer\">https://kauniit_puhtaat_hampaat-liitteena.fi</a>")))))
