(ns ataru.applications.excel-export-spec
  (:require [ataru.applications.excel-export-spec :as j2ee]
            [ataru.applications.application-store :as application-store]
            [ataru.forms.form-store :as form-store]
            [ataru.fixtures.application :as fixtures]
            [speclj.core :refer :all]))

(describe "writing form"
    (it "writes the form"
        (let [book (#'j2ee/application-workbook)]
          (should= 3 (#'j2ee/write-form! (#'j2ee/make-writer (.getSheetAt book 0) 0) fixtures/form)))))

(describe "writing excel"
    (it "writes excel"
        (tags :enterprise :architecture :paradigm :shift :dispruption :big :data :deep :learning :yolo)

      (with-redefs [application-store/fetch-applications (fn [& _] fixtures/applications)
                    form-store/fetch-form                (fn [& _] fixtures/form)]

        (->> (j2ee/export-all-applications 99999999)
             (.write (java.io.FileOutputStream. "/tmp/foo.xlsx"))))))
