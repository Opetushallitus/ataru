(ns ataru.applications.application-access-control
  (:require
   [ataru.virkailija.user.session-organizations :as session-orgs]
   [ataru.forms.form-access-control :as form-access-control]
   [ataru.applications.application-store :as application-store]
   [ataru.middleware.user-feedback :refer [user-feedback-exception]]
   [ataru.virkailija.user.organization-client :as organization-client]))

(defn check-form-access [form-key session organization-service]
  (when-not
    (form-access-control/form-allowed-by-key? form-key session organization-service)
    (throw (user-feedback-exception (str "Lomake " form-key " ei ole sallittu")))))

(defn check-form-access-by-id [form-id session organization-service]
  (when-not
    (form-access-control/form-allowed-by-id? form-id session organization-service)
    (throw (user-feedback-exception (str "Lomake " form-id " ei ole sallittu")))))

(defn check-forms-accesses [form-ids session organization-service]
  (doseq [form-id form-ids]
    (check-form-access-by-id form-id session organization-service)))

(defn check-application-access [application-key session organization-service]
  (when-not
    (session-orgs/organization-allowed?
      session
      organization-service
      #(application-store/get-application-organization-oid application-key))
    (throw (user-feedback-exception (str "Hakemus " application-key " ei ole sallittu")))))

(defn get-application-list-by-hakukohde [hakukohde-oid session organization-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   vector
   #(hash-map :applications (application-store/get-application-list-by-hakukohde hakukohde-oid %))
   #(hash-map :applications (application-store/get-full-application-list-by-hakukohde hakukohde-oid))))

(defn get-application-list-by-haku [haku-oid session organization-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   vector
   #(hash-map :applications (application-store/get-application-list-by-haku haku-oid %))
   #(hash-map :applications (application-store/get-full-application-list-by-haku haku-oid))))
