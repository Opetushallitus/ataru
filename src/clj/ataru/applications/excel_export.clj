(ns ataru.applications.excel-export
  (:import [org.apache.poi.ss.usermodel WorkbookFactory Row])
  (:require [ataru.forms.form-store :as form-store]
            [ataru.applications.application-store :as application-store]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.string :refer [trim]]
            [clojure.core.match :refer [match]]
            [clojure.java.io :refer [input-stream]]
            [taoensso.timbre :refer [spy]]))

(def ^:private row-offset 9)

(defn- application-workbook []
  (WorkbookFactory/create
    (input-stream
      "resources/templates/application-export-template.xlsx")))

(defn- update-row-cell! [sheet row column value]
  (when-let [v (not-empty (trim (str value)))]
    (-> (or (.getRow sheet row)
            (.createRow sheet row))
        (.getCell column Row/CREATE_NULL_AS_BLANK)
        (.setCellValue v)))
  sheet)

(defn- make-writer [sheet offset]
  (fn [row column value]
    (update-row-cell!
      sheet
      (+ offset row)
      column
      value)
    [sheet offset row column value]))

(defn- write-form! [writer {:keys [id name modified-by modified-time content] :as form}]
  (when (not-empty form)
    (do
      (writer 0 2 name)
      (writer 1 2 id)
      (writer 2 2 modified-time)
      (writer 3 2 modified-by)))
  3)

(defn- write-headers! [writer applications]
  (doseq [[column label] (zipmap (range) (map :label (:answers (first applications))))]
    (writer 0 column label)))

(defn- write-application! [writer application]
  (doseq [[column value] (zipmap (range) (map :value (:answers application)))]
    (writer 0 column value)))

(defn export-all-applications [form-id & {:keys [language] :or {language :fi}}]
  (with-open [workbook (application-workbook)]
    (do
      (let [form                   (form-store/fetch-form form-id)
            sheet                  (.getSheetAt workbook 0)
            local-row-offset       (write-form! (make-writer sheet 2) form)
            application-row-offset (atom 7)
            applications           (application-store/fetch-applications
                                     form-id
                                     {:limit 100 :lang (name language)})]
        (update-row-cell! sheet 0 2 (t/now)) ; timestamp the excel sheet
        (when (and (not-empty form) (not-empty applications))
          (do
            (write-headers! (make-writer sheet (swap! application-row-offset inc))
                            applications)
            (doseq [application applications]
              (write-application! (make-writer sheet (swap! application-row-offset inc)) application)))))
      (with-open [stream (java.io.ByteArrayOutputStream.)]
        (.write workbook stream)
        (.toByteArray stream)))))


