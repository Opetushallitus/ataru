(ns ataru.applications.excel-export-spec
  (:require [ataru.applications.excel-export :as j2ee]
            [ataru.applications.application-store :as application-store]
            [ataru.forms.form-store :as form-store]
            [ataru.fixtures.application :as fixtures]
            [speclj.core :refer :all])
  (:import (java.io FileOutputStream File)
           (java.util UUID)
           (org.apache.poi.ss.usermodel WorkbookFactory)))

(defn- verify-row
  [sheet row-num expected-values]
  (let [row   (.getRow sheet row-num)]
    (should-not-be-nil row)
    (doseq [col-idx (range (count expected-values))]
      (let [cell (.getCell row col-idx)
            expected (nth expected-values col-idx)]
        (if-not (nil? expected)
          (should= (nth expected-values col-idx) (.getStringCellValue cell))
          (should-be-nil cell))))))

(describe "writing excel"
  (tags :unit)

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
            (verify-row sheet 0
              ["Eka kysymys" "Toka kysymys" "Kolmas kysymys" "Neljas kysymys" "Viides kysymys" "Kuudes kysymys" "Seitsemas kysymys" "kuudes kysymys"])
            (verify-row sheet 1
              ["1" "2" "3" "4" "5" "6"])
            (verify-row sheet 2
              ["Vastaus" "lomakkeeseen" "asiallinen" "vastaus" nil "jee"])
            (verify-row sheet 3
              ["a" "b" "d" "e" nil nil "f" "g"]))
          (finally
            (.delete file))))))
