(ns ataru.email-spec
  (:require [ataru.email :as email]
            [speclj.core :refer :all]))

(describe "ataru.email/email?"
  (tags :unit)

  (def email-list
    [["franksmith@example.com" true]
     ["frank.smith@example.com" true]
     ["frank.smith+tag@example.com" true]
     ["frank.smith.lloyd@example.com" true]
     ["franksmith@example.domain.com" true]
     ["frank-smith@example.com" true]
     ["frank_smith@example.com" true]
     ["franksmith@example-domain.com" true]
     ["smith,frank@example.com" false]
     ["frank smith@example.com" false]
     ["frank@smith+tag@example.com" false]
     ["franksmith@example,com" false]
     ["franksmith@example domain.com" false]
     ["franksmith@äää.com" false]
     ["äää@example.com" false]
     [nil false]])

  (doall
  (for [[email expected] email-list]
    (it (str "should validate email " email " as " expected)
      (should= expected (email/email? email))))))
