(ns ataru.applications.application-service
  (:require
   [ataru.applications.application-access-control :as aac]
   [ataru.forms.form-access-control :as form-access-control]
   [ataru.forms.form-store :as form-store]
   [ataru.koodisto.koodisto :as koodisto]
   [ataru.applications.application-store :as application-store]
   [ataru.middleware.user-feedback :refer [user-feedback-exception]]
   [ataru.applications.excel-export :as excel]))

(defn get-application-list [form-key session organization-service]
  (aac/check-form-access form-key session organization-service)
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
                             (let [koodisto-uri (get-in koodisto-fields [key :uri])
                                   version      (get-in koodisto-fields [key :version])
                                   koodisto     (koodisto/get-koodisto-options koodisto-uri version)
                                   value        (->> (clojure.string/split koodi-value #"\s*,\s*")
                                                     (map (fn [koodi-uri]
                                                            (let [koodi (get-koodi koodisto koodi-uri)]
                                                              (get-in koodi [:label lang])))))]
                               (cond-> value
                                 (= (count value) 1)
                                 first))))))))))

(defn get-application [application-id session organization-service]
  (let [application (application-store/get-application application-id)
        form        (form-store/fetch-by-id (:form application))
        application (populate-koodisto-fields application form)]
    (aac/check-application-access application-id session organization-service)
    {:application application
     :form        form
     :events      (application-store/get-application-events application-id)
     :review      (application-store/get-application-review application-id)}))

(defn get-excel-report-of-applications [form-key session organization-service]
  (aac/check-form-access form-key session organization-service)
  (java.io.ByteArrayInputStream. (excel/export-all-applications form-key)))

(defn save-application-review [review session organization-service]
  (aac/check-application-access (:id review) session organization-service)
  (application-store/save-application-review review))
