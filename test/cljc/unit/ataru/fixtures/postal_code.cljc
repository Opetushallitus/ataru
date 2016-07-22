(ns ataru.fixtures.postal-code)

(def postal-code-list {nil     false
                       ""      false
                       "0"     false
                       "00"    false
                       "001"   false
                       "0010"  false
                       "00100" true
                       "a0000" false
                       "0a000" false
                       "00a00" false
                       "000a0" false
                       "0000a" false})
