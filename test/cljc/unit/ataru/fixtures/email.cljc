(ns ataru.fixtures.email)

(def email-list {nil false
                 "" false
                 "devnull@foo.bar" true
                 "foo@bar@invalid.com" false
                 "foo.baz@valid.com" true
                 "foo.baz@v.a.l.i.d.com" true
                 "invalid.email@example" false
                 "invalid.email@example.,com" false
                 "invalid. email@example.com" false
                 "valid.email1@example.com,valid.email2@example.com" false
                 "invalid.em%0Ail@example.com" false
                 "invalid.emäil@example.com" false})
