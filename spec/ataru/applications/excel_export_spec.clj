(ns ataru.applications.excel-export-spec
  (:require [ataru.applications.excel-export :as j2ee]
            [ataru.applications.application-store :as application-store]
            [ataru.forms.form-store :as form-store]
            [ataru.fixtures.application :as fixtures]
            [speclj.core :refer :all]))

(describe "writing form"
  (tags :unit)

  (it "writes the form"
    (let [book (#'j2ee/application-workbook)]
      (should= 3 (#'j2ee/write-form! (#'j2ee/make-writer (.getSheetAt book 0) 0) fixtures/form)))))

(describe "writing excel"
  (tags :unit)

  (it "writes excel"

    (with-redefs [application-store/fetch-applications (fn [& _] fixtures/applications)
                  form-store/fetch-form                (fn [& _] fixtures/form)]

      (->> (j2ee/export-all-applications 99999999)
        (.write (java.io.FileOutputStream. "/tmp/foo.xlsx"))))))
