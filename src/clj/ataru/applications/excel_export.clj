(ns ataru.applications.excel-export
  (:import [org.apache.poi.ss.usermodel Row]
           [java.io ByteArrayOutputStream]
           [org.apache.poi.xssf.usermodel XSSFWorkbook]
           [org.joda.time.format DateTimeFormat])
  (:require [ataru.forms.form-store :as form-store]
            [ataru.applications.application-store :as application-store]
            [ataru.koodisto.koodisto :as koodisto]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.string :as string :refer [trim]]
            [clojure.core.match :refer [match]]
            [clojure.java.io :refer [input-stream]]
            [taoensso.timbre :refer [spy]]))

(def tz (t/default-time-zone))

(def ^:private modified-time-format
  (f/formatter "yyyy-MM-dd HH:mm:ss" tz))

(def ^:private filename-time-format
  (f/formatter "yyyy-MM-dd_HHmm" tz))

(defn time-formatter
  ([date-time formatter]
   (f/unparse formatter date-time))
  ([date-time]
    (time-formatter date-time modified-time-format)))

(def ^:private form-meta-fields
  [{:label "Nimi"
    :field :name}
   {:label "Id"
    :field :id}
   {:label "Tunniste"
    :field :key}
   {:label "Viimeksi muokattu"
    :field :created-time
    :format-fn time-formatter}
   {:label "Viimeinen muokkaaja"
    :field :created-by}])

(def ^:private hakukohde-form-meta-fields
  (into form-meta-fields
        [{:label "Hakukohde"
          :field :hakukohde-name
          :from  :applications}
         {:label "Hakukohteen OID"
          :field :hakukohde
          :from  :applications}]))

(def ^:private application-meta-fields
  [{:label "Id"
    :field :key}
   {:label "Lähetysaika"
    :field :created-time
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
  [writer form applications fields]
  (doseq [meta-field fields]
    (let [col        (:column meta-field)
          value-from (case (:from meta-field)
                       :applications (first applications)
                       form)
          value      ((:field meta-field) value-from)
          formatter  (or (:format-fn meta-field) identity)]
      (writer 0 col (:label meta-field))
      (writer 1 col (formatter value)))))

(defn- write-headers! [writer headers meta-fields]
  (doseq [meta-field meta-fields]
    (writer 0 (:column meta-field) (:label meta-field)))
  (doseq [header headers]
    (writer 0 (+ (:column header) (count meta-fields)) (:header header))))

(defn- get-field-descriptor [field-descriptors key]
  (loop [field-descriptors field-descriptors]
    (if-let [field-descriptor (first field-descriptors)]
      (let [ret (if (contains? field-descriptor :children)
                  (get-field-descriptor (:children field-descriptor) key)
                  field-descriptor)]
        (if (= key (:id ret))
          ret
          (recur (next field-descriptors)))))))

(defn- get-label [koodisto lang koodi-uri]
  (let [koodi (->> koodisto
                   (filter (fn [{:keys [value]}]
                             (= value koodi-uri)))
                   first)]
    (get-in koodi [:label lang])))

(defn- koodi-uris->human-readable-value [{:keys [content]} {:keys [lang]} key value]
  (let [field-descriptor (get-field-descriptor content key)
        lang             (-> lang clojure.string/lower-case keyword)]
    (if-some [koodisto-source (:koodisto-source field-descriptor)]
      (let [koodisto         (koodisto/get-koodisto-options (:uri koodisto-source) (:version koodisto-source))
            koodi-uri->label (partial get-label koodisto lang)]
        (->> (clojure.string/split value #"\s*,\s*")
             (mapv koodi-uri->label)
             (interpose "\n")
             (apply str)))
      value)))

(defn- write-application! [writer application headers application-meta-fields form]
  (doseq [meta-field application-meta-fields]
    (let [meta-value ((or (:format-fn meta-field) identity) ((:field meta-field) application))]
      (writer 0 (:column meta-field) meta-value)))
  (doseq [answer (:answers application)]
    (let [column (:column (first (filter #(= (:label answer) (:header %)) headers)))
          value-or-values (-> (:value answer))
          value (or
                  (when (or (seq? value-or-values) (vector? value-or-values))
                    (->> value-or-values
                         (map (partial koodi-uris->human-readable-value form application (:key answer)))
                         (interpose "\n")
                         (apply str)))
                  (koodi-uris->human-readable-value form application (:key answer) value-or-values))]
      (writer 0 (+ column (count application-meta-fields)) value)))
  (let [application-review (application-store/get-application-review (:key application))]
    (when-let [notes (:notes application-review)]
      (let [column (+ (apply max (map :column headers))
                      (count application-meta-fields))]
        (writer 0 column notes)))))

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
        all-labels (distinct (concat labels-in-form labels-in-applications ["Muistiinpanot"]))]
    (map-indexed (fn [idx header]
                   {:header header :column idx})
                 all-labels)))

(defn- export-applications
  [applications form-key indexed-form-meta-fields]
  (let [workbook                (XSSFWorkbook.)
        form                    (form-store/fetch-by-key form-key)
        form-meta-sheet         (.createSheet workbook "Lomakkeen tiedot")
        applications-sheet      (.createSheet workbook "Hakemukset")
        application-meta-fields (indexed-meta-fields application-meta-fields)
        headers                 (extract-headers applications form)]
    (when (not-empty form)
      (write-form-meta! (make-writer form-meta-sheet 0) form applications indexed-form-meta-fields)
      (write-headers! (make-writer applications-sheet 0) headers application-meta-fields)
      (dorun (map-indexed
               (fn [idx application]
                 (let [writer (make-writer applications-sheet (inc idx))]
                   (write-application! writer application headers application-meta-fields form)))
               applications))
      (.createFreezePane applications-sheet 0 1 0 1))
    (with-open [stream (ByteArrayOutputStream.)]
      (.write workbook stream)
      (.toByteArray stream))))

(defn export-all-form-applications
  [form-key]
  (let [applications (application-store/get-applications-for-form form-key {})
        meta-fields  (indexed-meta-fields form-meta-fields)]
    (export-applications applications form-key meta-fields)))

(defn export-all-hakukohde-applications
  [form-key hakukohde-oid]
  (let [applications (application-store/get-applications-for-hakukohde form-key hakukohde-oid)
        meta-fields  (indexed-meta-fields hakukohde-form-meta-fields)]
    (export-applications applications form-key meta-fields)))

(defn filename
  ([form-key hakukohde-oid]
   (let [form           (form-store/fetch-by-key form-key)
         sanitized-name (-> (or (:name form))
                            (string/replace #"[\s]+" "-")
                            (string/replace #"[^\w-]+" ""))
         time           (time-formatter (t/now) filename-time-format)]
     (str sanitized-name "_" form-key "_" (if hakukohde-oid (str hakukohde-oid "_") "") time ".xlsx")))
  ([form-key]
    (filename form-key nil)))



