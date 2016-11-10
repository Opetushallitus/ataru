(ns ataru.applications.application-service
  (:require
    [ataru.applications.application-access-control :as aac]
    [ataru.forms.form-access-control :as form-access-control]
    [ataru.forms.form-store :as form-store]
    [ataru.koodisto.koodisto :as koodisto]
    [ataru.applications.application-store :as application-store]
    [ataru.middleware.user-feedback :refer [user-feedback-exception]]
    [ataru.applications.excel-export :as excel]
    [ataru.tarjonta-service.tarjonta-client :as tarjonta-client]))

(defn get-application-list-by-form [form-key session organization-service]
  (aac/check-form-access form-key session organization-service)
  {:applications (application-store/get-application-list-by-form form-key)})

(defn get-application-list-by-hakukohde [hakukohde-oid session organization-service]
  (when-let [form-key (tarjonta-client/get-form-key-for-hakukohde hakukohde-oid)] ; TODO maybe avoid remote call
    (aac/check-form-access form-key session organization-service)
    {:applications (application-store/get-application-list-by-hakukohde form-key hakukohde-oid)}))

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
  [application-key session organization-service]
  (let [application (dissoc (application-store/get-latest-application-by-key application-key) :secret)
        form        (form-store/fetch-by-id (:form application))
        application (populate-koodisto-fields application form)]
    (aac/check-application-access application-key session organization-service)
    {:application application
     :form        form
     :events      (application-store/get-application-events application-key)
     :review      (application-store/get-application-review application-key)}))

(defn get-excel-report-of-applications-by-form [form-key session organization-service]
  (aac/check-form-access form-key session organization-service)
  (java.io.ByteArrayInputStream. (excel/export-all-form-applications form-key)))

(defn get-excel-report-of-applications-by-hakukohde [form-key hakukohde-oid session organization-service]
  (aac/check-form-access form-key session organization-service)
  (java.io.ByteArrayInputStream. (excel/export-all-hakukohde-applications form-key hakukohde-oid)))

(defn save-application-review [review session organization-service]
  (let [application-key (:application-key review)]
    (aac/check-application-access application-key session organization-service)
    (application-store/save-application-review review)
    {:review (application-store/get-application-review application-key)
     :events (application-store/get-application-events application-key)}))
