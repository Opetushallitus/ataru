(ns ataru.fixtures.first-name)

(def first-name-list [["" "Kari" false]
                      ["Kari" "" false]
                      ["Kari-Pekka" "Kari-Pekka" true]
                      ["Kari-Pekka" "Kari" true]
                      ["Kari-Pekka" "Pekka" true]
                      ["Kari-Pekka" "Pekk" false]
                      ["Kari-Pekka" "Kar" false]
                      ["Kari-Pekka" "Kari Pekka" true]
                      ["Kari Pekka" "Kari-Pekka" false]
                      ["Kari  Pekka" "Kari Pekka" false]
                      ["Kari Pekka" "Kari  Pekka" false]
                      ["Kari Pekka" "Pekka Kari" false]
                      ["Yrjö Liisa Zalgo" "Ykä" false]
                      ["Yrjö Liisa Zalgo" "Yrjö" true]
                      ["Yrjö Liisa Zalgo" "Yrjö Liisa" true]
                      ["Yrjö Liisa Zalgo" "Liisa Zalgo" true]
                      ["Yrjö Liisa Zalgo" "Liisa" true]
                      ["Yrjö Liisa Zalgo" "Yrjö69" false]
                      ["Yrjö Liisa" "Yrjö  Liisa" false]
                      ["Yrjö  Liisa Zalgo" "Yrjö Liisa  Zalgo" false]
                      ["Yrjö-Liisa Zalgo" "Yrjö Liisa" true]
                      ["Yrjö-Liisa Zalgo" "Liisa Zalgo" true]
                      ["Yrjö Liisa Zalgo" "Yrjö-Liisa" false]])
