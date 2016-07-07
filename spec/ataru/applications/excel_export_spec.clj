(ns ataru.applications.excel-export-spec
  (:require [ataru.applications.excel-export :as j2ee]
            [ataru.applications.application-store :as application-store]
            [ataru.forms.form-store :as form-store]
            [ataru.fixtures.application :as fixtures]
            [speclj.core :refer :all])
  (:import (java.io FileOutputStream File)
           (java.util UUID)
           (org.apache.poi.ss.usermodel WorkbookFactory)))

(describe "writing form"
  (tags :unit)

  (it "writes the form"
    (let [book (#'j2ee/application-workbook)]
      (should= 3 (#'j2ee/write-form! (#'j2ee/make-writer (.getSheetAt book 0) 0) fixtures/form)))))

(describe "writing excel"
  (tags :excel)

  (around [spec]
    (with-redefs [application-store/exec-db (fn [& _] fixtures/applications)
                  form-store/fetch-form (fn [& _] fixtures/form)]
      (spec)))

  (it "writes excel"
      (let [file (File/createTempFile (str "excel-" (UUID/randomUUID)) ".xlsx")]
        (try
          (with-open [output (FileOutputStream. (.getPath file))]
            (->> (j2ee/export-all-applications 99999999)
                 (.write output)))
          (let [sheet      (-> file WorkbookFactory/create (.getSheetAt 0))
                header-row (iterator-seq (.cellIterator (.getRow sheet 8)))
                headers    (map #(.getStringCellValue %) (take 6 header-row))]
            (should= (nth headers 0) "Eka kysymys")
            (should= (nth headers 1) "Toka kysymys")
            (should= (nth headers 2) "Kolmas kysymys")
            (should= (nth headers 3) "Neljas kysymys")
            (should= (nth headers 4) "Viides kysymys")
            (should= (nth headers 5) "Kuudes kysymys")
            (should (every? nil? (nthrest headers 6))))
          (finally
            (.delete file))))))
