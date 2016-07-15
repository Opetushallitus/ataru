(ns ataru.applications.excel-export
  (:import [org.apache.poi.ss.usermodel Row]
           [java.io ByteArrayOutputStream]
           [org.apache.poi.xssf.usermodel XSSFWorkbook]
           [org.joda.time.format DateTimeFormat])
  (:require [ataru.forms.form-store :as form-store]
            [ataru.applications.application-store :as application-store]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.string :as string :refer [trim]]
            [clojure.core.match :refer [match]]
            [clojure.java.io :refer [input-stream]]
            [taoensso.timbre :refer [spy]]))

(def ^:private modified-time-format
  (f/formatter "yyyy-MM-dd HH:mm:ss"))

(def ^:private filename-time-format
  (f/formatter "yyyy-MM-dd_HHmm"))

(defn time-formatter
  [date-time]
  (f/unparse modified-time-format date-time))

(def ^:private form-meta-fields
  [{:label "Nimi"
    :field :name}
   {:label "Id"
    :field :id}
   {:label "Viimeksi muokattu"
    :field :modified-time
    :format-fn time-formatter}
   {:label "Viimeinen muokkaaja"
    :field :modified-by}])

(def ^:private application-meta-fields
  [{:label "Id"
    :field :key}
   {:label "Lähetysaika"
    :field :modified-time
    :format-fn time-formatter}])

(defn- indexed-meta-fields
  [fields]
  (map-indexed (fn [idx field] (merge field {:column idx})) fields))

(defn- update-row-cell! [sheet row column value]
  (when-let [v (not-empty (trim (str value)))]
    (-> (or (.getRow sheet row)
            (.createRow sheet row))
        (.getCell column Row/CREATE_NULL_AS_BLANK)
        (.setCellValue v)))
  sheet)

(defn- make-writer [sheet row-offset]
  (fn [row column value]
    (update-row-cell!
      sheet
      (+ row-offset row)
      column
      value)
    [sheet row-offset row column value]))

(defn- write-form-meta!
  [writer form fields]
  (doseq [meta-field fields]
    (let [col (:column meta-field)
          value ((:field meta-field) form)
          formatter (or (:format-fn meta-field) identity)]
      (writer 0 col (:label meta-field))
      (writer 1 col (formatter value)))))

(defn- write-headers! [writer headers meta-fields]
  (doseq [meta-field meta-fields]
    (writer 0 (:column meta-field) (:label meta-field)))
  (doseq [header headers]
    (writer 0 (+ (:column header) (count meta-fields)) (:header header))))

(defn- write-application! [writer application headers application-meta-fields]
  (doseq [meta-field application-meta-fields]
    (let [meta-value ((or (:format-fn meta-field) identity) ((:field meta-field) application))]
      (writer 0 (:column meta-field) meta-value)))
  (doseq [answer (:answers application)]
    (let [column (:column (first (filter #(= (:label answer) (:header %)) headers)))
          value (:value answer)]
      (writer 0 (+ column (count application-meta-fields)) value))))

(defn pick-form-labels
  [form-content]
  (flatten
    (reduce
      (fn [acc form-element]
        (if (< 0 (count (:children form-element)))
          (into acc [(pick-form-labels (:children form-element))])
          (into acc [(-> form-element :label :fi)])))
      []
      form-content)))

(defn- extract-headers
  [applications form]
  (let [labels-in-form (pick-form-labels (:content form))
        labels-in-applications (mapcat #(map :label (:answers %)) applications)
        all-labels (distinct (concat labels-in-form labels-in-applications))]
    (map-indexed (fn [idx header]
                   {:header header :column idx})
                 all-labels)))

(defn export-all-applications [form-id & {:keys [language] :or {language :fi}}]
  (let [workbook (XSSFWorkbook.)
        form (form-store/fetch-form form-id)
        form-meta-sheet (.createSheet workbook "Lomakkeen tiedot")
        applications-sheet (.createSheet workbook "Hakemukset")
        applications (application-store/fetch-applications
                       form-id
                       {:lang (name language)})
        application-meta-fields (indexed-meta-fields application-meta-fields)
        headers (extract-headers applications form)]
    (when (not-empty form)
      (write-form-meta! (make-writer form-meta-sheet 0) form (indexed-meta-fields form-meta-fields))
      (write-headers! (make-writer applications-sheet 0) headers application-meta-fields)
      (dorun (map-indexed
               (fn [idx application]
                 (let [writer (make-writer applications-sheet (inc idx))]
                   (write-application! writer application headers application-meta-fields)))
               applications))
      (.createFreezePane applications-sheet 0 1 0 1))
    (with-open [stream (ByteArrayOutputStream.)]
      (.write workbook stream)
      (.toByteArray stream))))

(defn filename
  [form-id]
  (let [form (form-store/fetch-form form-id)
        sanitized-name (-> (:name form)
                           (string/replace #"[\s]+" "-")
                           (string/replace #"[^\w-]+" ""))
        time (f/unparse filename-time-format (:last-modified form))]
    (str form-id "_" sanitized-name "_" time ".xlsx")))


