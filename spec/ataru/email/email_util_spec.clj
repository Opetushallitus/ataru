(ns ataru.email.email-util-spec
  (:require [ataru.email.email-util :as util]
            [speclj.core :refer [describe it should= with-stubs stub should-have-invoked]]))

(describe "email util"
  (describe "make email data"
    (it "returns email data as a map"
      (let [data (util/make-email-data ["teppo.testaa@testi.fi"] "Otsikko" {:test-param "foobar"})]
        (should= ["teppo.testaa@testi.fi"] (:recipients data))
        (should= "Otsikko" (:subject data))
        (should= {:test-param "foobar"} (:template-params data))))
    (it "sets default from field"
      (let [data (util/make-email-data ["teppo.testaa@testi.fi"] "Otsikko" {:test-param "foobar"})]
        (should= "no-reply@opintopolku.fi" (:from data)))))

  (describe "render emails for applicant and guardian"
    (with-stubs)
    (it "renders email for applicant"
      (let [template-params {:application-url "https://linkki.fi" :content-ending "Viestin loppu"}
            stub-render (stub :render)
            applicant-email-data (util/make-email-data ["teppo.testaa@testi.fi"] "Vahvistusviesti" template-params)
            guardian-email-data (util/make-email-data [] "Vahvistusviesti" template-params)
            emails (util/render-emails-for-applicant-and-guardian
                     applicant-email-data
                     guardian-email-data
                     stub-render)]
        (should-have-invoked :render {:with [template-params]})
        (should= 1 (count emails))
        (should= ["teppo.testaa@testi.fi"] (:recipients (first emails)))))

    (it "renders email for applicant and guardian"
      (let [template-params {:application-url "https://linkki.fi" :content-ending "Viestin loppu"}
            stub-render (stub :render)
            applicant-email-data (util/make-email-data ["teppo.testaa@testi.fi" "tiina.testaa@testi.fi"] "Vahvistusviesti" template-params)
            guardian-email-data (util/make-email-data ["huoltaja.testaa@testi.fi"] "Vahvistusviesti" template-params)
            emails (util/render-emails-for-applicant-and-guardian
                     applicant-email-data
                     guardian-email-data
                     stub-render)]
        (should-have-invoked :render {:with [template-params]})
        (should-have-invoked :render {:with [{}]})
        (should= 2 (count emails))
        (should= ["teppo.testaa@testi.fi" "tiina.testaa@testi.fi"] (:recipients (first emails)))
        (should= ["huoltaja.testaa@testi.fi"] (:recipients (last emails))))))

    (describe "subject length limiting"
      (it "renders subject with full application key and full message when total is less than 255 chars"
        (let [prefix "Vahvistusviesti: sinut on valittu haluamaasi opiskelupaikkaan, lue ohjeet paikan vastaanottoon"
              application-key "1.2.246.562.11.00000000000000012345"
              lang :fi
              subject (util/enrich-subject-with-application-key-and-limit-length prefix application-key lang)]
          (should= "Vahvistusviesti: sinut on valittu haluamaasi opiskelupaikkaan, lue ohjeet paikan vastaanottoon (Hakemusnumero: 1.2.246.562.11.00000000000000012345)" subject)))
      (it "renders subject with full application key and full message when total is exactly 255 chars"
        (let [prefix "Vahvistusviesti: onneksi olkoon - sinut on valittu haluamaasi opiskelupaikkaan, ohjeet paikan vastaanottoon sisältyvät viestiin - avaa viesti saadaksesi lisätietoja vastaanotosta määräaikaan mennessä..."
              application-key "1.2.246.562.11.00000000000000012345"
              lang :fi
              subject (util/enrich-subject-with-application-key-and-limit-length prefix application-key lang)]
          (should= "Vahvistusviesti: onneksi olkoon - sinut on valittu haluamaasi opiskelupaikkaan, ohjeet paikan vastaanottoon sisältyvät viestiin - avaa viesti saadaksesi lisätietoja vastaanotosta määräaikaan mennessä... (Hakemusnumero: 1.2.246.562.11.00000000000000012345)" subject)
          (should= 255 (count subject))))
      (it "renders subject with full application key and truncated message when total length more than 255 chars"
        (let [prefix "Vahvistusviesti sinulle: onneksi olkoon - sinut on valittu haluamaasi opiskelupaikkaan, ohjeet paikan vastaanottoon sisältyvät viestiin - avaa viesti saadaksesi lisätietoja vastaanotosta määräaikaan mennessä... Tärkeää tietoa!"
              application-key "1.2.246.562.11.00000000000000012345"
              lang :fi
              subject (util/enrich-subject-with-application-key-and-limit-length prefix application-key lang)]
          (should= "Vahvistusviesti sinulle: onneksi olkoon - sinut on valittu haluamaasi opiskelupaikkaan, ohjeet paikan vastaanottoon sisältyvät viestiin - avaa viesti saadaksesi lisätietoja vastaanotosta määräaikaan mennessä... (Hakemusnumero: 1.2.246.562.11.00000000000000012345)" subject)
          (should= 255 (count subject))))
      (it "renders subject with full application key and no message"
        (let [prefix ""
              application-key "1.2.246.562.11.00000000000000012345"
              lang :fi
              subject (util/enrich-subject-with-application-key-and-limit-length prefix application-key lang)]
          (should= " (Hakemusnumero: 1.2.246.562.11.00000000000000012345)" subject)))))