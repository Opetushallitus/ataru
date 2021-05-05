(ns ataru.haku.haku-service
  (:require
    [ataru.applications.application-access-control :as aac]
    [ataru.cache.cache-service :as cache]
    [ataru.util :as util]
    [ataru.ohjausparametrit.ohjausparametrit-protocol :as ohjausparametrit]
    [ataru.organization-service.organization-service :as organization-service]
    [ataru.organization-service.session-organizations :as session-orgs]
    [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
    [ataru.hakukohde.hakukohde-store :as hakukohde-store]
    [clj-time.core :as t]
    [clj-time.coerce :as c]
    [taoensso.timbre :as log]))

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

(defn- remove-organization-oid [haku]
  (dissoc haku :organization-oid))

(defn- hakukierros-paattynyt?
  [ohjausparametrit-service now haku-oid]
  (if-let [hkp (some-> (ohjausparametrit/get-parametri ohjausparametrit-service haku-oid)
                       (get-in [:PH_HKP :date])
                       c/from-long)]
    (t/after? now hkp)
    false))

(defn- remove-if-hakukierros-paattynyt
  [ohjausparametrit-service show-hakukierros-paattynyt? rows]
  (if show-hakukierros-paattynyt?
    rows
    (let [now  (t/now)
          hkp? (memoize (fn [haku-oid] (hakukierros-paattynyt? ohjausparametrit-service now haku-oid)))]
      (remove #(hkp? (:haku %)) rows))))

(defn- get-tarjonta-haut
  [ohjausparametrit-service
   organization-service
   tarjonta-service
   get-haut-cache
   session
   show-hakukierros-paattynyt?]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   (constantly {})
   #(->> (cache/get-from get-haut-cache :haut)
         (remove-if-hakukierros-paattynyt ohjausparametrit-service
                                          show-hakukierros-paattynyt?)
         (map (fn [h] (update h :hakukohde vector)))
         (aac/filter-authorized tarjonta-service
                                (some-fn (partial aac/authorized-by-form? %)
                                         (partial aac/authorized-by-tarjoajat? %)))
         (map (fn [h] (update h :hakukohde first)))
         handle-hakukohteet)
   #(->> (cache/get-from get-haut-cache :haut)
         (remove-if-hakukierros-paattynyt ohjausparametrit-service
                                          show-hakukierros-paattynyt?)
         (map remove-organization-oid)
         handle-hakukohteet)))

(defn- get-direct-form-haut
  [organization-service get-haut-cache session]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   (constantly {})
   #(->> (cache/get-from get-haut-cache :direct-form-haut)
         (filter (partial aac/authorized-by-form? %))
         (map remove-organization-oid)
         (util/group-by-first :key))
   #(->> (cache/get-from get-haut-cache :direct-form-haut)
         (map remove-organization-oid)
         (util/group-by-first :key))))

(defn- add-selection-to-hakukohteet
  [hakukohteet-without-selection]
    (let [hakukohdeoids (map #(:oid %) hakukohteet-without-selection)
          start-time (System/currentTimeMillis)
          hakukohde-oids-with-selection-state-used (hakukohde-store/selection-state-used-in-hakukohdes? hakukohdeoids)
          duration-db (quot (- (System/currentTimeMillis) start-time) 1000)
          hakukohteet (->> hakukohteet-without-selection
             (map (fn [{hakukohde-oid :oid :as hakukohde}]
                    (assoc
                      hakukohde
                      :selection-state-used (some?(some #(= hakukohde-oid %) hakukohde-oids-with-selection-state-used)))))
             (util/group-by-first :oid))
          duration-map (- (quot (System/currentTimeMillis) 1000) (+ start-time duration-db))]
            (log/info "Time taken to fetch oids " duration-db " s, total oids: " (count hakukohdeoids) " found: " (count hakukohde-oids-with-selection-state-used))
            (log/info "Time taken to map selection state used " duration-map " s")
              hakukohteet)
          )

(def time-limit-to-fetch-haut 7)

(defn get-haut
  [ohjausparametrit-service
   organization-service
   tarjonta-service
   get-haut-cache
   session
   show-hakukierros-paattynyt?]
  (let [start-time (System/currentTimeMillis)
        tarjonta-haut (get-tarjonta-haut ohjausparametrit-service
                                           organization-service
                                           tarjonta-service
                                           get-haut-cache
                                           session
                                           show-hakukierros-paattynyt?)
        time-after-tarjonta-haut (quot (- (System/currentTimeMillis) start-time) 1000)
        direct-form-haut (get-direct-form-haut organization-service get-haut-cache session)
        time-after-direct-form-haut (quot (- (System/currentTimeMillis) start-time) 1000)
        haut (->> (keys tarjonta-haut)
                  (keep #(tarjonta/get-haku tarjonta-service %))
                  (util/group-by-first :oid))
        time-after-haut (quot (- (System/currentTimeMillis) start-time) 1000)
        hakukohteet-without-selection (->> (keys tarjonta-haut)
                                            (mapcat #(tarjonta/hakukohde-search
                                                       tarjonta-service
                                                       %
                                                       nil)))
        time-after-hakukohteet-without (quot (- (System/currentTimeMillis) start-time) 1000)
        hakukohteet (add-selection-to-hakukohteet hakukohteet-without-selection)
        time-after-hakukohteet (quot (- (System/currentTimeMillis) start-time) 1000)
        hakukohderyhmat (util/group-by-first
                          :oid
                          (filter :active? (organization-service/get-hakukohde-groups organization-service)))
        time-after-hakukohderyhmat (quot (- (System/currentTimeMillis) start-time) 1000)
        duration (quot (- (System/currentTimeMillis) start-time) 1000)]
          (log/info "time-after-tarjonta-haut " time-after-tarjonta-haut)
          (log/info "time-after-direct-form-haut " time-after-direct-form-haut)
          (log/info "time-after-haut " time-after-haut)
          (log/info "time-after-hakukohteet-without " time-after-hakukohteet-without)
          (log/info "time-after-hakukohteet " time-after-hakukohteet)
          (log/info "time-after-hakukohderyhmat " time-after-hakukohderyhmat)

          (when (>= duration time-limit-to-fetch-haut)
            (log/warn "Duration of fetching haut is over the time limit, duration: " duration " s, limit: " time-limit-to-fetch-haut " s."))
          {:tarjonta-haut    tarjonta-haut
           :direct-form-haut direct-form-haut
           :haut             haut
           :hakukohteet      hakukohteet
           :hakukohderyhmat  hakukohderyhmat}))
