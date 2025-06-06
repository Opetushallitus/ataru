(ns ataru.applications.excel-export
  (:require [ataru.application.application-states :as application-states]
            [ataru.application.review-states :as review-states]
            [ataru.component-data.component-util :refer [answer-to-always-include?]]
            [ataru.excel-common :refer [form-field-belongs-to-hakukohde
                                        hakemuksen-yleiset-tiedot-field-labels
                                        hakukohderyhma-to-hakukohde-oids
                                        kasittelymerkinnat-field-labels]]
            [ataru.files.file-store :as file-store]
            [ataru.forms.form-store :as form-store]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [ataru.translations.texts :refer [excel-texts virkailija-texts]]
            [ataru.util :as util :refer [to-vec]]
            [ataru.tutkinto.tutkinto-util :as tutkinto-util]
            [ataru.koski.koski-service :as koski]
            [ataru.koski.koski-json-parser :refer [parse-koski-tutkinnot]]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.core.match :refer [match]]
            [clojure.string :as string :refer [trim]]
            [taoensso.timbre :as log])
  (:import [java.io ByteArrayOutputStream]
           [org.apache.poi.ss.usermodel Row$MissingCellPolicy VerticalAlignment]
           [org.apache.poi.xssf.usermodel XSSFCell XSSFSheet XSSFWorkbook]))

(def max-value-length 5000)

(def tz (t/time-zone-for-id "Europe/Helsinki"))

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
  [state lang]
  (or
   (lang (->> review-states/application-review-states
              (filter #(= (first %) state))
              first
              second))
   "Tuntematon"))

(defn hakukohde-review-formatter
  [requirement-name application-hakukohde-reviews lang]
  (let [reviews     (filter
                     #(= (:requirement %) requirement-name)
                     application-hakukohde-reviews)
        requirement (last
                     (first
                      (filter #(= (first %) (keyword requirement-name)) review-states/hakukohde-review-types)))
        get-label   (partial application-states/get-review-state-label-by-name requirement)]
    (if (= 1 (count reviews))
      (get-label (-> (first reviews) :state) lang)
      (clojure.string/join
       "\n"
       (map
        (fn [{:keys [hakukohde hakukohde-name state]}]
          (format "%s (%s): %s"
                  hakukohde-name
                  hakukohde
                  (get-label state lang)))
        reviews)))))

(defn- ehdollinen-formatter
  [ehdollinen?]
  (if (< 1 (count ehdollinen?))
    (clojure.string/join
     "\n"
     (map (fn [{:keys [hakukohde hakukohde-name ehdollinen?]}]
            (format "%s (%s): %s"
                    hakukohde-name
                    hakukohde
                    (if ehdollinen? "kyllä" "ei")))
          ehdollinen?))
    (if (:ehdollinen? (first ehdollinen?)) "kyllä" "ei")))

(defn- application-review-notes-formatter
  ([review-notes]
   (application-review-notes-formatter nil review-notes))
  ([state-to-select review-notes]
   (->> (if state-to-select
          (filter #(= state-to-select (:state-name %)) review-notes)
          (remove #(some? (:state-name %)) review-notes))
        (map (fn [{:keys [created-time notes hakukohde first-name last-name]}]
               (str
                (time-formatter created-time)
                " "
                (when (and first-name last-name)
                  (str
                   first-name
                   " "
                   last-name))
                (when hakukohde
                  (str " (hakukohde " hakukohde ")"))
                ": "
                notes)))
        (clojure.string/join ",\n"))))

(defn kk-payment-state-formatter
  [state lang]
  (or
   (lang (->> review-states/kk-application-payment-states
              (filter #(= (first %) state))
              first
              second))
   ""))

(def ^:private form-meta-fields
  [{:label     (:name excel-texts)
    :field     :name
    :format-fn #(some (partial get %) [:fi :sv :en])}
   {:label     (:id excel-texts)
    :field     :id}
   {:label     (:key excel-texts)
    :field     :key}
   {:label     (:created-time excel-texts)
    :field     :created-time
    :format-fn time-formatter}
   {:label     (:created-by excel-texts)
    :field     :created-by}])

(defn uneligible?
  [hakukohde-reviews]
  (some #(and (= "eligibility-state" (:requirement %))
              (= "uneligible" (:state %)))
        hakukohde-reviews))

(defn- render-when-uneligible
  [application]
  (uneligible? (get-in application [:application :application-hakukohde-reviews])))

(def ^:private application-meta-fields-by-id
  {"application-number"            {:field     [:application :key]}
   "application-submitted-time"    {:field     [:application :submitted]
                                    :format-fn time-formatter}
   "application-modified-time"     {:field     [:application :created-time]
                                    :format-fn time-formatter}
   "application-state"             {:field     [:application :state]
                                    :lang?     true
                                    :format-fn application-state-formatter}
   "student-number"                {:field     [:person :master-oid]}
   "applicant-oid"                 {:field     [:application :person-oid]}
   "turvakielto"                   {:field     [:person :turvakielto]
                                    :format-fn (fnil (fn [turvakielto] (if turvakielto "kyllä" "ei")) false)}
   "hakukohde-handling-state"      {:field     [:application :application-hakukohde-reviews]
                                    :lang?     true
                                    :format-fn (partial hakukohde-review-formatter "processing-state")}
   "kielitaitovaatimus"            {:field     [:application :application-hakukohde-reviews]
                                    :lang?     true
                                    :format-fn (partial hakukohde-review-formatter "language-requirement")}
   "tutkinnon-kelpoisuus"          {:field     [:application :application-hakukohde-reviews]
                                    :lang?     true
                                    :format-fn (partial hakukohde-review-formatter "degree-requirement")}
   "hakukelpoisuus"                {:field     [:application :application-hakukohde-reviews]
                                    :lang?     true
                                    :format-fn (partial hakukohde-review-formatter "eligibility-state")}
   "eligibility-set-automatically" {:field     [:application :eligibility-set-automatically]
                                    :format-fn #(clojure.string/join "\n" %)}
   "ineligibility-reason"          {:field     [:application-review-notes]
                                    :format-fn (partial application-review-notes-formatter "eligibility-state")
                                    :render-when-fn render-when-uneligible}
   "maksuvelvollisuus"             {:field     [:application :application-hakukohde-reviews]
                                    :lang?     true
                                    :format-fn (partial hakukohde-review-formatter "payment-obligation")}
   "valinnan-tila"                 {:field     [:application :application-hakukohde-reviews]
                                    :lang?     true
                                    :format-fn (partial hakukohde-review-formatter "selection-state")}
   "ehdollinen"                    {:field     [:ehdollinen?]
                                    :format-fn ehdollinen-formatter}
   "pisteet"                       {:field     [:application-review :score]}
   "application-review-notes"      {:field     [:application-review-notes]
                                    :format-fn application-review-notes-formatter}
   "kk-payment-state"              {:field     [:application :kk-payment-state]
                                    :lang?     true
                                    :format-fn kk-payment-state-formatter}})

(def ^:private application-meta-fields
  (map #(merge % (get application-meta-fields-by-id (:id %)))
       (concat hakemuksen-yleiset-tiedot-field-labels kasittelymerkinnat-field-labels)))

(def ^:private old-application-meta-fields-order
  ["application-number"
   "application-submitted-time"
   "application-modified-time"
   "application-state"
   "hakukohde-handling-state"
   "kielitaitovaatimus"
   "tutkinnon-kelpoisuus"
   "hakukelpoisuus"
   "eligibility-set-automatically"
   "ineligibility-reason"
   "maksuvelvollisuus"
   "valinnan-tila"
   "ehdollinen"
   "pisteet"
   "student-number"
   "applicant-oid"
   "turvakielto"
   "application-review-notes"
   "kk-payment-state"])

(def ^:private old-application-meta-fields
  (map #(merge % (get application-meta-fields-by-id (:id %)))
       (sort-by #(.indexOf old-application-meta-fields-order (:id %))
                (concat hakemuksen-yleiset-tiedot-field-labels kasittelymerkinnat-field-labels))))

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

(defn- write-headers! [writer headers]
  (doseq [header headers]
    (writer 0 (:column header) (:decorated-header header))))

(defn- get-label [koodisto lang koodi-uri]
  (let [koodi (->> koodisto
                   (filter (fn [{:keys [value]}]
                             (= value koodi-uri)))
                   first)]
    (util/non-blank-val (:label koodi) [lang :fi :sv :en])))

(defn- convert-answer-with-options-to-human-readable
  [value options lang]
  (some (fn [option]
          (when (= value (:value option))
            (or (util/non-blank-val (:label option) [lang :fi :sv :en])
                value)))
        options))

(defn- raw-values->human-readable-value [liiteri-cas-client field-descriptor {:keys [lang]} get-koodisto-options value]
  (let [lang (-> lang clojure.string/lower-case keyword)
        koodisto-source (:koodisto-source field-descriptor)
        options (:options field-descriptor)]
    (cond (some? koodisto-source)
          (let [koodisto (get-koodisto-options (:uri koodisto-source)
                                               (:version koodisto-source)
                                               (:allow-invalid? koodisto-source))
                koodi-uri->label (partial get-label koodisto lang)]
            (->> (clojure.string/split value #"\s*,\s*")
                 (mapv koodi-uri->label)
                 (interpose ",\n")
                 (apply str)))
          (= (:fieldType field-descriptor) "attachment")
          (try
            (let [[{:keys [filename size]}] (file-store/get-metadata liiteri-cas-client [value])]
              (str filename " (" (util/size-bytes->str size) ")"))
            (catch Exception _
              (util/non-blank-val (:internal-server-error virkailija-texts)
                                  [lang :fi :sv :en])))
          (and (not (= (:fieldType field-descriptor) "textField"))
               (not (empty? options)))
          (convert-answer-with-options-to-human-readable value options lang)
          :else
          value)))

(defn- ->tutkinto-values [id-answer-value tutkinnot lang]
  (let [ids (flatten id-answer-value)
        selected-tutkinnot (filter #(some #{(:id %)} ids) tutkinnot)
        field-mappings (tutkinto-util/get-tutkinto-field-mappings lang)]
    (mapv
      (fn [tutkinto]
        (->> (filter some? (map
                             #(if (:multi-lang? %)
                                (get-in tutkinto [(:koski-tutkinto-field %) lang])
                                ((:koski-tutkinto-field %) tutkinto))
                            field-mappings))
             (clojure.string/join ";")
             (util/to-vec)))
      selected-tutkinnot)))

(defn- write-answer-value-for-excel!
  [liiteri-cas-client writer person header form-fields-by-key get-koodisto-options application lang tutkinnot]
  (when-let [answer (first (filter #(= (:id header) (:key %)) (:answers application)))]
    (try
      (let [column                 (:column header)
            answer-key             (:key answer)
            field-descriptor       (cond
                                     (some? (:duplikoitu-kysymys-hakukohde-oid answer))
                                      (get form-fields-by-key (:original-question answer))
                                     (some? (:duplikoitu-followup-hakukohde-oid answer))
                                      (get form-fields-by-key (:original-followup answer))
                                     :else
                                      (get form-fields-by-key answer-key))
            value-or-values        (cond
                                     (contains? person (keyword answer-key))
                                     (get person (keyword answer-key))
                                     (tutkinto-util/is-koski-tutkinto-id-field? answer-key)
                                     (->tutkinto-values (:value answer) tutkinnot lang)
                                     :else
                                     (:value answer))
            ->human-readable-value (partial raw-values->human-readable-value liiteri-cas-client field-descriptor application get-koodisto-options)
            value                  (cond
                                     (util/is-question-group-answer? value-or-values)
                                     (->> value-or-values
                                          (map #(clojure.string/join "," %))
                                          (map ->human-readable-value)
                                          (map-indexed #(format "#%s: %s,\n" %1 %2))
                                          (apply str))

                                     (vector? value-or-values)
                                     (->> value-or-values
                                          (map ->human-readable-value)
                                          (interpose ",\n")
                                          (apply str))

                                     :else
                                     (->human-readable-value value-or-values))
            value-length           (count value)
            value-truncated        (if (< max-value-length value-length)
                                     (str
                                      (subs value 0 (- max-value-length 100))
                                      "—— [ vastaus liian pitkä Excel-vientiin, poistettu "
                                      (- value-length max-value-length -100) " merkkiä]")
                                     value)]
        (when value-truncated
          (writer 0 column value-truncated)))
      (catch Exception e
        (log/error "Caught exception while trying to parse value for answer"
                   (:key answer)
                   "from application"
                   (:key application)
                   ". Exception:"
                   e)
        (throw e)))))

(defn write-meta-field! [writer meta-field value-from lang col]
  (let [value-candidate (get-in value-from (to-vec (:field meta-field)))
        value (if (delay? value-candidate) @value-candidate value-candidate)
        format-fn  (:format-fn meta-field)
        render-when-fn (:render-when-fn meta-field)
        meta-value (if (some? format-fn)
                     (if (:lang? meta-field)
                       (format-fn value lang)
                       (format-fn value))
                     value)]
    (when (or (nil? render-when-fn)
              (render-when-fn value-from))
      (writer 0 col meta-value))))

(defn- write-form-meta!
  [writer form applications fields lang]
  (doseq [meta-field fields]
    (let [col        (:column meta-field)
          value-from (case (:from meta-field)
                       :applications (first applications)
                       form)]
      (write-meta-field! writer meta-field value-from lang col))))

(defn- write-application! [liiteri-cas-client
                           writer
                           application
                           application-review
                           application-review-notes
                           person
                           ehdollinen?
                           headers
                           form-fields-by-key
                           get-koodisto-options
                           lang
                           tutkinnot]
  (doseq [header headers]
    (if-let [meta-field (get application-meta-fields-by-id (:id header))]
      (let [value-from  {:application              application
                         :application-review       application-review
                         :application-review-notes application-review-notes
                         :person                   person
                         :ehdollinen?              ehdollinen?}]
        (write-meta-field! writer meta-field value-from lang (:column header)))
      (write-answer-value-for-excel! liiteri-cas-client writer person header form-fields-by-key
                                     get-koodisto-options application lang tutkinnot))))

(defn- pick-header
  [form-fields-by-id form-field]
  (str (match form-field
         {:params      {:adjacent true}
          :children-of parent-id}
         (str (util/non-blank-val (get-in form-fields-by-id [parent-id :label])
                                  [:fi :sv :en])
              " - ")
         {:fieldType "attachment"}
         "Liitepyyntö: "
         :else
         "")
       (util/non-blank-val (:label form-field) [:fi :sv :en])))

(defn- belongs-to-other-hakukohde?
  [form-field-belongs-to form-fields-by-id form-field]
  (if (not (form-field-belongs-to form-field))
    true
    (when-let [parent (or (some->> form-field
                                   :children-of
                                   (get form-fields-by-id))
                          (some->> form-field
                                   :followup-of
                                   (get form-fields-by-id)))]
      (belongs-to-other-hakukohde? form-field-belongs-to form-fields-by-id parent))))

(defn- is-per-hakukohde-followup?
  [form-fields-by-id question]
  (boolean
   (some->> question
            :followup-of
            (get form-fields-by-id)
            :per-hakukohde)))

(defn- followup-of-any? [form-field-or-answer included-ids form-fields-by-id]
  (let [previous-id (or (get form-field-or-answer :children-of)
                        (get form-field-or-answer :followup-of)
                        (get form-field-or-answer :original-question)
                        (get form-field-or-answer :original-followup))]
    (if (nil? previous-id)
      false
      (or (contains? included-ids previous-id)
          (followup-of-any? (get form-fields-by-id previous-id)
                            included-ids
                            form-fields-by-id)))))

(defn- headers-from-form
  [form-fields form-fields-by-id included-ids ids-only? skip-answers? form-field-belongs-to]
  (let [should-include?
        (fn [field]
          (let [candidate? (and (not (:exclude-from-answers field))
                                (util/answerable? field)
                                (not (:per-hakukohde field))
                                (not (is-per-hakukohde-followup? form-fields-by-id field)))
                hakukohde? (delay (not (belongs-to-other-hakukohde? form-field-belongs-to form-fields-by-id field)))
                always? (answer-to-always-include? (:id field))]
            (and candidate?
                 (if ids-only?
                   (or (contains? included-ids (:id field))
                       (and (followup-of-any? field included-ids form-fields-by-id)
                            @hakukohde?))
                   (or always?
                       (and (not always?)
                            (not skip-answers?)
                            (or (and (empty? included-ids) @hakukohde?)
                                (contains? included-ids (:id field)))))))))]
    (->> form-fields
         (filter should-include?)
         (map #(vector (:id %) (pick-header form-fields-by-id %))))))

(defn- duplicate-header-per-hakukohde
  [form-fields answer application strip-priority-index?]
  (let [field (first (filter #(= (:id %) (or (:original-question answer) (:original-followup answer))) form-fields))
        hakukohteet (:value (first (filter #(= (:key %) "hakukohteet") (:answers application))))
        get-hakukohde-for-answer (fn [answer] (first (filter #(string/includes? % (or (:duplikoitu-kysymys-hakukohde-oid answer) (:duplikoitu-followup-hakukohde-oid answer))) hakukohteet)))
        remove-oid-from-hakukohde (fn [hakukohde]
                                    ; Poistetaan hakutoiveen järjestysnumero alusta, jotta saadaan karsittua myöhemmin distinct:llä ylimääräiset sarakkeet.
                                    (-> (if strip-priority-index?
                                          (-> hakukohde
                                              (string/split #"^\(\d+\) ")
                                              (last))
                                          hakukohde)
                                        (string/reverse)
                                        (string/split #"\(" 2)
                                        (last)
                                        (string/reverse)))
        label (str (util/non-blank-val (:label field) [:fi :sv :en]) "\n" (remove-oid-from-hakukohde (get-hakukohde-for-answer answer)))]
    (vector (:key answer) label)))

(defn- original-question-id [id] (first (string/split id #"_")))

(defn- application-header-comparator
  [form-fields form-fields-by-id]
  (fn [[a-key] [b-key]]
    (let [a-field-id (original-question-id a-key)
          b-field-id (original-question-id b-key)
          a-field (get form-fields-by-id a-field-id)
          b-field (get form-fields-by-id b-field-id)
          a-field-idx (.indexOf form-fields a-field)
          b-field-idx (.indexOf form-fields b-field)]
      (- a-field-idx b-field-idx))))

(defn- headers-from-applications
  [form-fields form-fields-by-id form-field-belongs-to included-ids ids-only? skip-answers? applications]
  (let [indexed-answers (->> applications
                             (map-indexed (fn [index application] (map #(assoc % :application-index index) (:answers application))))
                             (flatten))
        answers-by-id (into {} (map (fn [answer] [(:key answer) answer]) indexed-answers))]
    (->> indexed-answers
         (remove #(or (contains? form-fields-by-id (:key %))
                      (if ids-only?
                        (let [hakukohde-oid (or (:duplikoitu-kysymys-hakukohde-oid %) (:duplikoitu-followup-hakukohde-oid %))]
                          (not (or (contains? included-ids (:key %))
                                   (and
                                    (followup-of-any? % included-ids (merge form-fields-by-id answers-by-id))
                                    ; Luodaan vastauksesta lomakkeen kenttää näyttävä objekti ja tarkistetaan sillä halutaanko se valita hakukohteen perusteella
                                    (form-field-belongs-to {:belongs-to-hakukohteet (if hakukohde-oid [hakukohde-oid] nil)})))))
                        (and skip-answers?
                             (not (answer-to-always-include? (:key %)))))))
         (map (fn [answer] (if (or (:original-question answer) (:original-followup answer))
                             (duplicate-header-per-hakukohde form-fields answer (nth applications (:application-index answer)) ids-only?)
                             (vector (:key answer) (util/non-blank-val (:label answer) [:fi :sv :en])))))
         (distinct)
         (sort (application-header-comparator form-fields form-fields-by-id)))))

(defn headers-from-meta-fields [included-ids ids-only? lang]
  (->> (if ids-only? application-meta-fields old-application-meta-fields)
       (filter #(if ids-only?
                  (contains? included-ids (:id %))
                  true))
       (map #(vec [(:id %) (-> % :label lang)]))))

(defn- extract-headers
  [applications form form-field-belongs-to included-ids ids-only? skip-answers? lang]
  (let [form-fields       (util/flatten-form-fields (:content form))
        form-fields-by-id (util/group-by-first :id form-fields)]
    (map-indexed (fn [idx [id header]]
                   {:id               id
                    :decorated-header (or header "")
                    :column           idx})
                 (concat
                  (headers-from-meta-fields included-ids ids-only? lang)
                  (headers-from-form form-fields
                                     form-fields-by-id
                                     included-ids
                                     ids-only?
                                     skip-answers?
                                     form-field-belongs-to)
                  (headers-from-applications form-fields
                                             form-fields-by-id
                                             form-field-belongs-to
                                             included-ids
                                             ids-only?
                                             skip-answers?
                                             applications)))))

(defn- create-form-meta-sheet [workbook styles meta-fields lang]
  (let [sheet  (.createSheet workbook "Lomakkeiden tiedot")
        writer (make-writer styles sheet 0)]
    (doseq [meta-field meta-fields
            :let [column (:column meta-field)
                  label  (-> meta-field :label lang)]]
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
          :let [sheet (.getSheetAt workbook (int n))
                row-count (.getPhysicalNumberOfRows sheet)]
          y (when (> row-count 0) (range (.getLastCellNum (.getRow sheet 0))))]
    (.autoSizeColumn sheet (short y))))

(defn- update-hakukohteet-for-legacy-applications [application]
  (let [hakukohteet (->> (:answers application)
                         (filter #(= "hakukohteet" (:key %)))
                         first)
        hakukohde   (:hakukohde application)]
    (if (or (not-empty hakukohteet) (empty? hakukohde))
      application
      (update application :answers conj
              {:key       "hakukohteet"
               :fieldType "hakukohteet"
               :value     hakukohde
               :label     {:fi "Hakukohteet"
                           :sv "Ansökningsmål"
                           :en "Application options"}}))))

(defn- get-hakukohde-name [get-hakukohde lang-s haku-oid hakukohde-oid]
  (let [lang (keyword lang-s)]
    (when-let [hakukohde (get-hakukohde haku-oid hakukohde-oid)]
      (str (util/non-blank-val (:name hakukohde) [lang :fi :sv :en]) " - "
           (util/non-blank-val (:tarjoaja-name hakukohde) [lang :fi :sv :en])))))

(defn- add-hakukohde-name [get-haku get-hakukohde lang hakukohde-answer haku-oid]
  (-> hakukohde-answer
      (update :value
              (partial map-indexed
                       (fn [index oid]
                         (let [name           (get-hakukohde-name get-hakukohde lang haku-oid oid)
                               priority-index (when (:prioritize-hakukohteet (get-haku haku-oid))
                                                (str "(" (inc index) ") "))]
                           (if name
                             (str priority-index name " (" oid ")")
                             (str priority-index oid))))))
      (update :value vec)))

(defn- add-hakukohde-names [get-haku get-hakukohde application]
  (update application :answers
          (partial map (fn [answer]
                         (if (= "hakukohteet" (:key answer))
                           (add-hakukohde-name get-haku get-hakukohde (:lang application) answer (:haku application))
                           answer)))))

(defn- add-all-hakukohde-reviews
  [get-hakukohde selected-hakukohde-oids application]
  (let [active-hakukohteet     (set (or
                                     (not-empty
                                      (:hakukohde application))
                                     ["form"]))
        all-reviews            (->> (application-states/get-all-reviews-for-all-requirements
                                     application
                                     selected-hakukohde-oids)
                                    (filter
                                     #(contains? active-hakukohteet (:hakukohde %))))
        all-reviews-with-names (map
                                (fn [{:keys [hakukohde] :as review}]
                                  (assoc review
                                         :hakukohde-name
                                         (get-hakukohde-name
                                          get-hakukohde
                                          (:lang application)
                                          (:haku application)
                                          hakukohde)))
                                all-reviews)]
    (assoc application :application-hakukohde-reviews all-reviews-with-names)))

(defn- get-ehdollinen?
  [get-hakukohde hakukohteiden-ehdolliset application selected-hakukohde-oids]
  (sequence
   (comp (filter (fn [hakukohde-oid]
                   (or (empty? selected-hakukohde-oids)
                       (some #{hakukohde-oid} selected-hakukohde-oids))))
         (map (fn [hakukohde-oid]
                {:hakukohde      hakukohde-oid
                 :hakukohde-name (get-hakukohde-name
                                  get-hakukohde
                                  (:lang application)
                                  (:haku application)
                                  hakukohde-oid)
                 :ehdollinen?    (contains? (get @hakukohteiden-ehdolliset
                                                 hakukohde-oid)
                                            (:key application))})))
   (:hakukohde application)))

(defn- filter-eligibility-set-automatically
  [selected-hakukohde-oids application]
  (if (empty? selected-hakukohde-oids)
    application
    (update application :eligibility-set-automatically
            (partial filter (set selected-hakukohde-oids)))))

(def asc #(compare %1 %2))

(def desc #(compare %2 %1))

(defn export-applications
  [liiteri-cas-client
   applications
   application-reviews
   application-review-notes
   selected-hakukohde
   selected-hakukohderyhma
   skip-answers?
   included-ids
   ids-only?
   sort-by-field
   sort-order
   lang
   hakukohteiden-ehdolliset
   tarjonta-service
   koodisto-cache
   organization-service
   ohjausparametrit-service
   koski-service]
  (let [[^XSSFWorkbook workbook styles] (create-workbook-and-styles)
        form-meta-fields             (indexed-meta-fields form-meta-fields)
        form-meta-sheet              (create-form-meta-sheet workbook styles form-meta-fields lang)
        get-form-by-id               (memoize form-store/fetch-by-id)
        get-latest-form-by-key       (memoize form-store/fetch-by-key)
        get-koodisto-options         (memoize (fn [uri version allow-invalid?]
                                                (koodisto/get-koodisto-options koodisto-cache uri version allow-invalid?)))
        hakukohteet-by-haku           (->> applications
                                           (group-by :haku)
                                           (map (fn [[haku applications]]
                                                  [haku (set (mapcat :hakukohde applications))]))
                                           (into {}))
        get-tarjonta-info     (memoize (fn [haku-oid hakukohde-oids]
                                         (-> (tarjonta-parser/parse-excel-tarjonta-info-by-haku
                                              tarjonta-service
                                              organization-service
                                              ohjausparametrit-service
                                              haku-oid
                                              hakukohde-oids)
                                             :tarjonta)))
        get-haku                     (fn [haku-oid] (get-tarjonta-info haku-oid (get hakukohteet-by-haku haku-oid)))
        get-hakukohde                (memoize (fn [haku-oid hakukohde-oid]
                                                (->> (get-tarjonta-info haku-oid (get hakukohteet-by-haku haku-oid))
                                                     :hakukohteet
                                                     (filter #(= hakukohde-oid (:oid %)))
                                                     first)))
        all-hakukohteet              (delay (->> hakukohteet-by-haku
                                                 (mapcat (fn [[haku hakukohteet]]
                                                           (:hakukohteet (get-tarjonta-info haku hakukohteet))))))
        selected-hakukohde-oids      (or (some-> selected-hakukohde vector)
                                         (and selected-hakukohderyhma
                                              (hakukohderyhma-to-hakukohde-oids all-hakukohteet selected-hakukohderyhma)))
        form-field-belongs-to        (fn [form-field] (form-field-belongs-to-hakukohde form-field selected-hakukohde selected-hakukohderyhma all-hakukohteet))]
    (->> applications
         (map update-hakukohteet-for-legacy-applications)
         (map (partial add-hakukohde-names get-haku get-hakukohde))
         (map (partial add-all-hakukohde-reviews get-hakukohde selected-hakukohde-oids))
         (map (partial filter-eligibility-set-automatically selected-hakukohde-oids))
         (reduce (fn [result {:keys [form] :as application}]
                   (let [form-key (:key (get-form-by-id form))
                         form     (get-latest-form-by-key form-key)]
                     (if (contains? result form-key)
                       (update-in result [form-key :applications] conj application)
                       (assoc result form-key {:sheet-name   (sheet-name form)
                                               :form         form
                                               :applications [application]}))))
                 {})
         (vals)
         (map-indexed (fn [sheet-idx {:keys [^String sheet-name form applications]}]
                        (let [applications-sheet (.createSheet workbook sheet-name)
                              headers            (extract-headers applications form form-field-belongs-to included-ids ids-only? skip-answers? lang)
                              meta-writer        (make-writer styles form-meta-sheet (inc sheet-idx))
                              header-writer      (make-writer styles applications-sheet 0)
                              form-fields-by-key (reduce #(assoc %1 (:id %2) %2)
                                                         {}
                                                         (util/flatten-form-fields (:content form)))
                              tutkinto-levels   (tutkinto-util/koski-tutkinto-levels-in-form form)]
                          (write-form-meta! meta-writer form applications form-meta-fields lang)
                          (write-headers! header-writer headers)
                          (->> applications
                               (sort-by sort-by-field (if (= sort-order :asc) asc desc))
                               (map-indexed (fn [row-idx application]
                                              (let [row-writer                   (make-writer styles applications-sheet (inc row-idx))
                                                    application-review           (get application-reviews (:key application))
                                                    review-notes-for-application (get application-review-notes (:key application))
                                                    person                       (:person application)
                                                    ; Getting ehdollinen? is quite expensive operation. Do it later and only if ehdollinen-column is included.
                                                    ehdollinen?                  (delay (get-ehdollinen? get-hakukohde hakukohteiden-ehdolliset application selected-hakukohde-oids))
                                                    tutkinnot                    (some->> (when
                                                                                            (tutkinto-util/koski-tutkinnot-in-application? application) (:person-oid application))
                                                                                          (koski/get-tutkinnot-for-oppija koski-service false)
                                                                                          :opiskeluoikeudet
                                                                                          (parse-koski-tutkinnot tutkinto-levels)
                                                                                          (tutkinto-util/sort-koski-tutkinnot))]
                                                (write-application! liiteri-cas-client
                                                                    row-writer
                                                                    application
                                                                    application-review
                                                                    review-notes-for-application
                                                                    person
                                                                    ehdollinen?
                                                                    headers
                                                                    form-fields-by-key
                                                                    get-koodisto-options
                                                                    lang
                                                                    tutkinnot))))
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
      (string/replace #"[åä]" "a")
      (string/replace #"ö" "o")
      (string/replace #"[^\w-]+" "")))

(defn create-filename [identifying-part]
  {:pre [(some? identifying-part)]}
  (str
   (sanitize-name identifying-part)
   "_"
   (time-formatter (t/now) filename-time-format)
   ".xlsx"))

