(ns ataru.haku.haku-service
  (:require
   [ataru.organization-service.session-organizations :as session-orgs]
   [ataru.applications.application-store :as application-store]
   [ataru.forms.form-store :as form-store]
   [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-protocol]))

(defn- raw-haku-row->hakukohde
  [{:keys [hakukohde application-count processed processing]}]
  {:oid               hakukohde
   :application-count application-count
   :processed         processed
   :unprocessed       (- application-count processed processing)})

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
  [raw-hakukohde-rows]
  (for [[haku-oid rows] (group-by :haku raw-hakukohde-rows)]
    (let [haku-application-count (:haku-application-count (first rows))
          {:keys [processed processing total]} (haku-processed-counts rows)
          unprocessed            (- total processed processing)]
      {:oid                    haku-oid
       :hakukohteet            (map raw-haku-row->hakukohde rows)
       :haku-application-count haku-application-count
       :application-count      total
       :processed              processed
       :unprocessed            unprocessed})))

(defn- authorized-by-tarjoajat?
  [authorized-organization-oids tarjoajat haku]
  {:pre [(set? authorized-organization-oids)
         (some? (:haku haku))
         (some? (:hakukohde haku))]}
  (not-empty
   (clojure.set/intersection
    authorized-organization-oids
    (get tarjoajat (:hakukohde haku)))))

(defn- authorized-by-form?
  [authorized-organization-oids haku]
  {:pre [(set? authorized-organization-oids)
         (some? (:organization-oid haku))]}
  (contains? authorized-organization-oids
             (:organization-oid haku)))

(defn- hakujen-tarjoajat [tarjonta-service haut]
  (->> haut
       (map :hakukohde)
       distinct
       (tarjonta-protocol/get-hakukohteet tarjonta-service)
       (reduce #(assoc %1 (:oid %2) (set (:tarjoajaOids %2)))
               {})))

(defn- remove-organization-oid [haku]
  (dissoc haku :organization-oid))

(defn get-haut
  [session organization-service tarjonta-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   vector
   #(let [haut (application-store/get-haut)]
      (->> haut
           (filter (partial authorized-by-tarjoajat? % (hakujen-tarjoajat
                                                        tarjonta-service
                                                        haut)))
           handle-hakukohteet))
   #(handle-hakukohteet (application-store/get-haut))))

(defn get-direct-form-haut [session organization-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   vector
   #(->>(application-store/get-direct-form-haut)
        (filter (partial authorized-by-form? %))
        (map remove-organization-oid))
   #(map remove-organization-oid (application-store/get-direct-form-haut))))
