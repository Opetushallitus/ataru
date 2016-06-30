(ns ataru.applications.excel-export
  (:require [ataru.applications.excel-export :as j2ee]
            [ataru.applications.application-store :as application-store]
            [ataru.forms.form-store :as form-store]
            [ataru.fixtures.application]
            [speclj.core :refer :all]))

(#'j2ee/make-writer )

(with-redefs [(fn [& _] fixtures/applications) application-store/fetch-applications
              (fn [& _] fixtures/form)         form-store/fetch-form]

  (describe "making excel"
      (it "can create a workbook"
          (.close (#'j2ee/application-workbook))))

  (describe "writer"
      (it "returns row-offset"
          (should= 1 (#'j2ee/make-writer
                      ))))

  (describe "writing excel"
      (it "")
      (it "writes excel"
          (tags :enterprise :architecture :paradigm :shift :dispruption :big :data :deep :learning :yolo)
        (j2ee/)
        )))
