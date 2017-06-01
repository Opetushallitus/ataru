(ns ataru.fixtures.first-name)

(def first-name-list [["Kari-Pekka" "Kari-Pekka" true]
                      ["Kari-Pekka" "Kari" true]
                      ["Kari-Pekka" "Pekka" true]
                      ["Kari-Pekka" "Pekk" false]
                      ["Kari-Pekka" "Kar" false]
                      ["Yrjö Liisa Zalgo" "Ykä" false]
                      ["Yrjö Liisa Zalgo" "Yrjö" true]
                      ["Yrjö Liisa Zalgo" "Yrjö Liisa" true]
                      ["Yrjö Liisa Zalgo" "Liisa Zalgo" true]
                      ["Yrjö Liisa Zalgo" "Liisa" true]
                      ["Yrjö Liisa Zalgo" "Yrjö69" false]])
