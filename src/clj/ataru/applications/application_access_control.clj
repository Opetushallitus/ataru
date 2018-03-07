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
  (or (nil? authorized-organization-oids)
      (some? (:haku application))
      (contains? authorized-organization-oids
                 (:organization-oid application))))

(defn- authorized-by-tarjoajat?
  [authorized-organization-oids application]
  (or (nil? authorized-organization-oids)
      (nil? (:haku application))
      (not-empty
       (clojure.set/intersection
        authorized-organization-oids
        (apply clojure.set/union
               (map (comp set :tarjoajaOids) (:hakukohde application)))))))

(defn- populate-applications-hakukohteet
  [tarjonta-service applications]
  (let [hakukohteet (->> applications
                         (mapcat :hakukohde)
                         distinct
                         (tarjonta-service/get-hakukohteet tarjonta-service)
                         (reduce #(assoc %1 (:oid %2) %2) {}))]
    (map #(update % :hakukohde (partial mapv (fn [oid] (get hakukohteet oid {:oid oid}))))
         applications)))

(defn- depopulate-application-hakukohteet
  [application]
  (update application :hakukohde (partial mapv :oid)))

(defn- remove-organization-oid [application]
  (dissoc application :organization-oid))

(defn filter-authorized
  ([tarjonta-service authorized-organization-oids applications]
   (filter-authorized tarjonta-service
                      authorized-organization-oids
                      [authorized-by-form?
                       authorized-by-tarjoajat?]
                      applications))
  ([tarjonta-service authorized-organization-oids predicates applications]
   (->> applications
        (populate-applications-hakukohteet tarjonta-service)
        (filter (if (empty? predicates)
                  (constantly true)
                  (apply every-pred
                         (map #(partial % authorized-organization-oids)
                              predicates))))
        (map depopulate-application-hakukohteet)
        (map remove-organization-oid))))

(defn applications-access-authorized?
  [organization-service tarjonta-service session application-keys rights]
  (session-orgs/run-org-authorized
   session
   organization-service
   rights
   (constantly false)
   #(->> (application-store/applications-authorization-data application-keys)
         (populate-applications-hakukohteet tarjonta-service)
         (every? (every-pred (partial authorized-by-form? %)
                             (partial authorized-by-tarjoajat? %))))
   (constantly true)))

(defn get-application-list-by-query
  [organization-service tarjonta-service session query-key query-value predicates]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   (constantly [])
   #(filter-authorized tarjonta-service % (conj predicates
                                                authorized-by-form?
                                                authorized-by-tarjoajat?)
                       (application-store/get-application-heading-list
                        query-key
                        query-value))
   #(filter-authorized tarjonta-service nil predicates
                       (application-store/get-application-heading-list
                        query-key
                        query-value))))

(defn get-latest-application-by-key
  [organization-service tarjonta-service session application-key]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   (constantly nil)
   #(some->> (application-store/get-latest-application-by-key application-key)
             vector
             (filter-authorized tarjonta-service %)
             first)
   #(remove-organization-oid
     (application-store/get-latest-application-by-key application-key))))

(defn external-applications
  [organization-service tarjonta-service session haku-oid hakukohde-oid hakemus-oids]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    #(filter-authorized tarjonta-service %
                        (application-store/get-external-applications
                         haku-oid
                         hakukohde-oid
                         hakemus-oids))
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
