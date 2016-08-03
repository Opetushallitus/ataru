(ns ataru.fixtures.date)

(def date-list
  {nil false
   "" false
   "11.11.1111" true
   "11111111" true
   "1.1.1111" true
   "4.11.2099" false
   "04112099" false})

