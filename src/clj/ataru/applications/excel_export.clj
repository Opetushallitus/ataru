(ns ataru.applications.excel-export
  (:import [org.apache.poi.ss.usermodel Row VerticalAlignment Row$MissingCellPolicy]
           [java.io ByteArrayOutputStream]
           [org.apache.poi.xssf.usermodel XSSFWorkbook XSSFCell XSSFCellStyle])
  (:require [ataru.forms.form-store :as form-store]
            [ataru.util.language-label :as label]
            [ataru.applications.application-store :as application-store]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.files.file-store :as file-store]
            [ataru.util :as util]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.string :as string :refer [trim]]
            [clojure.core.match :refer [match]]
            [clojure.java.io :refer [input-stream]]
            [taoensso.timbre :refer [spy debug]]
            [ataru.application.review-states :as review-states]
            [ataru.application.application-states :as application-states]))

(def max-value-length 5000)

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

(defn application-state-formatter
  [state]
  (or
    (->> review-states/application-review-states
         (filter #(= (first %) state)) first second)
    "Tuntematon"))

(defn hakukohde-review-formatter
  [requirement-name application]
  (let [reviews     (filter
                      #(= (:requirement %) requirement-name)
                      (:application-hakukohde-reviews application))
        requirement (last
                      (first
                        (filter #(= (first %) (keyword requirement-name)) review-states/hakukohde-review-types)))
        get-label   (partial application-states/get-review-state-label-by-name requirement)]
    (if (= 1 (count reviews))
      (get-label (-> (first reviews) :state))
      (clojure.string/join
        "\n"
        (map
          (fn [{:keys [hakukohde hakukohde-name state]}]
            (str hakukohde-name
                 " (" hakukohde "): "
                 (get-label state)))
          reviews)))))

(def ^:private form-meta-fields
  [{:label "Nimi"
    :field :name
    :format-fn #(some (partial get %) [:fi :sv :en])}
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
   {:label     "Hakemuksen tila"
    :field     :state
    :format-fn application-state-formatter}
   {:label     "Hakukohteen käsittelyn tila"
    :format-fn (partial hakukohde-review-formatter "processing-state")}
   {:label     "Kielitaitovaatimus"
    :format-fn (partial hakukohde-review-formatter "language-requirement")}
   {:label     "Tutkinnon kelpoisuus"
    :format-fn (partial hakukohde-review-formatter "degree-requirement")}
   {:label     "Hakukelpoisuus"
    :format-fn (partial hakukohde-review-formatter "eligibility-state")}
   {:label     "Maksuvelvollisuus"
    :format-fn (partial hakukohde-review-formatter "payment-obligation")}
   {:label     "Valinnan tila"
    :format-fn (partial hakukohde-review-formatter "selection-state")}
   {:label     "Hakijan henkilö-OID"
    :field     :person-oid
    :format-fn str}])

(def ^:private review-headers ["Muistiinpanot" "Pisteet"])

(def cell-style (atom nil))

(def cell-style-quote-prefixed (atom nil))

(defn- create-cell-styles!
  [workbook]
  (reset! cell-style
          (doto (.createCellStyle workbook)
            (.setWrapText true)
            (.setVerticalAlignment VerticalAlignment/TOP)))
  (reset! cell-style-quote-prefixed
          (doto (.createCellStyle workbook)
            (.setQuotePrefixed true)
            (.setWrapText true)
            (.setVerticalAlignment VerticalAlignment/TOP))))

(defn- create-workbook-and-styles!
  []
  (let [workbook (XSSFWorkbook.)]
    (create-cell-styles! workbook)
    workbook))

(defn- indexed-meta-fields
  [fields]
  (map-indexed (fn [idx field] (merge field {:column idx})) fields))

(defn- set-cell-style [cell value]
  (if (and (string? value)
           (contains? #{\= \+ \- \@} (first value)))
    (.setCellStyle cell @cell-style-quote-prefixed)
    (.setCellStyle cell @cell-style))
  cell)

(defn- update-row-cell! [sheet row column value workbook]
  (when-let [v (not-empty (trim (str value)))]
    (-> (or (.getRow sheet row)
            (.createRow sheet row))
        (.getCell column Row$MissingCellPolicy/CREATE_NULL_AS_BLANK)
        (set-cell-style value)
        (.setCellValue v)))
  sheet)

(defn- make-writer [sheet row-offset workbook]
  (fn [row column value]
    (update-row-cell!
      sheet
      (+ row-offset row)
      column
      value workbook)
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

(defn- get-label [koodisto lang koodi-uri]
  (let [koodi (->> koodisto
                   (filter (fn [{:keys [value]}]
                             (= value koodi-uri)))
                   first)]
    (get-in koodi [:label lang])))

(defn- raw-values->human-readable-value [{:keys [content]} {:keys [lang]} key value]
  (let [field-descriptor (->> (util/flatten-form-fields content)
                              (filter #(= key (:id %)))
                              first)
        lang (-> lang clojure.string/lower-case keyword)
        koodisto-source (:koodisto-source field-descriptor)
        options (:options field-descriptor)]
    (cond (some? koodisto-source)
          (let [koodisto (koodisto/get-koodisto-options (:uri koodisto-source) (:version koodisto-source))
                koodi-uri->label (partial get-label koodisto lang)]
            (->> (clojure.string/split value #"\s*,\s*")
                 (mapv koodi-uri->label)
                 (interpose ",\n")
                 (apply str)))
          (= (:fieldType field-descriptor) "attachment")
          (let [[{:keys [filename size]}] (file-store/get-metadata [value])]
            (when (and filename size)
              (str filename " (" (util/size-bytes->str size) ")")))
          (not (empty? options))
          (some (fn [option]
                  (when (= value (:value option))
                    (get-in option [:label lang] value)))
                options)
          :else
          value)))

(defn- all-answers-sec-or-vec? [answers]
  (every? sequential? answers))

(defn- kysymysryhma-answer? [value-or-values]
  (and (sequential? value-or-values)
       (all-answers-sec-or-vec? value-or-values)))

(defn- write-application! [writer application headers application-meta-fields form]
  (doseq [meta-field application-meta-fields]
    (let [meta-value ((or
                        (:format-fn meta-field)
                        identity)
                       ((or (:field meta-field)
                            identity)
                         application))]
      (writer 0 (:column meta-field) meta-value)))
  (doseq [answer (:answers application)]
    (let [column          (:column (first (filter #(= (:key answer) (:id %)) headers)))
          value-or-values (:value answer)
          value           (cond
                            (kysymysryhma-answer? value-or-values)
                            (->> value-or-values
                                 (map #(clojure.string/join "," %))
                                 (map (partial raw-values->human-readable-value form application (:key answer)))
                                 (map-indexed #(format "#%s: %s,\n" %1 %2))
                                 (apply str))

                            (sequential? value-or-values)
                            (->> value-or-values
                                 (map (partial raw-values->human-readable-value form application (:key answer)))
                                 (interpose ",\n")
                                 (apply str))

                            :else
                            (raw-values->human-readable-value form application (:key answer) value-or-values))
          value-length    (count value)
          value-truncated (if (< max-value-length value-length)
                            (str
                              (subs value 0 (- max-value-length 100))
                              "—— [ vastaus liian pitkä Excel-vientiin, poistettu "
                              (- value-length max-value-length -100) " merkkiä]")
                            value)]
      (when (and value-truncated column)
        (writer 0 (+ column (count application-meta-fields)) value-truncated))))
  (let [application-key              (:key application)
        application-review (application-store/get-application-review application-key)
        beef-header-count  (- (apply max (map :column headers)) (count review-headers))
        prev-header-count  (+ beef-header-count
                              (count application-meta-fields))
        notes-column       (inc prev-header-count)
        score-column       (inc notes-column)
        notes              (:notes application-review)
        score              (:score application-review)]
    (when (not-empty notes)
      (->> notes
           (map :notes)
           (clojure.string/join "\n")
           (writer 0 notes-column)))
    (when score (writer 0 score-column score))))

(defn- form-label? [form-element]
  (and (not= "infoElement" (:fieldClass form-element))
       (not (:exclude-from-answers form-element))))

(defn- hidden-answer? [form-element]
  (:exclude-from-answers form-element))

(defn- pick-label
  [form-element pick-cond]
  (when (pick-cond form-element)
    [[(:id form-element)
      (label/get-language-label-in-preferred-order (:label form-element))]]))

(defn pick-form-labels
  [form-content pick-cond]
  (reduce
   (fn [acc form-element]
     (let [followups (remove nil? (mapcat :followups (:options form-element)))]
       (cond
         (pos? (count (:children form-element)))
         (into acc (pick-form-labels (:children form-element) pick-cond))

         (pos? (count followups))
         (into (into acc (pick-label form-element pick-cond)) (pick-form-labels followups pick-cond))

         :else
         (into acc (pick-label form-element pick-cond)))))
   []
   form-content))

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
      {:fieldType "attachment"}
      (str "Liitepyyntö: " (label/get-language-label-in-preferred-order (:label element)))
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

(defn- remove-duplicates-by-field-id
  [labels-in-form labels-in-applications]
  (let [form-element-ids (set (map first labels-in-form))]
    (remove (fn [[key _]]
              (contains? form-element-ids key))
            labels-in-applications)))

(defn- extract-headers
  [applications form]
  (let [labels-in-form              (pick-form-labels (:content form) form-label?)
        labels-in-applications      (extract-headers-from-applications applications form)
        labels-only-in-applications (remove-duplicates-by-field-id labels-in-form labels-in-applications)
        all-labels                  (distinct (concat labels-in-form labels-only-in-applications (map vector (repeat nil) review-headers)))
        decorator                   (partial decorate (util/flatten-form-fields (:content form)) (:content form))]
    (for [[idx [id header]] (map vector (range) all-labels)
          :when (string? header)]
      {:id               id
       :decorated-header (decorator id header)
       :header           header
       :column           idx})))

(defn- create-form-meta-sheet [workbook meta-fields]
  (let [sheet  (.createSheet workbook "Lomakkeiden tiedot")
        writer (make-writer sheet 0 workbook)]
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

(defn- inject-haku-info
  [tarjonta-service ohjausparametrit-service application]
  (merge application
         (tarjonta-parser/parse-tarjonta-info-by-haku tarjonta-service ohjausparametrit-service (:haku application))))

(defn set-column-widths [workbook]
  (doseq [n (range (.getNumberOfSheets workbook))
          :let [sheet (.getSheetAt workbook n)]
          y (range (.getLastCellNum (.getRow sheet 0)))]
    (.autoSizeColumn sheet (short y))))

(defn- update-hakukohteet-for-legacy-applications [application]
  (let [hakukohteet (-> application :answers :hakukohteet)
        hakukohde   (:hakukohde application)]
    (if (or hakukohteet
            (and (not hakukohteet) (not hakukohde)))
      application
      (update application :answers conj
        {:key "hakukohteet" :fieldType "hakukohteet" :value (:hakukohde application) :label "Hakukohteet"}))))

(defn- get-hakukohde-name [tarjonta-service lang-s oid]
  (let [lang (keyword lang-s)]
    (when-let [hakukohde (tarjonta-parser/parse-hakukohde
                          tarjonta-service
                          (tarjonta/get-hakukohde tarjonta-service oid))]
      (str (get-in hakukohde [:name lang]) " - "
           (get-in hakukohde [:tarjoaja-name lang])))))

(defn- add-hakukohde-name [tarjonta-service lang hakukohde-answer haku-oid]
  (let [use-priority (some->> haku-oid
                              (tarjonta/get-haku tarjonta-service)
                              :usePriority)]
    (update hakukohde-answer :value
            (partial map-indexed (fn [index oid]
                                   (let [name           (get-hakukohde-name tarjonta-service lang oid)
                                         priority-index (when use-priority
                                                          (str "(" (inc index) ") "))]
                                     (if name
                                       (str priority-index name " (" oid ")")
                                       (str priority-index oid))))))))

(defn- add-hakukohde-names [tarjonta-service application]
  (update application :answers
          (partial map (fn [answer]
                         (if (= "hakukohteet" (:key answer))
                           (add-hakukohde-name tarjonta-service (:lang application) answer (:haku application))
                           answer)))))

(defn- add-all-hakukohde-reviews
  [tarjonta-service selected-hakukohde application]
  (let [all-reviews            (application-states/get-all-reviews-for-all-requirements
                                 application
                                 selected-hakukohde)
        all-reviews-with-names (map
                                 (fn [{:keys [hakukohde] :as review}]
                                   (assoc review
                                     :hakukohde-name
                                     (get-hakukohde-name
                                       tarjonta-service
                                       (:lang application)
                                       hakukohde)))
                                 all-reviews)]
    (assoc application :application-hakukohde-reviews all-reviews-with-names)))

(defn export-applications [applications selected-hakukohde tarjonta-service ohjausparametrit-service]
  (let [workbook                (create-workbook-and-styles!)
        form-meta-fields        (indexed-meta-fields form-meta-fields)
        form-meta-sheet         (create-form-meta-sheet workbook form-meta-fields)
        application-meta-fields (indexed-meta-fields application-meta-fields)
        get-form-by-id          (memoize form-store/fetch-by-id)
        get-latest-form-by-key  (memoize form-store/fetch-by-key)]
    (->> applications
         (map update-hakukohteet-for-legacy-applications)
         (map (partial add-hakukohde-names tarjonta-service))
         (map (partial add-all-hakukohde-reviews tarjonta-service selected-hakukohde))
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
                              meta-writer        (make-writer form-meta-sheet (inc sheet-idx) workbook)
                              header-writer      (make-writer applications-sheet 0 workbook)]
                          (write-form-meta! meta-writer form applications form-meta-fields)
                          (write-headers! header-writer headers application-meta-fields)
                          (->> applications
                               (sort-by :created-time)
                               (reverse)
                               (map (partial inject-haku-info tarjonta-service ohjausparametrit-service))
                               (map-indexed (fn [row-idx application]
                                              (let [row-writer (make-writer applications-sheet (inc row-idx) workbook)]
                                                (write-application! row-writer application headers application-meta-fields form))))
                               (dorun))
                          (.createFreezePane applications-sheet 0 1 0 1))))
         (dorun))
    (when (< (count applications) 1000)
      ; turns out .autoSizeColumn is a performance killer for large sheets
      (set-column-widths workbook))
    (with-open [stream (ByteArrayOutputStream.)]
      (.write workbook stream)
      (.toByteArray stream))))

(defn- sanitize-name [name]
  (-> name
      (string/replace #"[\s]+" "-")
      (string/replace #"[^\w-]+" "")))

(defn create-filename [identifying-part]
  {:pre [(some? identifying-part)]}
  (str
   (sanitize-name identifying-part)
   "_"
   (time-formatter (t/now) filename-time-format)
   ".xlsx"))
