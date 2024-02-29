(ns ataru.applications.excel-export.text-field-spec
  (:require [ataru.applications.excel-export :as excel-export]
            [ataru.cache.cache-service :as cache-service]
            [ataru.forms.form-store :as form-store]
            [ataru.ohjausparametrit.ohjausparametrit-service :as ohjausparametrit-service]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]
            [clj-time.core :as clj-time]
            [speclj.core :as speclj :refer :all])
  (:import [java.io File FileOutputStream]
           [java.util UUID]
           [org.apache.poi.ss.usermodel WorkbookFactory]))

(def liiteri-cas-client nil)

(def koodisto-cache (reify cache-service/Cache
                      #_{:clj-kondo/ignore [:unused-binding]}
                      (get-from [this key])
                      #_{:clj-kondo/ignore [:unused-binding]}
                      (get-many-from [this keys])
                      #_{:clj-kondo/ignore [:unused-binding]}
                      (remove-from [this key])
                      #_{:clj-kondo/ignore [:unused-binding]}
                      (clear-all [this])))

(defn export-applications [applications input-params]
  (let [application-reviews      {}
        application-review-notes {}
        hakukohteiden-ehdolliset {}
        lang                     :fi]
    (excel-export/export-applications liiteri-cas-client
                                      applications
                                      application-reviews
                                      application-review-notes
                                      (input-params :selected-hakukohde)
                                      (input-params :selected-hakukohderyhma)
                                      (input-params :skip-answers?)
                                      (or (input-params :included-ids) #{})
                                      true
                                      lang
                                      hakukohteiden-ehdolliset
                                      (tarjonta-service/new-tarjonta-service)
                                      koodisto-cache
                                      (organization-service/new-organization-service)
                                      (ohjausparametrit-service/new-ohjausparametrit-service))))

(defmacro with-excel [input-params bindings & body]
  `(let [~(first bindings) (File/createTempFile (str "excel-" (UUID/randomUUID)) ".xlsx")
         applications# ~(second bindings)]
     (try
       (with-open [output# (FileOutputStream. (.getPath ~(first bindings)))]
         (->> (export-applications applications# ~input-params)
              (.write output#)))
       ~@body
       (finally
         (.delete ~(first bindings))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-row
  [sheet row-num]
  (when-let [row (.getRow sheet row-num)]
    (map (fn [col] (some-> (.getCell row col)
                           .getStringCellValue))
         (range (.getLastCellNum row)))))

(defn verify-pairs-contain [expected-pairs all-pairs]
  (speclj/should==
   expected-pairs
   (set (filter expected-pairs all-pairs))))

(defn- verify-sheet-rows-contain [expected sheet [header-row-num data-row-num]]
  (let [header (get-row sheet header-row-num)
        data   (get-row sheet data-row-num)
        pairs  (map vector header data)]
    (verify-pairs-contain expected pairs)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-form []
  {:id           123
   :key          "form_123_key"
   :name         {:fi "Form name"}
   :created-by   "SEPPO PAPUNEN"
   :created-time (clj-time/date-time 2016 6 14 12 34 56)
   :content      []})

(defn build-application-for-form [form]
  {:id                            9432
   :key                           "application_9432_key"
   :created-time                  (clj-time/date-time 2016 6 15 12 34 56)
   :state                         "active"
   :form                          (:id form)
   :name                          {:fi "textField lomake"}
   :lang                          "fi"
   :person                        {}
   :application-hakukohde-reviews []
   :answers                       []})

(defn build-field [label-fi]
  {:id         (UUID/randomUUID)
   :label      {:fi label-fi}
   :fieldType  "textField"
   :fieldClass "formField"})

(defn build-answer-for-field [field value]
  {:key       (:id field)
   :label     (:label field)
   :fieldType (:fieldType field)
   :value     value})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(describe "excel export:"
          (tags :unit :excel)

          (context "text field:"
            (context "no followups:"
              (with text-field (build-field "Kysymys"))
              (with form (-> (build-form)
                             (assoc :content [@text-field])))
              (with application (-> (build-application-for-form @form)
                                    (assoc :answers [(build-answer-for-field @text-field "Vastaus")])))
              (it "should export an answer"
                  (with-redefs [form-store/fetch-by-id  (fn [_] @form)
                                form-store/fetch-by-key (fn [_] @form)]
                    (with-excel {:skip-answers? false} [file [@application]]
                      (let [workbook          (WorkbookFactory/create file)
                            application-sheet (.getSheetAt workbook 1)]
                        (verify-sheet-rows-contain
                         #{["Kysymys" "Vastaus"]}
                         application-sheet
                         [0 1]))))))

            (context "has followups:"
              (with followup (build-field "Lisäkysymys"))
              (with text-field (-> (build-field "Kysymys")
                                   (assoc :options [{:value     "0"
                                                     :followups [@followup]}])))
              (with form (-> (build-form)
                             (assoc :content [@text-field])))
              (with application (-> (build-application-for-form form)
                                    (assoc :answers [(build-answer-for-field @text-field "Vastaus K")
                                                     (build-answer-for-field @followup "Vastaus LK")])))
              (it "should export an answer for the field"
                  (with-redefs [form-store/fetch-by-id  (fn [_] @form)
                                form-store/fetch-by-key (fn [_] @form)]
                    (with-excel {:skip-answers? false} [file [@application]]
                      (let [workbook          (WorkbookFactory/create file)
                            application-sheet (.getSheetAt workbook 1)]
                        (verify-sheet-rows-contain
                         #{["Kysymys" "Vastaus K"]}
                         application-sheet
                         [0 1])))))

              (it "should export an answer for the followup"
                  (with-redefs [form-store/fetch-by-id  (fn [_] @form)
                                form-store/fetch-by-key (fn [_] @form)]
                    (with-excel {:skip-answers? false} [file [@application]]
                      (let [workbook          (WorkbookFactory/create file)
                            application-sheet (.getSheetAt workbook 1)]
                        (verify-sheet-rows-contain
                         #{["Lisäkysymys" "Vastaus LK"]}
                         application-sheet
                         [0 1]))))))))
