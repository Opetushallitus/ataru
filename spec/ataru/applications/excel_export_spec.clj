(ns ataru.applications.excel-export-spec
  (:require [ataru.applications.excel-export :as j2ee]
            [ataru.applications.application-store :as application-store]
            [ataru.forms.form-store :as form-store]
            [ataru.fixtures.application :as fixtures]
            [speclj.core :refer :all])
  (:import (java.io FileOutputStream File)
           (java.util UUID)))

(describe "writing form"
  (tags :unit)

  (it "writes the form"
    (let [book (#'j2ee/application-workbook)]
      (should= 3 (#'j2ee/write-form! (#'j2ee/make-writer (.getSheetAt book 0) 0) fixtures/form)))))

(describe "writing excel"
  (tags :unit)

  (around [spec]
    (with-redefs [application-store/fetch-applications (fn [& _] fixtures/applications)
                  form-store/fetch-form                (fn [& _] fixtures/form)]
      (spec)))

  (it "writes excel"
      (let [file (File/createTempFile (str "excel-" (UUID/randomUUID)) ".xlsx")]
        (try
          (with-open [output (FileOutputStream. (.getPath file))]
            (->> (j2ee/export-all-applications 99999999)
                 (.write output)))
          (finally
            (.delete file))))))
