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
  (tags :excel)

  (it "writes the form"
    (let [book (#'j2ee/application-workbook)]
      (should= 3 (#'j2ee/write-form! (#'j2ee/make-writer (.getSheetAt book 0) 0) fixtures/form)))))

(defn- verify-row
  [sheet row-num expected-values]
  (let [row (iterator-seq (.cellIterator (.getRow sheet row-num)))
        values (map #(.getStringCellValue %) (take 6 row))]
    (should= expected-values values)
    (should (every? nil? (nthrest values 6)))))

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
          (let [sheet (-> file WorkbookFactory/create (.getSheetAt 0))]
            (verify-row sheet 8
              ["Eka kysymys" "Toka kysymys" "Kolmas kysymys" "Neljas kysymys" "Viides kysymys" "Kuudes kysymys"])
            (verify-row sheet 9
              ["1" "2" "3" "4" "5" "6"])
            (verify-row sheet 10
              ["Vastaus" "lomakkeeseen" "asiallinen" "vastaus" "joo" "jee"])
            (verify-row sheet 11
              ["a" "b" "d" "e" "f" "g"]))
          (finally
            (.delete file))))))
