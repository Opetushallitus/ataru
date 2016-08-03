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
  (let [row (.getRow sheet row-num)]
    (should-not-be-nil row)
    (doseq [col-idx (range (count expected-values))]
      (let [cell (.getCell row col-idx)
            expected (nth expected-values col-idx)]
        (if-not (nil? expected)
          (should= (nth expected-values col-idx) (.getStringCellValue cell))
          (should-be-nil cell))))))

(defn- verify-pane-information
  [sheet]
  (let [info (.getPaneInformation sheet)]
    (should (.isFreezePane info))
    (should= 1 (.getHorizontalSplitPosition info))
    (should= 0 (.getVerticalSplitPosition info))
    (should= 1 (.getHorizontalSplitTopRow info))
    (should= 0 (.getVerticalSplitLeftColumn info))))

(describe "excel export"
  (tags :unit)

  (around [spec]
    (with-redefs [application-store/exec-db (fn [& _] fixtures/applications)
                  form-store/fetch-form (fn [& _] fixtures/form)
                  application-store/get-application-review (fn [application-id]
                                                             (when (= application-id 3)
                                                               fixtures/application-review))]
      (spec)))

  (it "has expected values"
      (let [file (File/createTempFile (str "excel-" (UUID/randomUUID)) ".xlsx")]
        (try
          (with-open [output (FileOutputStream. (.getPath file))]
            (->> (j2ee/export-all-applications 99999999)
                 (.write output)))
          (let [workbook (WorkbookFactory/create file)
                metadata-sheet (.getSheetAt workbook 0)
                applications-sheet (.getSheetAt workbook 1)]
            (verify-row metadata-sheet 0
              ["Nimi" "Id" "Viimeksi muokattu" "Viimeinen muokkaaja"])
            (verify-row metadata-sheet 1
              ["Test fixture what is this" "703" "2016-06-14 12:34:56" "DEVELOPER"])
            (verify-row applications-sheet 0
              ["Id" "Lähetysaika" "Eka kysymys" "Toka kysymys" "Kolmas kysymys" "Neljas kysymys" "Viides kysymys" "Kuudes kysymys" "Seitsemas kysymys" "Muistiinpanot"])
            (verify-row applications-sheet 1
              ["c58df586-fdb9-4ee1-b4c4-030d4cfe9f81" "2016-06-15 12:30:55" "1" "2" "3" "4" "5" "6" nil nil])
            (verify-row applications-sheet 2
              ["956ae57b-8bd2-42c5-90ac-82bd0a4fd31f" "2016-06-15 14:30:55" "Vastaus" "lomakkeeseen" "asiallinen" "vastaus" nil "jee" nil])
            (verify-row applications-sheet 3
              ["9d24af7d-f672-4c0e-870f-3c6999f105e0" "2016-06-16 06:00:00" "a" "b" "d" "e" nil "g" "f" "Some notes about the applicant"])
            (verify-pane-information applications-sheet))
          (finally
            (.delete file))))))
