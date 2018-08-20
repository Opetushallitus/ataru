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
   [["111.0"]]                               true
   [["abc"]]                                 false
   [["1.0"] ["2"]]                           true
   [["1" "2.000"] ["3" "4.45"]]              true
   [["1"] ["1.2"]]                           true
   [["x"] ["1"]]                             false
   [["1" "x"] ["3" "4"]]                     false
   [["1" "2"] ["3" "4.4534342342342342334"]] false})


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
   "1,231231239"            false
   [["23423"]]              true
   [["111.0"]]              false
   [["abc"]]                false
   [["1"] ["2"]]            true
   [["1" "2"] ["3" "4"]]    true
   [["1"] ["1.2"]]          false
   [["x"] ["1"]]            false
   [["1" "x"] ["3" "4"]]    false
   [["1" "2"] ["3" "4.45"]] false})
