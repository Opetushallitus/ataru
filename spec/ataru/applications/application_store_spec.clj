(ns ataru.applications.application-store-spec
  (:require [ataru.applications.application-store :as store]
            [ataru.fixtures.application :as fixtures]
            [clj-time.core :as c]
            [speclj.core :refer :all]))

(def form-key (:key fixtures/form))

(def expected-applications
  [{:key     "9d24af7d-f672-4c0e-870f-3c6999f105e0",
    :lang    "fi",
    :form-id 703
    :id      3
    :answers
             [{:key "G__19", :label "Eka kysymys", :value "a", :fieldType "textField"}
              {:key "G__17", :label "Toka kysymys", :value "b", :fieldType "textField"}
              {:key "G__24", :label "Kolmas kysymys", :value "d", :fieldType "textField"}
              {:key "G__36", :label "Neljas kysymys", :value "e", :fieldType "textField"}
              {:key "G__14", :label "Seitsemas kysymys", :value "f", :fieldType "textField"}
              {:key "G__47", :label "Kuudes kysymys", :value "g", :fieldType "textField"}]}
   {:key     "956ae57b-8bd2-42c5-90ac-82bd0a4fd31f",
    :lang    "fi",
    :form-id 703
    :id      2
    :answers
             [{:key "G__19", :label "Eka kysymys", :value "Vastaus", :fieldType "textField"}
              {:key "G__17", :label "Toka kysymys", :value "lomakkeeseen", :fieldType "textField"}
              {:key "G__24", :label "Kolmas kysymys", :value "asiallinen", :fieldType "textField"}
              {:key "G__36", :label "Neljas kysymys", :value "vastaus", :fieldType "textField"}
              {:key "G__47", :label "Kuudes kysymys", :value "jee", :fieldType "textField"}]}
   {:key     "c58df586-fdb9-4ee1-b4c4-030d4cfe9f81",
    :lang    "fi",
    :form-id 703
    :id      1
    :answers
             [{:key "G__19", :label "Eka kysymys", :value "1", :fieldType "textField"}
              {:key "G__17", :label "Toka kysymys", :value "2", :fieldType "textField"}
              {:key "G__24", :label "Kolmas kysymys", :value "3", :fieldType "textField"}
              {:key "G__36", :label "Neljas kysymys", :value "4", :fieldType "textField"}
              {:key "G__14", :label "Viides kysymys", :value "5", :fieldType "textField"}
              {:key "G__47", :label "Kuudes kysymys", :value "6", :fieldType "textField"}]}])

(def expected-hakukohde-application-ids [5 4])

(def person-oid "1.2.246.562.24.96282369159")

(def hakukohde-oid "1.2.246.562.29.11111111110")

(describe "get-applications"
          (tags :unit)

          (around [spec]
                  (with-redefs [store/exec-db (fn [ds-key query-fn params]
                                                (should= :db ds-key)
                                                (should= "yesql-get-applications-for-form" (-> query-fn .meta :name))
                                                (should= {:form_key "abcdefghjkl" :filtered_states ["unprocessed"]} params)
                                                (filter #(empty? (:hakukohde %)) fixtures/applications))]
                    (spec)))

          (it "should return all applications belonging to a form"
              (should=
               (mapv #(select-keys % [:id :key]) expected-applications)
               (mapv #(select-keys % [:id :key]) (store/get-applications-for-form form-key ["unprocessed"])))))

(describe "get-applications"
          (tags :unit)

          (around [spec]
                  (with-redefs [store/exec-db (fn [ds-key query-fn params]
                                                (should= :db ds-key)
                                                (should= "yesql-get-applications-for-hakukohde" (-> query-fn .meta :name))
                                                (should= {:filtered_states ["unprocessed"]
                                                          :hakukohde_oid   hakukohde-oid}
                                                         params)
                                                (filter #(and (contains? (set (:hakukohde %)) hakukohde-oid) (= (:form_id %) 703)) fixtures/applications))]
                    (spec)))

          (it "should return all applications belonging to a hakukohde"
              (should=
               expected-hakukohde-application-ids
               (mapv :id (store/get-applications-for-hakukohde ["unprocessed"] hakukohde-oid)))))

(describe "setting person oid to application"
          (tags :unit)

          (it "should set person oid to the application"
              (let [expected (assoc (first fixtures/applications) :person-oid person-oid)]
                (with-redefs [store/exec-db (fn [ds-key query-fn params]
                                              (should= :db ds-key)
                                              (should= "yesql-add-person-oid!" (-> query-fn .meta :name))
                                              (should= {:id 1 :person_oid person-oid} params)
                                              expected)]
                  (should= expected
                           (store/add-person-oid (:id expected) person-oid))))))
