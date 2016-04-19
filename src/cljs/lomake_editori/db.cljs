(ns lomake-editori.db)

(def default-db
  {:application {:degree-programme "Hierojan ammattitutkinto, koulutettu hieroja - Stadin Aikuisopisto"
                 :applications [{:id 1
                                 :applicant {:first "Jussi"
                                             :last "Fagerlund"}
                                 :arrived-at "15.4.2016 klo 11:32"
                                 :state {:description "Käsittelemättä"}}
                                {:id 2
                                 :applicant {:first "Esko"
                                             :last "Mörkö"}
                                 :arrived-at "10.5.2016 klo 10:11"
                                 :state {:description "Käsittelemättä"}}]}
   :editor {:forms [{:id "1111111" :name "Stadin aikuisopiston yhteinen lomake" :form-data {}}
                    {:id "2222222" :name "Salpauksen lomake" :form-data {}}
                    {:id "3333333" :name "Helsingin kaupungin lomake" :form-data {}}
                    {:id "4444444" :name "Aallon lomake" :form-data {}}
                    {:id "5555555" :name "Akin lomake" :form-data {}}
                    {:id "6666666" :name "Porvoon lomake" :form-data {}}]
            :selected-form-id "1111111"}
   ; Initial active panel on page load.
   :active-panel :editor})
