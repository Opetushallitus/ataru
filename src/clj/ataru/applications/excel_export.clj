(ns ataru.applications.excel-export
  (:import [org.apache.poi.ss.usermodel Row VerticalAlignment Row$MissingCellPolicy]
           [java.io ByteArrayOutputStream]
           [org.apache.poi.xssf.usermodel XSSFWorkbook XSSFSheet XSSFCell XSSFCellStyle])
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

(def answers-to-always-include
  #{"higher-education-qualification-in-finland-institution"
    "studies-required-by-higher-education-field"
    "studies-required-by-higher-education-institution"
    "address"
    "email"
    "preferred-name"
    "last-name"
    "country-of-residence"
    "higher-education-qualification-outside-finland-country"
    "other-eligibility-description"
    "phone"
    "passport-number"
    "nationality"
    "city"
    "ssn"
    "first-name"
    "birth-date"
    "postal-code"
    "hakukohteet"
    "higher-education-qualification-outside-finland-qualification"
    "higher-education-qualification-in-finland-qualification"
    "higher-education-qualification-outside-finland-institution"
    "higher-education-qualification-outside-finland-level"
    "studies-required-by-higher-education-scope"
    "birthplace"
    "language"
    "upper-secondary-school-completed-country"
    "higher-education-qualification-in-finland-year-and-date"
    "higher-education-qualification-outside-finland-year-and-date"
    "higher-education-qualification-in-finland-level"
    "national-id-number"
    "gender"
    "postal-office"
    "home-town"
    "other-eligibility-year-of-completion"})

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
  [requirement-name application-hakukohde-reviews]
  (let [reviews     (filter
                      #(= (:requirement %) requirement-name)
                      application-hakukohde-reviews)
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
  [{:label     "Id"
    :field     [:application :key]}
   {:label     "Lähetysaika"
    :field     [:application :created-time]
    :format-fn time-formatter}
   {:label     "Hakemuksen tila"
    :field     [:application :state]
    :format-fn application-state-formatter}
   {:label     "Hakukohteen käsittelyn tila"
    :field     [:application :application-hakukohde-reviews]
    :format-fn (partial hakukohde-review-formatter "processing-state")}
   {:label     "Kielitaitovaatimus"
    :field     [:application :application-hakukohde-reviews]
    :format-fn (partial hakukohde-review-formatter "language-requirement")}
   {:label     "Tutkinnon kelpoisuus"
    :field     [:application :application-hakukohde-reviews]
    :format-fn (partial hakukohde-review-formatter "degree-requirement")}
   {:label     "Hakukelpoisuus"
    :field     [:application :application-hakukohde-reviews]
    :format-fn (partial hakukohde-review-formatter "eligibility-state")}
   {:label     "Maksuvelvollisuus"
    :field     [:application :application-hakukohde-reviews]
    :format-fn (partial hakukohde-review-formatter "payment-obligation")}
   {:label     "Valinnan tila"
    :field     [:application :application-hakukohde-reviews]
    :format-fn (partial hakukohde-review-formatter "selection-state")}
   {:label     "Pisteet"
    :field     [:application-review :score]}
   {:label     "Hakijan henkilö-OID"
    :field     [:application :person-oid]}
   {:label     "Turvakielto"
    :field     [:person :turvakielto]
    :format-fn (fnil (fn [turvakielto] (if turvakielto "kyllä" "ei")) false)}])

(defn- create-cell-styles
  [workbook]
  {:style (doto (.createCellStyle workbook)
            (.setWrapText true)
            (.setVerticalAlignment VerticalAlignment/TOP))
   :quote-prefix-style
   (doto (.createCellStyle workbook)
     (.setQuotePrefixed true)
     (.setWrapText true)
     (.setVerticalAlignment VerticalAlignment/TOP))})

(defn- create-workbook-and-styles
  []
  (let [workbook (XSSFWorkbook.)]
    [workbook (create-cell-styles workbook)]))

(defn- indexed-meta-fields
  [fields]
  (map-indexed (fn [idx field] (merge field {:column idx})) fields))

(defn- set-cell-style ^XSSFCell [^XSSFCell cell value styles]
  (if (and (string? value)
           (contains? #{\= \+ \- \@} (first value)))
    (.setCellStyle cell (:quote-prefix-style styles))
    (.setCellStyle cell (:style styles)))
  cell)

(defn- update-row-cell! [styles ^XSSFSheet sheet row column value]
  (when-let [^String v (not-empty (trim (str value)))]
    (-> (or (.getRow sheet (int row))
            (.createRow sheet (int row)))
        (.getCell (int column) Row$MissingCellPolicy/CREATE_NULL_AS_BLANK)
        (set-cell-style value styles)
        (.setCellValue v)))
  sheet)

(defn- make-writer [styles sheet row-offset]
  (fn [row column value]
    (update-row-cell!
      styles
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

(defn- get-label [koodisto lang koodi-uri]
  (let [koodi (->> koodisto
                   (filter (fn [{:keys [value]}]
                             (= value koodi-uri)))
                   first)]
    (get-in koodi [:label lang])))

(defn- raw-values->human-readable-value [field-descriptor {:keys [lang]} get-koodisto-options value]
  (let [lang (-> lang clojure.string/lower-case keyword)
        koodisto-source (:koodisto-source field-descriptor)
        options (:options field-descriptor)]
    (cond (some? koodisto-source)
          (let [koodisto (get-koodisto-options (:uri koodisto-source) (:version koodisto-source))
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

(defn- write-application! [writer application application-review person headers application-meta-fields form-fields-by-key get-koodisto-options]
  (doseq [meta-field application-meta-fields]
    (let [meta-value ((or
                        (:format-fn meta-field)
                        identity)
                      (get-in {:application application
                               :application-review application-review
                               :person person}
                              (:field meta-field)))]
      (writer 0 (:column meta-field) meta-value)))
  (doseq [answer (:answers application)]
    (let [field-descriptor (get form-fields-by-key (:key answer))
          column          (:column (first (filter #(= (:key answer) (:id %)) headers)))
          value-or-values (:value answer)
          value           (cond
                            (kysymysryhma-answer? value-or-values)
                            (->> value-or-values
                                 (map #(clojure.string/join "," %))
                                 (map (partial raw-values->human-readable-value field-descriptor application get-koodisto-options))
                                 (map-indexed #(format "#%s: %s,\n" %1 %2))
                                 (apply str))

                            (sequential? value-or-values)
                            (->> value-or-values
                                 (map (partial raw-values->human-readable-value field-descriptor application get-koodisto-options))
                                 (interpose ",\n")
                                 (apply str))

                            :else
                            (raw-values->human-readable-value field-descriptor application get-koodisto-options value-or-values))
          value-length    (count value)
          value-truncated (if (< max-value-length value-length)
                            (str
                              (subs value 0 (- max-value-length 100))
                              "—— [ vastaus liian pitkä Excel-vientiin, poistettu "
                              (- value-length max-value-length -100) " merkkiä]")
                            value)]
      (when (and value-truncated column)
        (writer 0 (+ column (count application-meta-fields)) value-truncated)))))

(defn- form-label? [form-element]
  (and (not= "infoElement" (:fieldClass form-element))
       (not (:exclude-from-answers form-element))))

(defn- pick-answer? [skip-answers? element-id]
  (or (not skip-answers?)
      (contains? answers-to-always-include element-id)))

(defn- hidden-answer? [form-element]
  (:exclude-from-answers form-element))

(defn- pick-label
  [form-element]
  [(:id form-element)
   (label/get-language-label-in-preferred-order (:label form-element))])

(defn pick-form-labels
  [flat-fields pick-cond]
  (->> flat-fields
       (filter pick-cond)
       (map pick-label)))

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

(defn- extract-headers-from-applications [applications flat-fields skip-answers]
  (let [hidden-answers (->> (pick-form-labels flat-fields hidden-answer?)
                            (map first)
                            set)]
    (->> applications
         (mapcat (fn [application]
                   (map (fn [{:keys [key label]}] [key label])
                        (:answers application))))
         (into {})
         (filter (fn [[key _]]
                   (and (pick-answer? skip-answers key)
                        (not (contains? hidden-answers key))))))))

(defn- extract-headers
  [applications form skip-answers?]
  (let [flat-fields            (util/flatten-form-fields (:content form))
        labels-in-form         (pick-form-labels flat-fields
                                                 #(and (form-label? %)
                                                       (pick-answer? skip-answers? (:id %))))
        labels-in-applications (let [form-ids (set (map first labels-in-form))]
                                 (remove #(contains? form-ids (first %))
                                         (extract-headers-from-applications
                                          applications
                                          flat-fields
                                          skip-answers?)))
        all-labels             (concat labels-in-form labels-in-applications)]
    (for [[idx [id header]] (map vector (range) all-labels)
          :let [header (or header "")]]
      {:id               id
       :decorated-header (decorate flat-fields (:content form) id header)
       :header           header
       :column           idx})))

(defn- create-form-meta-sheet [workbook styles meta-fields]
  (let [sheet  (.createSheet workbook "Lomakkeiden tiedot")
        writer (make-writer styles sheet 0)]
    (doseq [meta-field meta-fields
            :let [column (:column meta-field)
                  label  (:label meta-field)]]
      (writer 0 column label))
    sheet))

(def ^:private invalid-char-matcher #"[\\/\*\[\]:\?]")

(defn- sheet-name [{:keys [id name]}]
  {:pre [(some? id)
         (some? name)]}
  (let [name (str id "_" (clojure.string/replace (some (partial get name) [:fi :sv :en]) invalid-char-matcher "_"))]
    (cond-> name
      (> (count name) 30)
      (subs 0 30))))

(defn set-column-widths [^XSSFWorkbook workbook]
  (doseq [n (range (.getNumberOfSheets workbook))
          :let [sheet (.getSheetAt workbook (int n))]
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

(defn- get-hakukohde-name [get-hakukohde lang-s oid]
  (let [lang (keyword lang-s)]
    (when-let [hakukohde (get-hakukohde oid)]
      (str (get-in hakukohde [:name lang]) " - "
           (get-in hakukohde [:tarjoaja-name lang])))))

(defn- add-hakukohde-name [get-haku get-hakukohde lang hakukohde-answer haku-oid]
  (update hakukohde-answer :value
          (partial map-indexed
                   (fn [index oid]
                     (let [name           (get-hakukohde-name get-hakukohde lang oid)
                           priority-index (when (:prioritize-hakukohteet (:tarjonta (get-haku haku-oid)))
                                            (str "(" (inc index) ") "))]
                       (if name
                         (str priority-index name " (" oid ")")
                         (str priority-index oid)))))))

(defn- add-hakukohde-names [get-haku get-hakukohde application]
  (update application :answers
          (partial map (fn [answer]
                         (if (= "hakukohteet" (:key answer))
                           (add-hakukohde-name get-haku get-hakukohde (:lang application) answer (:haku application))
                           answer)))))

(defn- add-all-hakukohde-reviews
  [get-hakukohde selected-hakukohde application]
  (let [all-reviews            (application-states/get-all-reviews-for-all-requirements
                                 application
                                 selected-hakukohde)
        all-reviews-with-names (map
                                 (fn [{:keys [hakukohde] :as review}]
                                   (assoc review
                                     :hakukohde-name
                                     (get-hakukohde-name
                                       get-hakukohde
                                       (:lang application)
                                       hakukohde)))
                                 all-reviews)]
    (assoc application :application-hakukohde-reviews all-reviews-with-names)))

(defn export-applications [applications application-reviews persons selected-hakukohde skip-answers? tarjonta-service ohjausparametrit-service]
  (let [[^XSSFWorkbook workbook styles] (create-workbook-and-styles)
        form-meta-fields                (indexed-meta-fields form-meta-fields)
        form-meta-sheet                 (create-form-meta-sheet workbook styles form-meta-fields)
        application-meta-fields         (indexed-meta-fields application-meta-fields)
        get-form-by-id                  (memoize form-store/fetch-by-id)
        get-latest-form-by-key          (memoize form-store/fetch-by-key)
        get-koodisto-options            (memoize koodisto/get-koodisto-options)
        get-hakukohde                   (memoize (fn [oid]
                                                   (tarjonta-parser/parse-hakukohde
                                                    tarjonta-service
                                                    (tarjonta/get-hakukohde tarjonta-service oid))))
        get-tarjonta-info               (memoize (fn [haku-oid]
                                                   (tarjonta-parser/parse-tarjonta-info-by-haku
                                                    tarjonta-service
                                                    ohjausparametrit-service
                                                    haku-oid)))]
    (->> applications
         (map update-hakukohteet-for-legacy-applications)
         (map (partial add-hakukohde-names get-tarjonta-info get-hakukohde))
         (map (partial add-all-hakukohde-reviews get-hakukohde selected-hakukohde))
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
         (map-indexed (fn [sheet-idx {:keys [^String sheet-name form applications]}]
                        (let [applications-sheet (.createSheet workbook sheet-name)
                              headers            (extract-headers applications form skip-answers?)
                              meta-writer        (make-writer styles form-meta-sheet (inc sheet-idx))
                              header-writer      (make-writer styles applications-sheet 0)
                              form-fields-by-key (reduce #(assoc %1 (:id %2) %2)
                                                         {}
                                                         (util/flatten-form-fields (:content form)))]
                          (write-form-meta! meta-writer form applications form-meta-fields)
                          (write-headers! header-writer headers application-meta-fields)
                          (->> applications
                               (sort-by :created-time)
                               (reverse)
                               (map #(merge % (get-tarjonta-info (:haku %))))
                               (map-indexed (fn [row-idx application]
                                              (let [row-writer (make-writer styles applications-sheet (inc row-idx))
                                                    application-review (get application-reviews (:key application))
                                                    person (get persons (:person-oid application))]
                                                (write-application! row-writer
                                                                    application
                                                                    application-review
                                                                    person
                                                                    headers
                                                                    application-meta-fields
                                                                    form-fields-by-key
                                                                    get-koodisto-options))))
                               (dorun))
                          (.createFreezePane applications-sheet 0 1 0 1))))
         (dorun))
    (set-column-widths workbook)
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

