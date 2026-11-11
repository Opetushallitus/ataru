(ns ataru.applications.feedback-excel-spec
  (:require [ataru.applications.feedback-excel :as feedback-excel]
            [ataru.test-utils :refer [with-excel-workbook]]
            [ataru.time :as t]
            [speclj.core :refer [describe it should= should-be-nil tags]])
  (:import [org.apache.poi.ss.usermodel Workbook Sheet CellType]))

(defn- cell-text [^Sheet sheet row column]
  (some-> (.getRow sheet (int row))
          (.getCell (int column))
          (.getStringCellValue)))

(defn- cell-number [^Sheet sheet row column]
  (some-> (.getRow sheet (int row))
          (.getCell (int column))
          (.getNumericCellValue)))

(defn- first-sheet ^Sheet [^Workbook workbook]
  (.getSheetAt workbook 0))

(def ^:private feedback
  [{:created-time (t/to-time-zone (t/date-time 2026 11 7 14 5 9) (t/time-zone-for-id "Europe/Helsinki"))
    :form-key     "form-key"
    :form-id      1
    :form-name    "Testilomake"
    :haku-oid     "1.2.246.562.29.00000000001"
    :stars        4
    :feedback     "Hyvä lomake"
    :user-agent   "Mozilla/5.0 (test)"}
   {:created-time (t/to-time-zone (t/date-time 2026 11 6 9 0 0) (t/time-zone-for-id "Europe/Helsinki"))
    :form-key     "form-key"
    :form-id      1
    :form-name    "Testilomake"
    :haku-oid     nil
    :stars        1
    :feedback     "=HYPERLINK(\"http://example.org\")"
    :user-agent   nil}])

(describe "feedback excel"
  (tags :unit :feedback-excel)

  (it "writes a header row in the requested language"
    (with-excel-workbook (feedback-excel/export-feedback feedback :fi)
      (fn [workbook]
        (let [sheet (first-sheet workbook)]
          (should= "Palautteet" (.getSheetName sheet))
          (should= ["Aikaleima" "Arvosana" "Palaute" "Käyttäjäagentti" "Haku"]
                   (map #(cell-text sheet 0 %) (range 5))))))
    (with-excel-workbook (feedback-excel/export-feedback feedback :sv)
      (fn [workbook]
        (let [sheet (first-sheet workbook)]
          (should= "Respons" (.getSheetName sheet))
          (should= "Tidsstämpel" (cell-text sheet 0 0))))))

  (it "writes one row per feedback in the given order"
    (with-excel-workbook (feedback-excel/export-feedback feedback :fi)
      (fn [workbook]
        (let [sheet (first-sheet workbook)]
          (should= 3 (.getPhysicalNumberOfRows sheet))
          (should= "2026-11-07 14:05:09" (cell-text sheet 1 0))
          (should= 4.0 (cell-number sheet 1 1))
          (should= "Hyvä lomake" (cell-text sheet 1 2))
          (should= "Mozilla/5.0 (test)" (cell-text sheet 1 3))
          (should= "1.2.246.562.29.00000000001" (cell-text sheet 1 4))
          (should= "2026-11-06 09:00:00" (cell-text sheet 2 0))
          (should= 1.0 (cell-number sheet 2 1))))))

  (it "leaves missing values blank"
    (with-excel-workbook (feedback-excel/export-feedback feedback :fi)
      (fn [workbook]
        (let [sheet (first-sheet workbook)]
          (should-be-nil (.getCell (.getRow sheet 2) 3))
          (should-be-nil (.getCell (.getRow sheet 2) 4))))))

  (it "stores applicant text starting like a formula as quoted text"
    (with-excel-workbook (feedback-excel/export-feedback feedback :fi)
      (fn [workbook]
        (let [cell (.getCell (.getRow (first-sheet workbook) 2) 2)]
          (should= CellType/STRING (.getCellType cell))
          (should= true (.getQuotePrefixed (.getCellStyle cell)))
          (should= "=HYPERLINK(\"http://example.org\")" (.getStringCellValue cell))))))

  (it "stores text with whitespace before a formula character as quoted text"
    (doseq [text [" =1+1" "\t=1+1" "\r\n@SUM(A1)" "  -1" "+1"]]
      (with-excel-workbook (feedback-excel/export-feedback [{:stars 1 :feedback text}] :fi)
        (fn [workbook]
          (let [cell (.getCell (.getRow (first-sheet workbook) 1) 2)]
            (should= true (.getQuotePrefixed (.getCellStyle cell)))
            (should= text (.getStringCellValue cell)))))))

  (it "does not quote ordinary text"
    (with-excel-workbook (feedback-excel/export-feedback feedback :fi)
      (fn [workbook]
        (let [cell (.getCell (.getRow (first-sheet workbook) 1) 2)]
          (should= false (.getQuotePrefixed (.getCellStyle cell)))))))

  (it "truncates values that are too long for an excel cell"
    (let [user-agent (apply str (repeat 40000 "a"))]
      (with-excel-workbook (feedback-excel/export-feedback [{:stars 1 :user-agent user-agent}] :fi)
        (fn [workbook]
          (let [value (cell-text (first-sheet workbook) 1 3)]
            (should= 5000 (count value))
            (should= (str (subs user-agent 0 4999) "…") value))))))

  (it "writes only the header row when there is no feedback"
    (with-excel-workbook (feedback-excel/export-feedback [] :en)
      (fn [workbook]
        (let [sheet (first-sheet workbook)]
          (should= 1 (.getPhysicalNumberOfRows sheet))
          (should= "Feedback" (.getSheetName sheet)))))))
