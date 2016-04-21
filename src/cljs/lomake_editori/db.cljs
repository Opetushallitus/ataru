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
   :editor {:forms [{:id "1" :name "Placeholder-lomake, pitäisi korvautua ajax-datalla" :form-data {}}]
   Fetc         :selected-form-id "1111111"}
   ; Initial active panel on page load.
   :active-panel :editor})
