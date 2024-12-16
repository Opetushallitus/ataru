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
    [taoensso.timbre :as log]
    [ataru.tarjonta.haku :as haku]
    [ataru.user-rights :as user-rights]
    [ataru.applications.application-service :as application-service]
    [ataru.suoritus.suoritus-service :as suoritus-service]
    [ataru.applications.suoritus-filter :as suoritus-filter]
    [clojure.set :as set]))

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

(defn harkinnanvarainen-valinta-paattynyt?
  [ohjausparametrit-service now haku-oid]
  (if-let [hvvptp (some-> (ohjausparametrit/get-parametri ohjausparametrit-service haku-oid)
                    (get-in [:PH_HVVPTP :date])
                    c/from-long)]
    (t/after? now hvvptp)
    false))

(defn- remove-if-hakukierros-paattynyt
  [ohjausparametrit-service show-hakukierros-paattynyt? rows]
  (if show-hakukierros-paattynyt?
    rows
    (let [now  (t/now)
          hkp? (memoize (fn [haku-oid] (hakukierros-paattynyt? ohjausparametrit-service now haku-oid)))]
      (remove #(hkp? (:haku %)) rows))))

(defn- haut-with-hakukierros-paattynyt-removed
  [ohjausparametrit-service get-haut-cache show-hakukierros-paattynyt?]
  (->> (cache/get-from get-haut-cache :haut)
       (remove-if-hakukierros-paattynyt ohjausparametrit-service
         show-hakukierros-paattynyt?)))

(defn- keep-haut-authorized-by-form-or-hakukohde
  [tarjonta-service authorized-organization-oids haut]
  (->> haut
       (map (fn [h] (update h :hakukohde vector)))
       (aac/filter-authorized-by-form-or-hakukohde tarjonta-service authorized-organization-oids)
       (map (fn [h] (update h :hakukohde first)))))

(defn- toisen-asteen-yhteishaut-oids
  [tarjonta-service haku-authorized-by-form-or-hakukohde? haut]
  (->> haut
       (map :haku)
       distinct
       (remove haku-authorized-by-form-or-hakukohde?)
       (map (partial tarjonta/get-haku tarjonta-service))
       (filter haku/toisen-asteen-yhteishaku?)
       (map :oid)
       set))

(defn- haut-for-opinto-ohjaaja
  [tarjonta-service session haut-authorized-by-form-or-hakukohde haut]
  (if (user-rights/has-opinto-ohjaaja-right-for-any-organization? session)
    (let [haku-authorized-by-form-or-hakukohde? (set (map :haku haut-authorized-by-form-or-hakukohde))
          toisen-asteen-yhteishaku-oid?         (toisen-asteen-yhteishaut-oids
                                                  tarjonta-service
                                                  haku-authorized-by-form-or-hakukohde?
                                                  haut)]
      (filter (comp toisen-asteen-yhteishaku-oid? :haku) haut))
    []))

(defn- get-tarjonta-haut-for-ordinary-user
  [ohjausparametrit-service tarjonta-service get-haut-cache show-hakukierros-paattynyt? session authorized-organization-oids]
  (let [all-haut (haut-with-hakukierros-paattynyt-removed
                   ohjausparametrit-service
                   get-haut-cache
                   show-hakukierros-paattynyt?)
        haut     (if (user-rights/all-organizations-have-only-opinto-ohjaaja-rights? session)
                   (haut-for-opinto-ohjaaja
                     tarjonta-service
                     session
                     #{}
                     all-haut)
                   (let [haut-authorized-by-form-or-hakukohde (keep-haut-authorized-by-form-or-hakukohde
                                                                tarjonta-service
                                                                authorized-organization-oids
                                                                all-haut)
                         haut-for-opinto-ohjaaja              (haut-for-opinto-ohjaaja
                                                                tarjonta-service
                                                                session
                                                                haut-authorized-by-form-or-hakukohde
                                                                all-haut)]
                     (concat haut-for-opinto-ohjaaja haut-authorized-by-form-or-hakukohde)))]
    (handle-hakukohteet haut)))

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
   [:view-applications :edit-applications :opinto-ohjaaja]
   (constantly {})
   (partial get-tarjonta-haut-for-ordinary-user
     ohjausparametrit-service
     tarjonta-service
     get-haut-cache
     show-hakukierros-paattynyt?
     session)
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

(defn- add-kevyt-valinta-to-hakukohteet
  [hakukohteet-without-selection]
    (let [hakukohdeoids (map #(:oid %) hakukohteet-without-selection)
          hakukohde-oids-with-selection-state-used (hakukohde-store/selection-state-used-in-hakukohdes? hakukohdeoids)
          hakukohteet (->> hakukohteet-without-selection
             (map (fn [{hakukohde-oid :oid :as hakukohde}]
                    (assoc
                      hakukohde
                      :selection-state-used (some? (some #(= hakukohde-oid %) hakukohde-oids-with-selection-state-used)))))
             (util/group-by-first :oid))]
              hakukohteet))

(defn filter-and-count-hakukohteet-by-students
  [toisen-asteen-yhteishaut hakukohteet applications-persons-and-hakukohteet students-in-lahtokoulut]
    (let [toisen-asteen-yhteishaun-hakukohteet (filter #(contains? toisen-asteen-yhteishaut (:haku-oid %)) hakukohteet)
          allowed-applications-persons-and-hakukohteet (filter #(contains? students-in-lahtokoulut (:person_oid %))
                                                               applications-persons-and-hakukohteet)
          hakukohde-counts       (->> allowed-applications-persons-and-hakukohteet
                                      (mapcat :hakukohde)
                                      (reduce (fn [p n] (update p n #(+ 1 (or % 0)))) {}))
          allowed-hakukohde-oids (->> allowed-applications-persons-and-hakukohteet
                                      (mapcat :hakukohde)
                                      set)
          update-hakukohde-counts-fn (fn [hk] (assoc hk :application-count (get hakukohde-counts (:oid hk))))
          filtered-hakukohteet    (->> toisen-asteen-yhteishaun-hakukohteet
                                       (filter #(contains? allowed-hakukohde-oids (:oid %)))
                                       (map update-hakukohde-counts-fn))
          hakukohteet-by-haku-fn  (fn [haku] (filter #(= haku (:haku-oid %)) filtered-hakukohteet))
          count-applications      (fn [haku]
                                    (let [hakukohde-oidit (set (map :oid (:hakukohteet haku)))]
                                      (->> allowed-applications-persons-and-hakukohteet
                                           (filter #(not (= (count (:hakukohde %)) (count (set/difference (set (:hakukohde %)) hakukohde-oidit)))))
                                           (count))))]
          (->> toisen-asteen-yhteishaut
              (map (fn [haku] {:haku haku :hakukohteet (hakukohteet-by-haku-fn haku)}))
              (map #(assoc % :total (count-applications %))))))

(defn- limit-allowed-hakukohteet-for-opinto-ohjaaja
  [suoritus-service application-service hakukohteet haut lahtokoulut]
  (when-let [toisen-asteen-yhteishaut (->> (vals haut)
                                           (filter haku/toisen-asteen-yhteishaku?)
                                           (map :oid)
                                           set)]
    (let [hakuaika-end-years       (->> toisen-asteen-yhteishaut
                                        (map #(get haut %))
                                        (mapcat :hakuajat)
                                        (map #(suoritus-filter/year-for-suoritus-filter (:end %)))
                                        (distinct))
          persons-in-lahtokoulut (->> lahtokoulut
                                      (mapcat #(suoritus-service/oppilaitoksen-opiskelijat-useammalle-vuodelle
                                                 suoritus-service
                                                 %
                                                 hakuaika-end-years
                                                 (suoritus-filter/luokkatasot-for-suoritus-filter)))
                                      (map :person-oid)
                                      set)
          applications-persons-and-hakukohteet (mapcat
                                                 #(application-service/get-applications-persons-and-hakukohteet-by-haku application-service %)
                                                 toisen-asteen-yhteishaut)]
      (filter-and-count-hakukohteet-by-students toisen-asteen-yhteishaut hakukohteet applications-persons-and-hakukohteet persons-in-lahtokoulut))))

(defn- tarjonta-haut-with-hakukohteet-that-user-can-access
  [session
   organization-service
   suoritus-service
   application-service
   hakukohteet
   haut
   tarjonta-haut]
  (let [allowed-hakukohteet-with-counts (when
                                          (and (user-rights/has-opinto-ohjaaja-right-for-any-organization? session)
                                               (user-rights/all-organizations-have-only-opinto-ohjaaja-rights? session))
                                          (limit-allowed-hakukohteet-for-opinto-ohjaaja
                                            suoritus-service
                                            application-service
                                            hakukohteet
                                            haut
                                            (aac/organization-oids-for-opinto-ohjaaja organization-service session)))]
    (if allowed-hakukohteet-with-counts
      (util/map-kv
        tarjonta-haut
        (fn [haku]
          (let [haku-with-allowed (first (filter #(= (:haku %) (:oid haku)) allowed-hakukohteet-with-counts))
                hakukohteet-to-show
                (filter
                  #(some (fn [hk] (= (:oid %) (:oid hk))) (:hakukohteet haku-with-allowed))
                  (:hakukohteet haku))]
            (assoc haku :hakukohteet hakukohteet-to-show :haku-application-count (:total haku-with-allowed)))))
      tarjonta-haut)))

(def time-limit-to-fetch-haut 12)

(defn get-haut
  [ohjausparametrit-service
   organization-service
   tarjonta-service
   suoritus-service
   application-service
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
        direct-form-haut (get-direct-form-haut organization-service get-haut-cache session)
        haut (->> (keys tarjonta-haut)
                  (keep #(tarjonta/get-haku tarjonta-service %))
                  (util/group-by-first :oid))
        hakukohteet (->> (keys tarjonta-haut)
                      (mapcat #(tarjonta/hakukohde-search
                                 tarjonta-service
                                 %
                                 nil)))
        hakukohteet-with-kevyt-valinta (if show-hakukierros-paattynyt?
                                         (util/group-by-first :oid hakukohteet)
                                         (add-kevyt-valinta-to-hakukohteet hakukohteet))
        hakukohderyhmat (util/group-by-first
                          :oid
                          (filter :active? (organization-service/get-hakukohde-groups organization-service)))
        tarjonta-haut-with-updated-counts (tarjonta-haut-with-hakukohteet-that-user-can-access
                                            session
                                            organization-service
                                            suoritus-service
                                            application-service
                                            hakukohteet
                                            haut
                                            tarjonta-haut)
        duration (quot (- (System/currentTimeMillis) start-time) 1000)]

          (when (>= duration time-limit-to-fetch-haut)
            (log/warn "Duration of fetching haut is over the time limit, duration: " duration " s, limit: " time-limit-to-fetch-haut " s."))
          {:tarjonta-haut    tarjonta-haut-with-updated-counts
           :direct-form-haut direct-form-haut
           :haut             haut
           :hakukohteet      hakukohteet-with-kevyt-valinta
           :hakukohderyhmat  hakukohderyhmat}))


