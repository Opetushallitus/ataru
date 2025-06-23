(ns ataru.person-service.person-service
  (:require [ataru.cache.cache-service :as cache]
            [ataru.config.core :refer [config]]
            [ataru.person-service.birth-date-converter :as bd-converter]
            [ataru.person-service.oppijanumerorekisteri-person-extract :as orpe]
            [ataru.person-service.person-client :as person-client]
            [ataru.person-service.person-util :as person-util]
            [clojure.string :as cs]
            [com.stuartsierra.component :as component]
            [ataru.date :as date]))

(defprotocol PersonService
  (create-or-find-person [this application]
    "Create or find a person in Oppijanumerorekisteri.")

  (get-persons [this oids]
    "Find multiple persons from Oppijanumerorekisteri.")

  (get-person [this oid]
    "Find a person from ONR.")

  (linked-oids [this oids]))

(defn- parse-onr-aidinkieli [person]
  (some-> person :aidinkieli :kieliKoodi cs/upper-case))

(defn- person-info-from-onr-person [person]
  (merge {:first-name     (:etunimet person)
          :preferred-name (:kutsumanimi person)
          :last-name      (:sukunimi person)
          :nationality    (->> (-> person :kansalaisuus)
                               (mapv #(vector (get % :kansalaisuusKoodi "999"))))}
         (let [birth-date (:syntymaaika person)]
           (when-not (cs/blank? birth-date)
             (let [finnish-birth-date (bd-converter/convert-to-finnish-format birth-date)
                   minor (date/minor? finnish-birth-date)]
               (cond-> {:birth-date finnish-birth-date}
                       (boolean? minor) (assoc :minor minor)))))
         (when-not (cs/blank? (:hetu person))
           {:ssn (:hetu person)})
         (when-not (cs/blank? (-> person :sukupuoli))
           {:gender (-> person :sukupuoli)})
         (let [aidinkieli (parse-onr-aidinkieli person)]
           (when-not (cs/blank? aidinkieli)
             {:language aidinkieli}))))

(defn parse-person [application person-from-onr]
  (let [yksiloity   (or (-> person-from-onr :yksiloity)
                        (-> person-from-onr :yksiloityVTJ))
        person-info (if yksiloity
                      (person-info-from-onr-person person-from-onr)
                      (person-util/person-info-from-application application))]
    (merge person-info
           (when (some? (:person-oid application))
             {:oid         (:person-oid application)
              :turvakielto (-> person-from-onr :turvakielto boolean)
              :yksiloity   (boolean yksiloity)}))))

(defn parse-person-with-master-oid [application person-from-onr]
  (let [person (parse-person application person-from-onr)
        master-oid (:oppijanumero person-from-onr)]
    (merge person {:master-oid master-oid})))

(defrecord IntegratedPersonService [henkilo-cache
                                    oppijanumerorekisteri-cas-client]
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  PersonService

  (create-or-find-person [_ application]
    (let [person (orpe/extract-person-from-application application)
          eidas-id (:eidas-id application)
          eidas-identification (when (some? eidas-id)
                                 [{:idpEntityId "eidas" :identifier eidas-id}])]
      (if (:eiSuomalaistaHetua person)
        (let [id (first (:identifications person))
              match-response (person-client/get-person-by-identification
                             oppijanumerorekisteri-cas-client id)
              match-person (:body match-response)]
          (if
            (and (= :found (:status match-response))
                 (= (:sukupuoli match-person) (:sukupuoli person))
                 (= (:syntymaaika match-person) (:syntymaaika person)))
            {:status :found-matching :oid (:oidHenkilo match-person)}
            (let [new-person (person-client/create-person
                               oppijanumerorekisteri-cas-client
                               person)]
              (if (= :not-found (:status match-response))
                (if (nil? eidas-id)
                  (do
                    (person-client/add-identification-to-person
                      oppijanumerorekisteri-cas-client
                      (:oid new-person) id)
                    {:status :created-with-email-id :oid (:oid new-person)})
                  (do
                    (person-client/add-identification-to-person
                      oppijanumerorekisteri-cas-client
                      (:oid new-person) eidas-identification)
                    {:status :created-with-eidas-id :oid (:oid new-person)}))
                {:status :dob-or-gender-conflict :oid (:oid new-person)}))))
        (let [response (person-client/create-or-find-person
                        oppijanumerorekisteri-cas-client
                        person)]
          (when (some? eidas-id)
            (person-client/add-identification-to-person
              oppijanumerorekisteri-cas-client
              (:oid response) eidas-identification))
          response))))

  (get-persons [_ oids] (cache/get-many-from henkilo-cache oids))

  (get-person [_ oid] (cache/get-from henkilo-cache oid))

  (linked-oids [_ oids]
    (person-client/linked-oids oppijanumerorekisteri-cas-client oids)))

(def fake-onr-person {:oidHenkilo   "1.2.3.4.5.6"
                      :hetu         "020202A0202"
                      :etunimet     "Testi"
                      :kutsumanimi  "Testi"
                      :sukunimi     "Ihminen"
                      :syntymaaika  "1941-06-16"
                      :sukupuoli    "2"
                      :kansalaisuus [{:kansalaisuusKoodi "246"}]
                      :aidinkieli   {:id          "742310"
                                     :kieliKoodi  "fi"
                                     :kieliTyyppi "suomi"}
                      :turvakielto  false
                      :yksiloity    false
                      :yksiloityVTJ false})

(defrecord FakePersonService []
  component/Lifecycle
  PersonService

  (start [this] this)
  (stop [this] this)

  (create-or-find-person [_ application]
   (condp = (:id application)
     "122221" {:oid "2.3.4.5.6.7" :status :found-matching}
     "133331" {:oid "3.4.5.6.7.8" :status :dob-or-gender-conflict}
     {:oid  "1.2.3.4.5.6" :status :created}))

  (get-persons [this oids]
    (reduce #(assoc %1 %2 (.get-person this %2))
            {}
            oids))

  (get-person [_ oid]
    (condp = oid
      "2.2.2" (merge fake-onr-person
                     {:oidHenkilo "2.2.2"
                      :turvakielto true
                      :yksiloity   true
                      :etunimet    "Ari"
                      :kutsumanimi "Ari"
                      :sukunimi    "Vatanen"
                      :hetu         "141196-933S"})
      "1.2.3.4.5.303" (merge fake-onr-person
                             {:kansalaisuus [{:kansalaisuusKoodi "784"}]
                              :yksiloity true
                              :yksiloityVTJ true})
      "1.2.3.4.5.808" (merge fake-onr-person
                             {:kansalaisuus [{:kansalaisuusKoodi "250"}]
                              :yksiloity true
                              :yksiloityVTJ true})
      "1.2.3.4.5.909" (merge fake-onr-person
                       {:kansalaisuus [{:kansalaisuusKoodi "250"}]
                        :yksiloity true
                        :yksiloityVTJ false})
      (merge fake-onr-person
             {:oidHenkilo oid})))

  (linked-oids [_ oids]
    (into {} (map (fn [x] {x {:master-oid x :linked-oids #{x (str x "2")}}}) oids))))

(defn new-person-service []
  (if (-> config :dev :fake-dependencies) ;; Ui automated test mode
    (->FakePersonService)
    (map->IntegratedPersonService {})))
