(ns ataru.applications.application-access-control
  (:require
   [ataru.organization-service.session-organizations :as session-orgs]
   [ataru.forms.form-access-control :as form-access-control]
   [ataru.applications.application-store :as application-store]
   [ataru.middleware.user-feedback :refer [user-feedback-exception]]
   [ataru.odw.odw-service :as odw-service]
   [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-service]))

(defn- authorized-by-form?
  [authorized-organization-oids application]
  {:pre [(set? authorized-organization-oids)
         (some? (:organization-oid application))]}
  (contains? authorized-organization-oids
             (:organization-oid application)))

(defn- authorized-by-tarjoajat?
  [authorized-organization-oids tarjoajat application]
  {:pre [(set? authorized-organization-oids)
         (some? (:haku application))
         (some? (:hakukohde application))]}
  (not-empty
   (clojure.set/intersection
    authorized-organization-oids
    (->> (:hakukohde application)
         (map tarjoajat)
         (apply clojure.set/union)))))

(defn authorized? [authorized-organization-oids tarjoajat application]
  (if (some? (:haku application))
    (authorized-by-tarjoajat? authorized-organization-oids
                              tarjoajat
                              application)
    (authorized-by-form? authorized-organization-oids
                         application)))

(defn applications-tarjoajat [tarjonta-service applications]
  (->> applications
       (mapcat :hakukohde)
       distinct
       (tarjonta-service/get-hakukohteet tarjonta-service)
       (reduce #(assoc %1 (:oid %2) (set (:tarjoajaOids %2)))
               {})))

(defn- filter-authorized-applications
  [tarjonta-service authorized-organization-oids applications]
  (let [tarjoajat (applications-tarjoajat tarjonta-service applications)]
    (filter (partial authorized? authorized-organization-oids tarjoajat)
            applications)))

(defn- remove-organization-oid [application]
  (dissoc application :organization-oid))

(defn applications-access-authorized?
  [organization-service tarjonta-service session application-keys rights]
  (session-orgs/run-org-authorized
   session
   organization-service
   rights
   (constantly false)
   #(let [affected-applications (application-store/applications-authorization-data application-keys)]
      (every? (partial authorized? % (applications-tarjoajat
                                      tarjonta-service
                                      affected-applications))
              affected-applications))
   (constantly true)))

(defn get-application-list-by-query
  [organization-service tarjonta-service session query-key query-value]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   (constantly [])
   #(->> (application-store/get-application-heading-list query-key query-value)
         (filter-authorized-applications tarjonta-service %)
         (map remove-organization-oid))
   #(map remove-organization-oid (application-store/get-application-heading-list query-key query-value))))

(defn get-latest-application-by-key
  [organization-service tarjonta-service session application-key]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   (constantly nil)
   #(when-let [application (application-store/get-latest-application-by-key application-key)]
      (when (authorized? % (applications-tarjoajat tarjonta-service [application]) application)
        (remove-organization-oid application)))
   #(remove-organization-oid (application-store/get-latest-application-by-key application-key))))

(defn external-applications
  [organization-service tarjonta-service session haku-oid hakukohde-oid hakemus-oids]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    #(->> (application-store/get-external-applications
           haku-oid
           hakukohde-oid
           hakemus-oids)
          (filter-authorized-applications tarjonta-service %)
          (map remove-organization-oid))
    #(map remove-organization-oid (application-store/get-external-applications
                                   haku-oid
                                   hakukohde-oid
                                   hakemus-oids))))

(defn hakurekisteri-applications [organization-service session haku-oid hakukohde-oids person-oids modified-after]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    (constantly nil)
    #(application-store/get-hakurekisteri-applications
       haku-oid
       hakukohde-oids
       person-oids
       modified-after)))

(defn application-key-to-person-oid [organization-service session haku-oid hakukohde-oids]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   (constantly nil)
   (constantly nil)
   #(application-store/get-person-and-application-oids
     haku-oid
     hakukohde-oids)))

(defn omatsivut-applications [organization-service session person-oid]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   (constantly nil)
   (constantly nil)
   #(application-store/get-full-application-list-by-person-oid-for-omatsivut-and-refresh-old-secrets
     person-oid)))

(defn onr-applications [organization-service session person-oid]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   (constantly nil)
   (constantly nil)
   #(application-store/onr-applications person-oid)))

(defn get-applications-for-odw [organization-service session person-service from-date]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    (constantly nil)
    #(odw-service/get-applications-for-odw person-service from-date)))

(defn get-applications-for-tilastokeskus [organization-service session haku-oid]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    (constantly nil)
    #(application-store/get-application-info-for-tilastokeskus haku-oid)))

(defn get-applications-for-valintalaskenta [organization-service session hakukohde-oid application-keys]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    (constantly nil)
    #(application-store/get-applications-for-valintalaskenta hakukohde-oid application-keys)))
