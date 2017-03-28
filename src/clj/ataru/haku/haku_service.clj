(ns ataru.haku.haku-service
  (:require
   [ataru.virkailija.user.session-organizations :as session-orgs]
   [ataru.applications.application-store :as application-store]
   [ataru.forms.form-store :as form-store]))

(defn- raw-haku-row->hakukohde [tarjonta-service raw-haku-row]
  (merge (select-keys raw-haku-row [:application-count :unprocessed])
         {:oid (:hakukohde raw-haku-row)
          :name (or
                 (-> tarjonta-service
                     (.get-hakukohde (:hakukohde raw-haku-row))
                     :hakukohteenNimet
                     :kieli_fi)
                 (:hakukohde raw-haku-row))}))

(defn- handle-haut [tarjonta-service raw-haku-rows]
  (for [[haku-oid rows] (group-by :haku raw-haku-rows)] ;; (def hautg (group-by :haku hauts))
    {:oid               haku-oid
     :name              (or
                         (-> tarjonta-service
                             (.get-haku haku-oid)
                             :nimi
                             :kieli_fi)
                         haku-oid)
     :hakukohteet       (map (partial raw-haku-row->hakukohde tarjonta-service) rows)
     :application-count (apply + (map :application-count rows))
     :unprocessed       (apply + (map :unprocessed rows))}))

(defn get-haut [session organization-service tarjonta-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   vector
   #(handle-haut tarjonta-service (application-store/get-haut2 %))
   #(handle-haut tarjonta-service (application-store/get-all-haut2))))

(defn- application-count->form [{:keys [key] :as form}]
  (assoc form :application-count (application-store/get-application-count-with-deleteds-by-form-key key)))

(defn- deleted-with-applications? [{:keys [application-count deleted]}]
  (or (not deleted)
      (> application-count 0)))

(defn get-forms-for-application-listing [session organization-service]
  {:forms (->> (session-orgs/run-org-authorized
                session
                organization-service
                [:view-applications :edit-applications]
                vector
                #(form-store/get-forms true %)
                #(form-store/get-all-forms true))
               (map #(application-count->form %))
               (filter deleted-with-applications?))})
