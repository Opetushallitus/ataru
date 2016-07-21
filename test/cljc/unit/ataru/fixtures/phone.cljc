(ns ataru.fixtures.phone)

(def phone-list {nil false
                 "" false
                 "05012345" true
                 "+3585012345" true
                 "050" false
                 "+35850" false
                 "050 1234 56" true
                 "+358 50 1234 56" true
                 "+358 50 12 34 56" true})
