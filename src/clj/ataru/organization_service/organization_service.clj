(ns ataru.organization-service.organization-service
  (:require [clojure.string :as s]
            [com.stuartsierra.component :as component]
            [ataru.cache.cache-service :as cache]
            [ataru.config.core :refer [config]]
            [ataru.organization-service.organization-client :as org-client]))

(def group-oid-prefix "1.2.246.562.28")

(defn unknown-group [oid] {:oid oid :name {:fi "Tuntematon ryhmä"} :type :group})

(defprotocol OrganizationService
  (get-all-organizations [this direct-organizations-for-user]
    "Gets a flattened organization hierarhy based on direct organizations")
  (get-hakukohde-groups [this]
    "Gets all hakukohde groups")
  (get-organizations-for-oids [this organization-oids]))

(defn get-organization-hierarchies
  [organizations-hierarchy-cache direct-oids]
  (mapcat (fn [org-oid]
            (cache/get-from organizations-hierarchy-cache org-oid))
          direct-oids))

(defn group-oid? [oid] (clojure.string/starts-with? oid group-oid-prefix))

(defn- hakukohderyhmat-from-groups [groups]
  (let [hakukohde-groups (filter :hakukohderyhma? groups)]
    (map #(select-keys % [:oid :name :hakukohderyhma? :active?]) hakukohde-groups)))

;; The real implementation for Organization service
(defrecord IntegratedOrganizationService [organizations-hierarchy-cache all-organization-groups-cache]
  component/Lifecycle
  OrganizationService

  (get-hakukohde-groups [_]
    (->> (cache/get-from all-organization-groups-cache :dummy-key)
         vals
         hakukohderyhmat-from-groups))

  (get-all-organizations [_ direct-organizations]
    (let [[groups orgs]       ((juxt filter remove) #(group-oid? (:oid %)) direct-organizations)
          ;; Only fetch hierarchy for actual orgs, not groups:
          flattened-hierarchy (get-organization-hierarchies
                               organizations-hierarchy-cache
                               (map :oid orgs))]
      ;; Include groups as-is in the result:
      (concat groups flattened-hierarchy)))

  (get-organizations-for-oids [_ organization-oids]
    (let [[group-oids normal-org-oids] ((juxt filter remove) group-oid? organization-oids)
          normal-orgs (map org-client/get-organization-cached normal-org-oids)
          all-groups  (cache/get-from all-organization-groups-cache :dummy-key)
          groups      (map #(get all-groups % (unknown-group %)) group-oids)]
      (concat normal-orgs groups)))

  (start [this] this)

  (stop [this] this))

(def fake-org-by-oid
  {"1.2.246.562.10.11"                   {:name {:fi "Lasikoulu"}, :oid "1.2.246.562.10.11", :type :organization}
   "1.2.246.562.10.22"                   {:name {:fi "Omnia"}, :oid "1.2.246.562.10.22", :type :organization}
   "1.2.246.562.10.1234334543"           {:name {:fi "Telajärven aikuislukio"}, :oid "1.2.246.562.10.1234334543", :type :organization}
   "1.2.246.562.10.0439845"              {:name {:fi "Test org"}, :oid "1.2.246.562.10.0439845" :type :organization}
   "1.2.246.562.28.1"                    {:name {:fi "Test group"}, :oid "1.2.246.562.28.1" :type :group}
   "1.2.246.562.10.0439846"              {:name {:fi "Test org 2"}, :oid "1.2.246.562.10.0439846" :type :organization}
   "1.2.246.562.28.2"                    {:name {:fi "Test group 2"}, :oid "1.2.246.562.28.2" :type :group}
   "1.2.246.562.10.10826252480"          {:name {:fi "Testiorganisaatio"}, :oid "1.2.246.562.10.10826252480" :type :organization}
   "form-access-control-test-oppilaitos" {:name {:fi "Testioppilaitos"}, :oid "form-access-control-test-oppilaitos" :type :organization}
   "1.2.246.562.10.10826252479"          {:name {:fi "Tarjoajan oppilaitos"} :oid "1.2.246.562.10.10826252479" :type :organization}
   "1.2.246.562.10.00000000001"          {:name {:fi "Test OPH"}, :oid "1.2.246.562.10.00000000001" :type :organization}})

(defn fake-orgs-by-root-orgs [root-orgs]
  (some->> root-orgs
           (map :oid)
           (map name)
           (map #(get fake-org-by-oid %))))

;; Test double for UI tests
(defrecord FakeOrganizationService []
  OrganizationService

  (get-hakukohde-groups [_]
    (hakukohderyhmat-from-groups
      [(org-client/fake-hakukohderyhma 1)
       (org-client/fake-hakukohderyhma 2)
       (org-client/fake-hakukohderyhma 3)
       (org-client/fake-hakukohderyhma 4)]))

  (get-all-organizations [_ root-orgs]
    (fake-orgs-by-root-orgs root-orgs))

  (get-organizations-for-oids [_ organization-oids]
    (map fake-org-by-oid organization-oids)))

(defn new-organization-service []
  (if (-> config :dev :fake-dependencies) ;; Ui automated test mode
    (->FakeOrganizationService)
    (->IntegratedOrganizationService nil nil)))

(def oppilaitostyyppi-peruskoulut "oppilaitostyyppi_11#1")
(def oppilaitostyyppi-peruskouluasteen-erityiskoulut "oppilaitostyyppi_12#1")
(def oppilaitostyyppi-perus-ja-lukioasteen-koulut "oppilaitostyyppi_19#1")
(def oppilaitostyyppi-lukiot "oppilaitostyyppi_15#1")
(def oppilaitostyyppi-ammatilliset-oppilaitokset "oppilaitostyyppi_21#1")
(def oppilaitostyyppi-ammatilliset-erityisoppilaitokset "oppilaitostyyppi_22#1")
(def oppilaitostyyppi-kansanopistot "oppilaitostyyppi_63#1")
(def oppilaitostyyppi-kansalaisopistot "oppilaitostyyppi_64#1")

(defn is-suitable-as-lahtokoulu-for-toisen-asteen-yhteishaku?
  [organization]
  (and
    (#{oppilaitostyyppi-peruskoulut
       oppilaitostyyppi-peruskouluasteen-erityiskoulut
       oppilaitostyyppi-perus-ja-lukioasteen-koulut
       oppilaitostyyppi-lukiot
       oppilaitostyyppi-ammatilliset-oppilaitokset
       oppilaitostyyppi-ammatilliset-erityisoppilaitokset
       oppilaitostyyppi-kansanopistot
       oppilaitostyyppi-kansalaisopistot}
     (:oppilaitostyyppi organization))
    (some #(= "organisaatiotyyppi_02" %) (:organisaatiotyypit organization))))
