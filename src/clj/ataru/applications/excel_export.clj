(ns ataru.applications.excel-export
  (:import [org.apache.poi.ss.usermodel WorkbookFactory])
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

(defn update-row-cell! [sheet row column value]
  (when-let [v (not-empty (trim (str value)))]
    (-> (.getRow sheet row)
        (.getCell column)
        (.setCellValue v)))
  sheet)

(defn- make-writer [sheet offset]
  (let [local-offset (atom (dec offset))]
    (fn [column value]
      (do
        (update-row-cell!
          sheet
          (swap! local-offset inc)
          column
          value)
        @local-offset))))

(defn- write-form! [writer form]
  (when (not-empty form)
    (do
      (writer 1 "foo"))))

(defn- write-application! [writer application]
  (writer 1 "bar"))

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


