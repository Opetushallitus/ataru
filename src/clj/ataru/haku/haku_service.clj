(ns ataru.haku.haku-service
  (:require
   [ataru.util :as util]
   [ataru.organization-service.organization-service :as organization-service]
   [ataru.organization-service.session-organizations :as session-orgs]
   [ataru.applications.application-store :as application-store]
   [ataru.forms.form-store :as form-store]
   [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
   [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]))

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
  (util/map-kv
   (group-by :haku raw-hakukohde-rows)
   (fn [rows]
     (let [haku-application-count               (:haku-application-count (first rows))
           {:keys [processed processing total]} (haku-processed-counts rows)
           unprocessed                          (- total processed processing)]
       {:oid                    (:haku (first rows))
        :hakukohteet            (map raw-haku-row->hakukohde rows)
        :haku-application-count haku-application-count
        :application-count      total
        :processed              processed
        :unprocessed            unprocessed}))))

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
  (util/map-kv (->> (map :hakukohde haut)
                    distinct
                    (tarjonta/get-hakukohteet tarjonta-service)
                    (util/group-by-first :oid))
               (comp set :tarjoajaOids)))

(defn- remove-organization-oid [haku]
  (dissoc haku :organization-oid))

(defn- get-tarjonta-haut
  [organization-service tarjonta-service session]
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

(defn- get-direct-form-haut
  [organization-service session]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   vector
   #(->> (application-store/get-direct-form-haut)
         (filter (partial authorized-by-form? %))
         (map remove-organization-oid)
         (util/group-by-first :key))
   #(->> (application-store/get-direct-form-haut)
         (map remove-organization-oid)
         (util/group-by-first :key))))

(defn get-haut
  [organization-service tarjonta-service session]
  (let [tarjonta-haut (get-tarjonta-haut organization-service tarjonta-service session)]
    {:tarjonta-haut    tarjonta-haut
     :direct-form-haut (get-direct-form-haut organization-service session)
     :haut             (->> (keys tarjonta-haut)
                            distinct
                            (keep #(tarjonta/get-haku tarjonta-service %))
                            (map tarjonta-service/parse-haku)
                            (util/group-by-first :oid))
     :hakukohteet      (->> (keys tarjonta-haut)
                            distinct
                            (mapcat #(tarjonta/hakukohde-search tarjonta-service % nil))
                            (util/group-by-first :oid))
     :hakukohderyhmat  (util/group-by-first
                        :oid
                        (organization-service/get-hakukohde-groups organization-service))}))
