(ns ataru.applications.excel-export
  (:import [org.apache.poi.ss.usermodel WorkbookFactory])
  (:require [ataru.forms.form-store :as form-store]
            [ataru.applications.application-store :as application-store]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.core.match :refer [match]]
            [clojure.java.io :refer [input-stream]]
            [taoensso.timbre :refer [spy]]))

(defn- application-workbook []
  (WorkbookFactory/create
    (input-stream
      "resources/templates/application-export-template.xlsx")))

(defn update-row-cell! [sheet row column value]
  (when-let [v (not-empty (trim (str value)))]
    (-> (.getRow sheet row)
        (.getCell column)
        (.setCellValue value)))
  sheet)

(defn export-all-applications [form-id & {:keys [language] :or {:language :fi}}]
  (let [applications (application-store/fetch-applications form-id {:limit 1000})
        form (form-store/fetch-form form-id)
        wb (application-workbook)]))

;(count (store/fetch-applications 703 {:limit 1}))


