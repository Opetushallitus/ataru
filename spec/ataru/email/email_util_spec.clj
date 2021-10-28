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
        (should= ["huoltaja.testaa@testi.fi"] (:recipients (last emails)))))))