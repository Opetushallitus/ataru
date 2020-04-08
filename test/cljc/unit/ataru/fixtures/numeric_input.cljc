(ns ataru.fixtures.numeric-input)

(def numbers
  {"0"                                       true
   "-1"                                      true
   "+1"                                      true
   "1234"                                    true
   "0.00"                                    true
   "0,00"                                    true
   "0.1231"                                  true
   "0,1231"                                  true
   "-0.12313"                                true
   "+0.31231"                                true
   "+0,31231"                                true
   "0,0001"                                  true
   "-0,1231"                                 true
   "1,23123123"                              true           ; 8 decimals!
   "asfda"                                   false          ; Non numeric chars
   "asfda123"                                false          ; Non numeric chars
   "asfda123adf"                             false          ; Non numeric chars
   "123asdf"                                 false          ; Non numeric chars
   "as123,faf234"                            false          ; Non numeric chars
   "12313,asda"                              false          ; Non numeric chars
   "asfa,1231"                               false          ; Non numeric chars
   "00.1231"                                 false          ; Too many leading zeroes
   "001"                                     false          ; Leading zeroes
   "0,121.0"                                 false          ; Too many separators
   "0,121,0"                                 false          ; Too many separators
   "10."                                     false          ; No decimal part
   ".1201"                                   false          ; No integer part
   "1,231231239"                             false          ; Too many (9) decimals
   })


(def integers
  {"0"                      true
   "-1"                     true
   "+1"                     true
   "1234"                   true
   "0,00"                   false
   "0.1231"                 false
   "0,1231"                 false
   "-0.12313"               false
   "+0.31231"               false
   "+0,31231"               false
   "0,0001"                 false
   "-0,1231"                false
   "1,23123123"             false
   "asfda"                  false
   "asfda123"               false
   "asfda123adf"            false
   "123asdf"                false
   "as123,faf234"           false
   "12313,asda"             false
   "asfa,1231"              false
   "00.1231"                false
   "001"                    false
   "0,121.0"                false
   "0,121,0"                false
   "10."                    false
   ".1201"                  false
   "1,231231239"            false})

(def value-between
  {"1"    {true  [[nil "2"]
                  ["0" "2"]
                  ["-1" "2"]
                  ["-1" nil]
                  ["0" nil]
                  ["1" nil]
                  ["1" "1"]
                  ["1.0" "1.0000"]
                  ["1" "1.1"]
                  ["0.99999" "1.00001"]
                  ["0.99999" nil]
                  ["-0.99999" nil]
                  [nil "1.00001"]]
           false [[nil "0.9"]
                  ["1.00001" nil]
                  ["0.009" "0.9999"]]}
   "-1"   {true  [[nil "0"]
                  ["-1" nil]]
           false [["0" nil]]}
   "-0.9" {true  [["-0.9" nil]
                  [nil "-0.8"]
                  ["-0.99" nil]]
           false [["-0.8" nil]
                  ["-1" "-0.99"]]}
   "0"    {true  [["0" "0"]
                  ["-0.1" nil]
                  [nil "0.1"]
                  ["-0.99" "0.99"]
                  ["-100" "100"]]
           false [["1" nil]
                  [nil "-1"]]}})
