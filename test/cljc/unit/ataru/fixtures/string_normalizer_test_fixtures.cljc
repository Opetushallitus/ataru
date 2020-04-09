(ns ataru.fixtures.string-normalizer-test-fixtures)

(def string-normalizer-fixtures
  [{:expected "IELTS_Test-Report_Formq__General_training_.pdf" :input "IELTS Test-Report Formq (General training).pdf"}
   {:expected "KPa_Tyo_todistus__VTT.pdf" :input "KPa_Tyo?todistus_ VTT.pdf"}
   {:expected "7.IELTS.jpg" :input "7.IELTS.jpg"}
   {:expected "______________.jpg" :input "???? ????? ???.jpg"}
   {:expected "IMG_20180918_171840_2.jpg" :input "IMG_20180918_171840~2.jpg"}
   {:expected "tyotodistus.jpg" :input "työtodistus.jpg"}
   {:expected "Ahtari.txt" :input "Ähtäri.txt"}
   {:expected "Aland_ake.txt" :input "Åland åke.txt"}
   {:expected "O_o.txt" :input "Ö_ö.txt"}
   {:expected "Creme_Brulee.jpg" :input "Crème Brulée.jpg"}
   {:expected "___________________________.txt" :input "!\"#$%&'()*+,/:;<=>?[\\]^{|}~.txt"}])
