(ns ataru.haku.haku-service
  (:require
   [ataru.organization-service.session-organizations :as session-orgs]
   [ataru.applications.application-store :as application-store]
   [ataru.forms.form-store :as form-store]))

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
  (reduce (fn [haut [haku-oid rows]]
            (let [haku-application-count               (:haku-application-count (first rows))
                  {:keys [processed processing total]} (haku-processed-counts rows)
                  unprocessed                          (- total processed processing)]
              (assoc haut haku-oid {:oid                    haku-oid
                                    :hakukohteet            (map raw-haku-row->hakukohde rows)
                                    :haku-application-count haku-application-count
                                    :application-count      total
                                    :processed              processed
                                    :unprocessed            unprocessed})))
          {}
          (group-by :haku raw-hakukohde-rows)))

(defn get-haut
  [session organization-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   vector
   #(handle-hakukohteet (application-store/get-haut %))
   #(handle-hakukohteet (application-store/get-all-haut))))

(defn get-direct-form-haut [session organization-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   vector
   #(application-store/get-direct-form-haut %)
   #(application-store/get-all-direct-form-haut)))
