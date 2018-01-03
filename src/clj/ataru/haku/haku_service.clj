(ns ataru.haku.haku-service
  (:require
   [ataru.virkailija.user.session-organizations :as session-orgs]
   [ataru.applications.application-store :as application-store]
   [ataru.forms.form-store :as form-store]))

(defn- raw-haku-row->hakukohde [tarjonta-service raw-haku-row]
  (merge (select-keys raw-haku-row [:application-count :processed :haku])
         {:oid  (:hakukohde raw-haku-row)
          :name (or
                  (.get-hakukohde-name tarjonta-service (:hakukohde raw-haku-row))
                  {:fi (:hakukohde raw-haku-row)})}))

(defn- handle-haut [tarjonta-service raw-haku-rows]
  (for [[haku-oid rows] (group-by :haku raw-haku-rows)]
    {:oid               haku-oid
     :name              (or
                          (.get-haku-name tarjonta-service haku-oid)
                          {:fi haku-oid})
     :hakukohteet       (map (partial raw-haku-row->hakukohde tarjonta-service) rows)
     :application-count (:haku-application-count (first rows))
     :processed         (:haku-processed (first rows))}))

(defn get-haut [session organization-service tarjonta-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   vector
   #(handle-haut tarjonta-service (application-store/get-haut %))
   #(handle-haut tarjonta-service (application-store/get-all-haut))))

(defn get-direct-form-haut [session organization-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   vector
   #(application-store/get-direct-form-haut %)
   #(application-store/get-all-direct-form-haut)))
