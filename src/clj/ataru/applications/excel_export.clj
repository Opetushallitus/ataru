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

(defn- write-form! [writer form]
  (when (not-empty form)
    (do
      (writer 0 1 "foo")
      (writer 0 2 "column")
      (writer 1 1 "bar")
      (writer 1 2 "column")))
  1)

(defn- write-application! [writer application]
  (do
    (writer 0 1 "bar")
    1))

(defn export-all-applications [form-id & {:keys [language] :or {language :fi}}]
  (with-open [workbook (application-workbook)]
    (do
      (let [form             (form-store/fetch-form form-id)
            sheet            (.getSheetAt workbook 0)
            local-row-offset (write-form! (make-writer sheet row-offset) form)
            application-row-offset (atom local-row-offset)]
        (doseq [application (and (not-empty form)
                                 (application-store/fetch-applications
                                   form-id
                                   {:limit 1 :lang language}))
                :let        []]
          (reset! application-row-offset
                  (write-application! (make-writer sheet @application-row-offset) application))))
      (with-open [stream (java.io.ByteArrayOutputStream.)]
        (.write workbook stream)
        (.getBytes stream)))))


