(ns ataru.applications.application-store-spec
  (:require [ataru.applications.application-store :as store]
            [ataru.fixtures.application :as fixtures]
            [speclj.core :refer :all]))

(def form-id (:id fixtures/form))

(def expected-applications
  [{:key "c58df586-fdb9-4ee1-b4c4-030d4cfe9f81",
    :lang "fi",
    :form-id 703
    :state :received
    :answers
    [{:key "G__19", :label "Eka kysymys", :value "1", :fieldType "textField"}
     {:key "G__17", :label "Toka kysymys", :value "2", :fieldType "textField"}
     {:key "G__24", :label "Kolmas kysymys", :value "3", :fieldType "textField"}
     {:key "G__36", :label "Neljas kysymys", :value "4", :fieldType "textField"}
     {:key "G__14", :label "Viides kysymys", :value "5", :fieldType "textField"}
     {:key "G__47", :label "Kuudes kysymys", :value "6", :fieldType "textField"}]}
   {:key "956ae57b-8bd2-42c5-90ac-82bd0a4fd31f",
    :lang "fi",
    :form-id 703
    :state :received
    :answers
    [{:key "G__19", :label "Eka kysymys", :value "Vastaus", :fieldType "textField"}
     {:key "G__17", :label "Toka kysymys", :value "lomakkeeseen", :fieldType "textField"}
     {:key "G__24", :label "Kolmas kysymys", :value "asiallinen", :fieldType "textField"}
     {:key "G__36", :label "Neljas kysymys", :value "vastaus", :fieldType "textField"}
     {:key "G__47", :label "Kuudes kysymys", :value "jee", :fieldType "textField"}]}
   {:key "9d24af7d-f672-4c0e-870f-3c6999f105e0",
    :lang "fi",
    :form-id 703
    :state :received
    :answers
    [{:key "G__19", :label "Eka kysymys", :value "a", :fieldType "textField"}
     {:key "G__17", :label "Toka kysymys", :value "b", :fieldType "textField"}
     {:key "G__24", :label "Kolmas kysymys", :value "d", :fieldType "textField"}
     {:key "G__36", :label "Neljas kysymys", :value "e", :fieldType "textField"}
     {:key "G__14", :label "Seitsemas kysymys", :value "f", :fieldType "textField"}
     {:key "G__47", :label "kuudes kysymys", :value "g", :fieldType "textField"}]}])

(describe "fetch-applications"
  (tags :unit)

  (around [spec]
    (with-redefs [store/exec-db (fn [ds-key query-fn params]
                                  (should= :db ds-key)
                                  (should= "yesql-application-query-by-modified" (-> query-fn .meta :name))
                                  (should= {:form_id 703, :limit 100, :lang "fi"} params)
                                  fixtures/applications)]
      (spec)))

  (it "should return all applications belonging to a form"
    (should= expected-applications (map
                                     #(dissoc % :modified-time)
                                     (store/fetch-applications form-id {})))))
