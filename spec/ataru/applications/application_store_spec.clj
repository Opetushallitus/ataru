(ns ataru.applications.application-store-spec
  (:require [ataru.applications.application-store :as store]
            [ataru.component-data.higher-education-base-education-module :as hebem]
            [ataru.fixtures.application :as fixtures]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.forms.form-store :as forms]
            [ataru.util :as util]
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
                                                (->> fixtures/applications
                                                     (filter #(empty? (:hakukohde %)))
                                                     (sort-by :created-time)
                                                     reverse))]
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
                                                (->> fixtures/applications
                                                     (filter #(and (contains? (set (:hakukohde %)) hakukohde-oid)
                                                                   (= (:form_id %) 703)))
                                                     (sort-by :created-time)
                                                     reverse))]
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

(describe "getting application edit history"
  (tags :unit :versions)

  (it "should get history"
    (let [expected (filter #(= "9d24af7d-f672-4c0e-870f-aaaa" (:key %)) fixtures/applications)]
      (with-redefs [store/exec-db (fn [ds-key query-fn params]
                                    (should= :db ds-key)
                                    (should= "yesql-get-application-versions" (-> query-fn .meta :name))
                                    (should= {:application_key "9d24af7d-f672-4c0e-870f-aaaa"} params)
                                    expected)
                    forms/get-form-by-application (fn [_] form-fixtures/version-test-form)]
        (should== [{:G__224 {:label "Toistuva kysymys ryhmässä"
                             :old   [["x" "y" "z"]
                                     ["a" "b" "c"]]
                             :new   [["x" "y" "1"]
                                     ["a" "b" "asdfa"]]}
                    :G__119 {:label "Eka kysymys"
                             :old   "z"
                             :new   ""}
                    :G__117 {:label "Toistuva kysymys"
                             :old   ["x" "y" "z"]
                             :new   ["x" "y" "a"]}}]
                  (store/get-application-version-changes nil "9d24af7d-f672-4c0e-870f-aaaa"))))))

(describe "creating application attachment reviews"
  (tags :unit :attachments)

  (it "should handle attachment with both ryhma and hakukohdeliitos"
      (let [create-field (fn [id parent-id hakukohteet ryhmat]
                             {:id                        id
                              :belongs-to-hakukohteet    hakukohteet
                              :belongs-to-hakukohderyhma ryhmat
                              :children-of               parent-id})
            fields-by-id {:a (create-field :a nil
                               nil
                               nil)
                          :b (create-field :b :a
                               [:ha]
                               [:rb])
                          :c (create-field :c :b
                               [:ha]
                               [])
                          :d (create-field :d :c
                               []
                               [:rb])
                          :e (create-field :e :d
                               [:ha]
                               [:ra])
                          :f (create-field :f :e
                               [:hc]
                               [:rb])
                          :g (create-field :g :f
                               [:ha :hb :hc :hd]
                               [:ra :rb])}
            hakutoiveet  [{:oid :hd :hakukohderyhmat [:rb :ra]}
                          {:oid :hc :hakukohderyhmat [:ra]}
                          {:oid :hb :hakukohderyhmat [:rb]}
                          {:oid :ha :hakukohderyhmat [:rb]}]
            result       (store/hakukohde-oids-for-attachment-review
                           (:g fields-by-id) hakutoiveet fields-by-id false #{})]
        (should== [:ha] result)))

  (it "should create attachment reviews for new application without hakukohteet"
    (let [application       (first (filter #(= "attachments" (:key %)) fixtures/applications))
          flat-form-content (util/flatten-form-fields (:content form-fixtures/attachment-test-form))
          answers-by-key    (-> application :content :answers util/answers-by-key)
          fields-by-id      (util/form-fields-by-id form-fixtures/attachment-test-form)]
      (should== [{:application_key "attachments"
                  :attachment_key  "att__1"
                  :state           "not-checked"
                  :updated?        false
                  :hakukohde       "form"}
                 {:application_key "attachments"
                  :attachment_key  "att__2"
                  :state           "attachment-missing"
                  :updated?        false
                  :hakukohde       "form"}]
                (store/create-application-attachment-reviews
                 (:key application)
                 (store/filter-visible-attachments answers-by-key
                                                   flat-form-content
                                                   fields-by-id)
                 answers-by-key
                 nil
                 []
                 false
                 fields-by-id
                 #{}))))

  (it "should create attachment reviews for new application with hakukohteet"
    (let [application       (first (filter #(= "attachments" (:key %)) fixtures/applications))
          flat-form-content (util/flatten-form-fields (:content form-fixtures/attachment-test-form))
          answers-by-key    (-> application :content :answers util/answers-by-key)
          fields-by-id      (util/form-fields-by-id form-fixtures/attachment-test-form)]
      (should== [{:application_key "attachments"
                  :attachment_key  "att__1"
                  :state           "not-checked"
                  :updated?        false
                  :hakukohde       "hakukohde1"}
                 {:application_key "attachments"
                  :attachment_key  "att__1"
                  :state           "not-checked"
                  :updated?        false
                  :hakukohde       "hakukohde2"}
                 {:application_key "attachments"
                  :attachment_key  "att__2"
                  :state           "attachment-missing"
                  :updated?        false
                  :hakukohde       "hakukohde1"}
                 {:application_key "attachments"
                  :attachment_key  "att__2"
                  :state           "attachment-missing"
                  :updated?        false
                  :hakukohde       "hakukohde2"}]
                (store/create-application-attachment-reviews
                 (:key application)
                 (store/filter-visible-attachments answers-by-key
                                                   flat-form-content
                                                   fields-by-id)
                 answers-by-key
                 nil
                 [{:oid "hakukohde1"} {:oid "hakukohde2"}]
                 false
                 fields-by-id
                 #{}))))

  (it "should create attachment reviews only for visible attachments"
    (let [flat-form-content (util/flatten-form-fields (:content form-fixtures/visible-attachment-test-form))
          fields-by-id      (util/form-fields-by-id form-fixtures/visible-attachment-test-form)]
      (should== [{:application_key "application-oid"
                  :attachment_key  "attachment_1"
                  :state           "attachment-missing"
                  :updated?        false
                  :hakukohde       "hakukohde1"}]
                (store/create-application-attachment-reviews
                 "application-oid"
                 (store/filter-visible-attachments {:choice_1 {:value "0"}}
                                                   flat-form-content
                                                   fields-by-id)
                 {:choice_1 {:value "0"}}
                 nil
                 [{:oid "hakukohde1"}]
                 false
                 fields-by-id
                 #{}))
      (should== [{:application_key "application-oid"
                  :attachment_key  "attachment_3"
                  :state           "attachment-missing"
                  :updated?        false
                  :hakukohde       "hakukohde1"}]
                (store/create-application-attachment-reviews
                 "application-oid"
                 (store/filter-visible-attachments {:choice_1 {:value "2"}}
                                                   flat-form-content
                                                   fields-by-id)
                 {:choice_1 {:value "2"}}
                 nil
                 [{:oid "hakukohde1"}]
                 false
                 fields-by-id
                 #{}))))

  (it "should update attachment reviews for application without hakukohteet"
    (let [application       (first (filter #(= "attachments" (:key %)) fixtures/applications))
          flat-form-content (util/flatten-form-fields (:content form-fixtures/attachment-test-form))
          answers-by-key    (-> application :content :answers util/answers-by-key)
          fields-by-id      (util/form-fields-by-id form-fixtures/attachment-test-form)]
      (should== [{:application_key "attachments"
                  :attachment_key  "att__1"
                  :state           "not-checked"
                  :updated?        false
                  :hakukohde       "form"}
                 {:application_key "attachments"
                  :attachment_key  "att__2"
                  :state           "attachment-missing"
                  :updated?        true
                  :hakukohde       "form"}]
                (store/create-application-attachment-reviews
                 (:key application)
                 (store/filter-visible-attachments answers-by-key
                                                   flat-form-content
                                                   fields-by-id)
                 answers-by-key
                 {:att__1 {:value ["liite-id"]}
                  :att__2 {:value ["32131"]}}
                 []
                 true
                 fields-by-id
                 #{})))))

(describe "application with base education"
          (tags :unit :attachments)

  (it "should be possible to extract attachment ids from base education module"
    (should== #{"pohjakoulutus_kk_ulk--attachement"
                "pohjakoulutus_lk--attachment"
                "pohjakoulutus_avoin--attachment"
                "pohjakoulutus_am--attachment"
                "pohjakoulutus_amt--attachment"
                "pohjakoulutus_ulk--attachment"
                "pohjakoulutus_kk--attachment"
                "pohjakoulutus_muu--attachment"}
              (hebem/non-yo-attachment-ids
               form-fixtures/base-education-attachment-test-form))))
