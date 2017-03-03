(ns ataru.applications.application-service
  (:require
    [ataru.applications.application-access-control :as aac]
    [ataru.forms.form-access-control :as form-access-control]
    [ataru.forms.form-store :as form-store]
    [ataru.koodisto.koodisto :as koodisto]
    [ataru.applications.application-store :as application-store]
    [ataru.middleware.user-feedback :refer [user-feedback-exception]]
    [ataru.applications.excel-export :as excel]
    [taoensso.timbre :refer [spy debug]]
    [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser])
  (:import [java.io ByteArrayInputStream]))

(defn get-application-list-by-form [form-key session organization-service]
  (aac/check-form-access form-key session organization-service :view-applications)
  {:applications (application-store/get-application-list-by-form form-key)})

(defn- extract-koodisto-fields [field-descriptor-list]
  (reduce
    (fn [result {:keys [children id koodisto-source]}]
      (if (some? children)
        (merge result (extract-koodisto-fields children))
        (cond-> result
          (every? some? [id koodisto-source])
          (assoc id (select-keys koodisto-source [:uri :version])))))
    {}
    field-descriptor-list))

(defn- get-koodi [koodisto koodi-value]
  (let [koodi-pred (comp (partial = koodi-value) :value)]
    (->> koodisto
         (filter koodi-pred)
         first)))

(defn- populate-koodisto-fields [application {:keys [content]}]
  (let [koodisto-fields (extract-koodisto-fields content)
        lang            (-> (:lang application)
                            clojure.string/lower-case
                            keyword)]
    (update application :answers
      (partial map
        (fn [{:keys [key] :as answer}]
          (cond-> answer
            (contains? koodisto-fields key)
            (update :value (fn [koodi-value]
                             (let [koodisto-uri         (get-in koodisto-fields [key :uri])
                                   version              (get-in koodisto-fields [key :version])
                                   koodisto             (koodisto/get-koodisto-options koodisto-uri version)
                                   human-readable-value (->> (cond-> koodi-value
                                                               (string? koodi-value)
                                                               (clojure.string/split #"\s*,\s*"))
                                                             (map (fn [koodi-uri]
                                                                    (let [koodi (get-koodi koodisto koodi-uri)]
                                                                      (get-in koodi [:label lang])))))]
                               (cond-> human-readable-value
                                 (= (count human-readable-value) 1)
                                 first))))))))))

(defn get-application-with-human-readable-koodis
  "Get application that has human-readable koodisto values populated
   onto raw koodi values."
  [application-key session organization-service tarjonta-service]
  (let [bare-application (application-store/get-latest-application-by-key application-key)
        form             (form-store/fetch-by-id (:form bare-application))
        tarjonta-info    (tarjonta-parser/parse-tarjonta-info tarjonta-service (:hakukohde bare-application))
        application      (populate-koodisto-fields bare-application form)]
    (aac/check-application-access application-key session organization-service :view-applications)
    {:application (merge application tarjonta-info)
     :form        form
     :events      (application-store/get-application-events application-key)
     :review      (application-store/get-application-review application-key)}))

(defn get-excel-report-of-applications-by-form
  [form-key filtered-states session organization-service tarjonta-service]
  (aac/check-form-access form-key session organization-service :view-applications)
  (let [applications (application-store/get-applications-for-form form-key filtered-states)]
    (ByteArrayInputStream. (excel/export-applications applications tarjonta-service))))

(defn get-excel-report-of-applications-by-hakukohde
  [hakukohde-oid filtered-states session organization-service tarjonta-service]
  (let [applications (->> (application-store/get-applications-for-hakukohde filtered-states hakukohde-oid)
                          (filter (comp #(form-access-control/form-allowed-by-key?
                                          %
                                          session
                                          organization-service
                                          :view-applications)
                                        :form-key)))]
    (ByteArrayInputStream. (excel/export-applications applications tarjonta-service))))

(defn get-excel-report-of-applications-by-haku
  [haku-oid filtered-states session organization-service tarjonta-service]
  (let [applications (->> (application-store/get-applications-for-haku haku-oid filtered-states)
                          (filter (comp #(form-access-control/form-allowed-by-key?
                                          %
                                          session
                                          organization-service
                                          :view-applications)
                                        :form-key)))]
    (ByteArrayInputStream. (excel/export-applications applications tarjonta-service))))

(defn save-application-review [review session organization-service]
  (let [application-key (:application-key review)]
    (aac/check-application-access application-key session organization-service)
    (application-store/save-application-review review session)
    {:review (application-store/get-application-review application-key)
     :events (application-store/get-application-events application-key)}))
