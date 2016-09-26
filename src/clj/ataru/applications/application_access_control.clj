(ns ataru.applications.application-access-control
  (:require
   [ataru.forms.form-access-control :as form-access-control]
   [ataru.applications.application-store :as application-store]
   [ataru.middleware.user-feedback :refer [user-feedback-exception]]))

(defn check-form-access [form-key session]
  (when-not
      (form-access-control/form-allowed? form-key session)
    (throw (user-feedback-exception (str "Lomake " form-key " ei ole sallittu")))))

(defn get-application-list [form-key session]
  (check-form-access form-key session)
  {:applications (application-store/get-application-list form-key)})
