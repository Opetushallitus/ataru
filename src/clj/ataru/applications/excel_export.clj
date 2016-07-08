(ns ataru.applications.excel-export
  (:import [org.apache.poi.ss.usermodel WorkbookFactory Row]
           [java.io ByteArrayOutputStream]
           (org.apache.poi.xssf.usermodel XSSFWorkbook$SheetIterator XSSFWorkbook))
  (:require [ataru.forms.form-store :as form-store]
            [ataru.applications.application-store :as application-store]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.string :refer [trim]]
            [clojure.core.match :refer [match]]
            [clojure.java.io :refer [input-stream]]
            [taoensso.timbre :refer [spy]]))

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

(defn- write-headers! [writer headers]
  (doseq [header headers]
    (writer 0 (:column header) (:header header))))

(defn- write-application! [writer application headers]
  (doseq [answer (:answers application)]
    (let [column (:column (first (filter #(= (:label answer) (:header %)) headers)))
          value  (:value answer)]
      (writer 0 column value))))

(defn- extract-headers
  [applications]
  (->> (reduce
         (fn [form-headers application]
           (into form-headers
             (reduce
               (fn [application-headers answer]
                 (conj application-headers (:label answer)))
               []
               (:answers application))))
         []
         applications)
       distinct
       (map-indexed (fn [idx header] {:header header :column idx}))))

(defn export-all-applications [form-id & {:keys [language] :or {language :fi}}]
      (let [workbook               (XSSFWorkbook.)
            form                   (form-store/fetch-form form-id)
            sheet                  (.createSheet workbook "Hakemukset")
            application-row-offset (atom -1)
            applications           (application-store/fetch-applications
                                     form-id
                                     {:limit 100 :lang (name language)})
            headers                (extract-headers applications)]
        (when (and (not-empty form) (not-empty applications))
          (do
            (write-headers! (make-writer sheet (swap! application-row-offset inc))
                            headers)
            (doseq [application applications]
              (write-application! (make-writer sheet (swap! application-row-offset inc)) application headers))))
      (with-open [stream (ByteArrayOutputStream.)]
        (.write workbook stream)
        (.toByteArray stream))))


