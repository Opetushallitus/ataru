(ns ataru.applications.application-access-control
  (:require
   [ataru.forms.form-access-control :as form-access-control]
   [ataru.forms.form-store :as form-store]
   [ataru.koodisto.koodisto :as koodisto]
   [ataru.applications.application-store :as application-store]
   [ataru.middleware.user-feedback :refer [user-feedback-exception]]
   [ataru.applications.excel-export :as excel]))

(defn- check-form-access [form-key session organization-service]
  (when-not
      (form-access-control/form-allowed-by-key? form-key session organization-service)
    (throw (user-feedback-exception (str "Lomake " form-key " ei ole sallittu")))))

(defn- check-application-access [application-id session organization-service]
  (when-not
      (form-access-control/organization-allowed?
       session
       organization-service
       #(application-store/get-application-organization-oid application-id))
    (throw (user-feedback-exception (str "Hakemus " application-id " ei ole sallittu")))))

(defn- check-review-access [review-id session organization-service]
  (when-not
      (form-access-control/organization-allowed?
       session
       organization-service
       #(application-store/get-application-review-organization-oid review-id))
    (throw (user-feedback-exception (str "Hakemuksen arvostelu " review-id " ei ole sallittu")))))

(defn get-application-list [form-key session organization-service]
  (check-form-access form-key session organization-service)
  {:applications (application-store/get-application-list form-key)})

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
  (let [koodisto-fields (extract-koodisto-fields content)]
    (update application :answers
      (partial map
        (fn [{:keys [key] :as answer}]
          (cond-> answer
            (contains? koodisto-fields key)
            (update :value (fn [koodi-value]
                             (let [koodisto-uri (get-in koodisto-fields [key :uri])
                                   version      (get-in koodisto-fields [key :version])
                                   koodisto     (koodisto/get-koodisto-options koodisto-uri version)]
                               (-> koodisto
                                   (get-koodi koodi-value)
                                   (get-in [:label :fi])))))))))))

(defn get-application [application-id session organization-service]
  (let [application (application-store/get-application application-id)
        form        (form-store/fetch-by-id (:form application))
        application (populate-koodisto-fields application form)]
    (check-application-access application-id session organization-service)
    {:application application
     :form        form
     :events      (application-store/get-application-events application-id)
     :review      (application-store/get-application-review application-id)}))

(defn get-excel-report-of-applications [form-key session organization-service]
  (check-form-access form-key session organization-service)
  (java.io.ByteArrayInputStream. (excel/export-all-applications form-key)))

(defn save-application-review [review session organization-service]
  (check-application-access (:id review) session organization-service)
  (application-store/save-application-review review))
