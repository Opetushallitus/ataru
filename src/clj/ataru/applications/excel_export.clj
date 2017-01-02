(ns ataru.applications.excel-export
  (:import [org.apache.poi.ss.usermodel Row]
           [java.io ByteArrayOutputStream]
           [org.apache.poi.xssf.usermodel XSSFWorkbook]
           [org.joda.time.format DateTimeFormat])
  (:require [ataru.forms.form-store :as form-store]
            [ataru.application.review-states :refer [application-review-states]]
            [ataru.applications.application-store :as application-store]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.util :as util]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.string :as string :refer [trim]]
            [clojure.core.match :refer [match]]
            [clojure.java.io :refer [input-stream]]
            [taoensso.timbre :refer [spy debug]]))

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

(defn state-formatter [state]
  (or (get application-review-states state) "Tuntematon"))

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
    :format-fn time-formatter}
   {:label "Tila"
    :field :state
    :format-fn state-formatter}])

(def ^:private review-headers ["Muistiinpanot" "Pisteet"])

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
      (writer 0 col (formatter value)))))

(defn- write-headers! [writer headers meta-fields]
  (doseq [meta-field meta-fields]
    (writer 0 (:column meta-field) (:label meta-field)))
  (doseq [header headers]
    (writer 0 (+ (:column header) (count meta-fields)) (:decorated-header header))))

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
    (let [column          (:column (first (filter #(= (:label answer) (:header %)) headers)))
          value-or-values (-> (:value answer))
          value           (or
                           (when (or (seq? value-or-values) (vector? value-or-values))
                             (->> value-or-values
                                  (map (partial koodi-uris->human-readable-value form application (:key answer)))
                                  (interpose "\n")
                                  (apply str)))
                           (koodi-uris->human-readable-value form application (:key answer) value-or-values))]
      (writer 0 (+ column (count application-meta-fields)) value)))
  (let [application-review  (application-store/get-application-review (:key application))
        beef-header-count   (- (apply max (map :column headers)) (count review-headers))
        prev-header-count   (+ beef-header-count
                               (count application-meta-fields))
        notes-column        (inc prev-header-count)
        score-column        (inc notes-column)
        notes               (:notes application-review)
        score               (:score application-review)]
    (when notes (writer 0 notes-column notes))
    (when score (writer 0 score-column score))))

(defn pick-form-labels
  [form-content]
  (->> (reduce
         (fn [acc form-element]
           (if (< 0 (count (:children form-element)))
             (into acc (pick-form-labels (:children form-element)))
             (concat acc (when-let [label (and (not= "infoElement" (:fieldClass form-element)))]
                           [[(:id form-element) (-> form-element :label :fi)]]))))
         []
         form-content)))

(defn- find-parent [element fields]
  (let [contains-element? (fn [children] (some? ((set (map :id children)) (:id element))))]
    (reduce
      (fn [parent field]
        (or
          (when (and (= "wrapperElement" (:fieldClass field))
                  (not-empty (:children field))
                  (contains-element? (:children field)))
            field)
          (when (not-empty (:children field))
            (or parent
              (find-parent element (:children field))))
          parent))
      nil
      fields)))

(defn- decorate [flat-fields fields id header]
  (let [element (first (filter #(= (:id %) id) flat-fields))]
    (match element
      {:params {:adjacent true}}
      (if-let [parent-element (find-parent element fields)]
        (str (-> parent-element :label :fi) " - " header)
        header)

      :else header)))

(defn- extract-headers
  [applications form]
  (let [labels-in-form         (pick-form-labels (:content form))
        labels-in-applications (mapcat #(map (fn [answer] (vals (select-keys answer [:key :label]))) (:answers %)) applications)
        all-labels             (distinct (concat labels-in-form labels-in-applications (map vector (repeat nil) review-headers)))
        decorator              (partial decorate (util/flatten-form-fields (:content form)) (:content form))]
    (for [[idx [id header]] (map vector (range) all-labels)
          :when             (string? header)]
      {:decorated-header (decorator id header)
       :header           header
       :column           idx})))

(defn- create-form-meta-sheet [workbook meta-fields]
  (let [sheet  (.createSheet workbook "Lomakkeiden tiedot")
        writer (make-writer sheet 0)]
    (doseq [meta-field meta-fields
            :let [column (:column meta-field)
                  label  (:label meta-field)]]
      (writer 0 column label))
    sheet))

(defn export-applications [applications]
  (let [workbook                (XSSFWorkbook.)
        form-meta-fields        (indexed-meta-fields form-meta-fields)
        form-meta-sheet         (create-form-meta-sheet workbook form-meta-fields)
        application-meta-fields (indexed-meta-fields application-meta-fields)
        get-form-by-id          (memoize form-store/fetch-by-id)
        get-latest-form-by-key  (memoize form-store/fetch-by-key)]
    (->> applications
         (reduce (fn [result {:keys [form] :as application}]
                   (let [form     (get-form-by-id form)
                         form-key (:key form)]
                     (if (contains? result form-key)
                       (update-in result [form-key :applications] conj application)
                       (assoc result form-key {:sheet-idx    (count result)
                                               :sheet-name   (:name (get-latest-form-by-key form-key))
                                               :form         form
                                               :applications [application]}))))
                 {})
         (reduce-kv (fn [workbook _ {:keys [sheet-idx sheet-name form applications]}]
                      (let [applications-sheet      (.createSheet workbook sheet-name)
                            headers                 (extract-headers applications form)
                            meta-writer             (make-writer form-meta-sheet (inc sheet-idx))
                            header-writer           (make-writer applications-sheet 0)]
                        (write-form-meta! meta-writer form applications form-meta-fields)
                        (write-headers! header-writer headers application-meta-fields)
                        (dorun (map-indexed (fn [row-idx application]
                                              (let [row-writer (make-writer applications-sheet (inc row-idx))]
                                                (write-application! row-writer application headers application-meta-fields form)))
                                            applications))
                        (.createFreezePane applications-sheet 0 1 0 1))
                      workbook)
                    workbook))
    (with-open [stream (ByteArrayOutputStream.)]
      (.write workbook stream)
      (.toByteArray stream))))

(defn- sanitize-name [name]
  (-> name
      (string/replace #"[\s]+" "-")
      (string/replace #"[^\w-]+" "")))

(defn filename-by-form
  [form-key]
  {:post [(some? %)]}
  (let [form           (form-store/fetch-by-key form-key)
        sanitized-name (sanitize-name (:name form))
        time           (time-formatter (t/now) filename-time-format)]
    (str sanitized-name "_" form-key "_" time ".xlsx")))

(defn filename-by-hakukohde
  [hakukohde-oid]
  {:post [(some? %)]}
  (when-let [hakukohde-name (->> (application-store/get-hakukohteet)
                                 (filter (comp (partial = hakukohde-oid) :hakukohde))
                                 (map :hakukohde-name)
                                 (first))]
    (let [sanitized-name (sanitize-name hakukohde-name)
          time           (time-formatter (t/now) filename-time-format)]
      (str sanitized-name "_" time ".xlsx"))))
