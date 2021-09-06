(ns ataru.email.application-email-confirmation-spec
  (:require [ataru.email.application-email-confirmation :as email-confirmation]
            [speclj.core :refer [describe it should= with-stubs stub should-have-invoked]]))

(describe "render emails for applicant and guardian"
  (with-stubs)
  (it "renders email for applicant"
      (let [template-params {:application-url "https://linkki.fi" :content-ending "Viestin loppu"}
            stub-render (stub :render)
            emails (email-confirmation/render-emails-for-applicant-and-guardian
                    ["teppo.testaa@testi.fi"]
                    []
                    "Vahvistusviesti"
                    "lahettilas@shakki.fi"
                    template-params
                    stub-render)]
        (should-have-invoked :render {:with [template-params]})
        (should= 1 (count emails))
        (should= ["teppo.testaa@testi.fi"] (:recipients (first emails)))))

  (it "renders email for applicant and guardian"
      (let [template-params {:application-url "https://linkki.fi" :content-ending "Viestin loppu"}
            stub-render (stub :render)
            emails (email-confirmation/render-emails-for-applicant-and-guardian
                     ["teppo.testaa@testi.fi" "tiina.testaa@testi.fi"]
                     ["huoltaja.testaa@testi.fi"]
                     "Vahvistusviesti"
                     "lahettilas@shakki.fi"
                     template-params
                     stub-render)]
        (should-have-invoked :render {:with [template-params]})
        (should-have-invoked :render {:with [{}]})
        (should= 2 (count emails))
        (should= ["teppo.testaa@testi.fi" "tiina.testaa@testi.fi"] (:recipients (first emails)))
        (should= ["huoltaja.testaa@testi.fi"] (:recipients (last emails))))))