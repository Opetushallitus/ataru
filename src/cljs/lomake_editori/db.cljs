(ns lomake-editori.db)

(def default-db
  {:application {:applications [{:id 1
                                 :applicant {:first "Jussi"
                                             :last "Fagerlund"}
                                 :arrived-at "15.4.2016 klo 11:32"
                                 :state {:description "Käsittelemättä"}}]}
   :active-panel :editor})
