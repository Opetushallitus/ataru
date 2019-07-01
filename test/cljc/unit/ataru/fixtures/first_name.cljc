(ns ataru.fixtures.first-name)

(def first-name-list [["Kari-Pekka" "Kari-Pekka" true]
                      ["Kari-Pekka" "Kari" false]
                      ["Kari-Pekka" "Pekka" false]
                      ["Kari-Pekka" "Pekk" false]
                      ["Kari-Pekka" "Kar" false]
                      ["Yrjö Liisa Zalgo" "Ykä" false]
                      ["Yrjö Liisa Zalgo" "Yrjö" true]
                      ["Yrjö Liisa Zalgo" "Yrjö Liisa" false]
                      ["Yrjö Liisa Zalgo" "Liisa Zalgo" false]
                      ["Yrjö Liisa Zalgo" "Liisa" true]
                      ["Yrjö Liisa Zalgo" "Yrjö69" false]
                      [" Yrjö  Liisa " " Yrjö" false]
                      [" Yrjö  Liisa " "" false]])
