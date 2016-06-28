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

(defn- write-form! [wb form]
  )

(defn- write-application! [wb application]
  )

(defn export-all-applications [form-id & {:keys [language] :or {language :fi}}]
  (with-open [wb (application-workbook)]
    (let [form (form-store/fetch-form form-id)]
      (write-form! wb form)
      (when-let [applications (and (not-empty form)
                                   (application-store/fetch-applications
                                     form-id
                                     {:limit 1 :lang language}))]
        (doseq [application applications]
          (write-application! wb application))))))


