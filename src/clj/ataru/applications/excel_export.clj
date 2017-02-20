(ns ataru.applications.excel-export
  (:import [org.apache.poi.ss.usermodel Row]
           [java.io ByteArrayOutputStream]
           [org.apache.poi.xssf.usermodel XSSFWorkbook])
  (:require [ataru.forms.form-store :as form-store]
            [ataru.util.language-label :as label]
            [ataru.application.review-states :refer [application-review-states]]
            [ataru.applications.application-store :as application-store]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.util :as util]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.string :as string :refer [trim]]
            [clojure.core.match :refer [match]]
            [clojure.java.io :refer [input-stream]]
            [taoensso.timbre :refer [spy debug]]
            [ataru.hakukohde.hakukohde-access-control :as hakukohde-access-control]
            [ataru.haku.haku-access-control :as haku-access-control]))

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

(def ^:private application-meta-fields
  [{:label "Id"
    :field :key}
   {:label     "Lähetysaika"
    :field     :created-time
    :format-fn time-formatter}
   {:label     "Tila"
    :field     :state
    :format-fn state-formatter}
   {:label     "Hakukohde"
    :field     :hakukohde-name
    :format-fn str}
   {:label     "Hakukohteen OID"
    :field     :hakukohde
    :format-fn str}
   {:label     "Koulutuskoodin nimi ja tunniste"
    :field     :koulutus-identifiers
    :format-fn (partial string/join "; ")}])

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
  (doseq [answer (:answers application)
          :when (some (comp (partial = (:label answer)) :header) headers)]
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

(defn- form-label? [form-element]
  (and (not= "infoElement" (:fieldClass form-element))
       (not (:exclude-from-answers form-element))))

(defn- hidden-answer? [form-element]
  (:exclude-from-answers form-element))

(defn pick-form-labels
  [form-content pick-cond]
  (->> (reduce
         (fn [acc form-element]
           (if (< 0 (count (:children form-element)))
             (into acc (pick-form-labels (:children form-element) pick-cond))
             (into acc (when (pick-cond form-element)
                         [[(:id form-element)
                           (label/get-language-label-in-preferred-order (:label form-element))]]))))
         []
         form-content)))

(defn- find-parent [element fields]
  (let [contains-element? (fn [children] (some? ((set (map :id children)) (:id element))))
        followup-dropdown (fn [field] (mapcat :followups (:options field)))]
    (reduce
      (fn [parent field]
        (or parent
          (match field
            ((_ :guard contains-element?) :<< :children) field

            ((followups :guard not-empty) :<< followup-dropdown)
            (or
              (when (contains-element? followups)
                field)
              (find-parent element followups))

            ((children :guard not-empty) :<< :children)
            (find-parent element children)

            :else nil)))
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

(defn- extract-headers-from-applications [applications form]
  (let [hidden-answers (map first (pick-form-labels (:content form) hidden-answer?))]
    (mapcat (fn [application]
              (->> (:answers application)
                   (filter (fn [answer]
                             (not (some (partial = (:key answer)) hidden-answers))))
                   (mapv (fn [answer]
                           (vals (select-keys answer [:key :label]))))))
            applications)))

(defn- extract-headers
  [applications form]
  (let [labels-in-form         (pick-form-labels (:content form) form-label?)
        labels-in-applications (extract-headers-from-applications applications form)
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

(def ^:private invalid-char-matcher #"[\\/\*\[\]:\?]")

(defn- sheet-name [{:keys [id name]}]
  {:pre [(some? id)
         (some? name)]}
  (let [name (str id "_" (clojure.string/replace name invalid-char-matcher "_"))]
    (cond-> name
      (> (count name) 30)
      (subs 0 30))))

(defn- inject-hakukohde-name
  [tarjonta-service application]
  (if-let [hakukohde-oid (:hakukohde application)]
    (merge application {:hakukohde-name (-> (.get-hakukohde tarjonta-service hakukohde-oid) :hakukohteenNimet :kieli_fi)})
    application))

(defn- inject-koulutus-information
  [tarjonta-service application]
  (if-let [hakukohde-oid (:hakukohde application)]
    (let [hakukohde            (.get-hakukohde tarjonta-service hakukohde-oid)
          koulutus-oids        (map :oid (:koulutukset hakukohde))
          koulutus-identifiers (when koulutus-oids
                                 (->> koulutus-oids
                                      (map #(.get-koulutus tarjonta-service %))
                                      (map (fn [koulutus]
                                             (string/join
                                              ", "
                                              (remove string/blank?
                                                      [(-> koulutus :koulutuskoodi :nimi)
                                                       (-> koulutus :tutkintonimike :nimi)
                                                       (-> koulutus :tarkenne)]))))))]
      (if koulutus-identifiers
        (merge application {:koulutus-identifiers koulutus-identifiers})
        application))
    application))

(defn export-applications [applications tarjonta-service]
  (let [workbook                (XSSFWorkbook.)
        form-meta-fields        (indexed-meta-fields form-meta-fields)
        form-meta-sheet         (create-form-meta-sheet workbook form-meta-fields)
        application-meta-fields (indexed-meta-fields application-meta-fields)
        get-form-by-id          (memoize form-store/fetch-by-id)
        get-latest-form-by-key  (memoize form-store/fetch-by-key)]
    (->> applications
         (reduce (fn [result {:keys [form] :as application}]
                   (let [form-key (:key (get-form-by-id form))
                         form     (get-latest-form-by-key form-key)]
                     (if (contains? result form-key)
                       (update-in result [form-key :applications] conj application)
                       (let [value {:sheet-name   (sheet-name form)
                                    :form         form
                                    :applications [application]}]
                         (assoc result form-key value)))))
                 {})
         (map second)
         (map-indexed (fn [sheet-idx {:keys [sheet-name form applications]}]
                        (let [applications-sheet (.createSheet workbook sheet-name)
                              headers            (extract-headers applications form)
                              meta-writer        (make-writer form-meta-sheet (inc sheet-idx))
                              header-writer      (make-writer applications-sheet 0)]
                          (write-form-meta! meta-writer form applications form-meta-fields)
                          (write-headers! header-writer headers application-meta-fields)
                          (->> applications
                               (sort-by :created-time)
                               (reverse)
                               (map (partial inject-hakukohde-name tarjonta-service))
                               (map (partial inject-koulutus-information tarjonta-service))
                               (map-indexed (fn [row-idx application]
                                              (let [row-writer (make-writer applications-sheet (inc row-idx))]
                                                (write-application! row-writer application headers application-meta-fields form))))
                               (dorun))
                          (.createFreezePane applications-sheet 0 1 0 1))))
         (dorun))
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
  [hakukohde-oid session organization-service tarjonta-service]
  {:post [(some? %)]}
  (when-let [hakukohde-name (->> (hakukohde-access-control/get-hakukohteet session organization-service tarjonta-service)
                                 (filter (comp (partial = hakukohde-oid) :hakukohde))
                                 (map :hakukohde-name)
                                 (first))]
    (let [sanitized-name (sanitize-name hakukohde-name)
          time           (time-formatter (t/now) filename-time-format)]
      (str sanitized-name "_" time ".xlsx"))))

(defn filename-by-haku
  [haku-oid session organization-service tarjonta-service]
  {:post [(some? %)]}
  (when-let [hakukohde-name (->> (haku-access-control/get-haut session organization-service tarjonta-service)
                                 (filter (comp (partial = haku-oid) :haku))
                                 (map :haku-name)
                                 (first))]
    (let [sanitized-name (sanitize-name hakukohde-name)
          time           (time-formatter (t/now) filename-time-format)]
      (str sanitized-name "_" time ".xlsx"))))
