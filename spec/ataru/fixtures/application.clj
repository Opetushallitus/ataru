(ns ataru.fixtures.application
  (:require [clj-time.core :as c]))

(def form {
 :id 703,
 :name "Test fixture what is this",
 :modified-by "DEVELOPER",
 :modified-time (c/now)
 :content
 [{:id "G__31",
   :label {:fi "Osion nimi joo on", :sv "Avsnitt namn"},
   :children
   [{:id "G__19",
     :label {:fi "tekstiä", :sv ""},
     :required false,
     :fieldType "textField",
     :fieldClass "formField"}
    {:id "G__17",
     :label {:fi "ghj", :sv ""},
     :params {},
     :required false,
     :fieldType "textField",
     :fieldClass "formField"}
    {:id "G__17",
     :label {:fi "a", :sv ""},
     :params {},
     :required false,
     :fieldType "textField",
     :fieldClass "formField"}
    {:id "G__24",
     :label {:fi "gg", :sv ""},
     :params {},
     :required false,
     :fieldType "textField",
     :fieldClass "formField"}
    {:id "G__36",
     :label {:fi "c", :sv ""},
     :params {},
     :required false,
     :fieldType "textField",
     :fieldClass "formField"}],
   :fieldType "fieldset",
   :fieldClass "wrapperElement"}
  {:id "G__14",
   :label {:fi "aef", :sv ""},
   :params {},
   :required false,
   :fieldType "textField",
   :fieldClass "formField"}
  {:id "G__47",
   :label {:fi "freFD", :sv ""},
   :params {},
   :required false,
   :fieldType "textField",
   :fieldClass "formField"}]})

(def applications
  [{:key "c58df586-fdb9-4ee1-b4c4-030d4cfe9f81",
  :lang "fi",
  :modified-time (c/now)
  :form_id 703
  :state "received"
  :content
    {:answers
     [{:key "G__19", :label {:fi "Eka kysymys"}, :value "1", :fieldType "textField"}
      {:key "G__17", :label {:fi "Toka kysymys"}, :value "2", :fieldType "textField"}
      {:key "G__24", :label {:fi "Kolmas kysymys"}, :value "3", :fieldType "textField"}
      {:key "G__36", :label {:fi "Neljas kysymys"}, :value "4", :fieldType "textField"}
      {:key "G__14", :label {:fi "Viides kysymys"}, :value "5", :fieldType "textField"}
      {:key "G__47", :label {:fi "Kuudes kysymys"}, :value "6", :fieldType "textField"}]}}
 {:key "956ae57b-8bd2-42c5-90ac-82bd0a4fd31f",
  :lang "fi",
  :modified-time (c/now)
  :form_id 703
  :state "received"
  :content
    {:answers
     [{:key "G__19", :label {:fi "Eka kysymys"}, :value "Vastaus", :fieldType "textField"}
      {:key "G__17", :label {:fi "Toka kysymys"}, :value "lomakkeeseen", :fieldType "textField"}
      {:key "G__24", :label {:fi "Kolmas kysymys"}, :value "asiallinen", :fieldType "textField"}
      {:key "G__36", :label {:fi "Neljas kysymys"}, :value "vastaus", :fieldType "textField"}
      {:key "G__47", :label {:fi "Kuudes kysymys"}, :value "jee", :fieldType "textField"}]}}
 {:key "9d24af7d-f672-4c0e-870f-3c6999f105e0",
  :lang "fi",
  :modified-time (c/now)
  :form_id 703
  :state "received"
  :content
    {:answers
     [{:key "G__19", :label {:fi "Eka kysymys"}, :value "a", :fieldType "textField"}
      {:key "G__17", :label {:fi "Toka kysymys"}, :value "b", :fieldType "textField"}
      {:key "G__24", :label {:fi "Kolmas kysymys"}, :value "d", :fieldType "textField"}
      {:key "G__36", :label {:fi "Neljas kysymys"}, :value "e", :fieldType "textField"}
      {:key "G__14", :label {:fi "Seitsemas kysymys"}, :value "f", :fieldType "textField"}
      {:key "G__47", :label {:fi "kuudes kysymys"}, :value "g", :fieldType "textField"}]}}])
