(ns ataru.haku.haku-service
  (:require
   [ataru.virkailija.user.session-organizations :as session-orgs]
   [ataru.applications.application-store :as application-store]
   [ataru.forms.form-store :as form-store]))

(defn- raw-haku-row->hakukohde
  [tarjonta-service {:keys [hakukohde application-count processed processing] :as raw-haku-row}]
  (merge (select-keys raw-haku-row [:application-count :processed :haku])
         {:oid         hakukohde
          :unprocessed (- application-count processed processing)
          :name        (or
                         (.get-hakukohde-name tarjonta-service hakukohde)
                         {:fi hakukohde})}))

(defn- haku-processed-counts
  [hakukohteet]
  (reduce
    (fn [{:keys [total processed processing]} hakukohde]
      {:processed  (+ processed (:processed hakukohde))
       :processing (+ processing (:processing hakukohde))
       :total      (+ total (:application-count hakukohde))})
    {:processed  0
     :processing 0
     :total      0}
    hakukohteet))

(defn- handle-hakukohteet
  [tarjonta-service raw-hakukohde-rows]
  (for [[haku-oid rows] (group-by :haku raw-hakukohde-rows)]
    (let [haku-application-count (:haku-application-count (first rows))
          {:keys [processed processing total]} (haku-processed-counts rows)
          unprocessed            (- total processed processing)]
      {:oid                    haku-oid
       :name                   (or
                                 (.get-haku-name tarjonta-service haku-oid)
                                 {:fi haku-oid})
       :hakukohteet            (map (partial raw-haku-row->hakukohde tarjonta-service) rows)
       :haku-application-count haku-application-count
       :application-count      total
       :processed              processed
       :unprocessed            unprocessed})))

(defn get-haut
  [session organization-service tarjonta-service]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    vector
    #(handle-hakukohteet tarjonta-service (application-store/get-haut %))
    #(handle-hakukohteet tarjonta-service (application-store/get-all-haut))))

(defn get-direct-form-haut [session organization-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   vector
   #(application-store/get-direct-form-haut %)
   #(application-store/get-all-direct-form-haut)))
