(ns ataru.cache.caches
  (:require [ataru.applications.application-store :as application-store]
            [ataru.cache.cache-service :as cache]
            [ataru.cache.in-memory-cache :as in-memory]
            [ataru.cache.two-layer-cache :as two-layer]
            [ataru.cache.redis-cache :as redis]
            [ataru.cache.union-cache :as union-cache]
            [ataru.forms.form-store :as form-store]
            [ataru.lokalisointi-service.lokalisointi-service :as lokalisointi-service]
            [ataru.suoritus.suorituspalvelu-client :as suorituspalvelu-client]
            [ataru.tarjonta-service.kouta.kouta-client :as kouta-client]
            [ataru.tarjonta-service.tarjonta-client :as tarjonta-client]
            [ataru.organization-service.organization-client :as organization-client]
            [ataru.ohjausparametrit.ohjausparametrit-client :as ohjausparametrit-client]
            [ataru.statistics.statistics-service :as stats]
            [ataru.koodisto.koodisto-db-cache :as koodisto-cache]
            [ataru.suoritus.suoritus-service :as suoritus-service]
            [ataru.config.core :refer [config]]
            [clojure.string :as s]
            [com.stuartsierra.component :as component]
            [ataru.cas.client :as cas])
  (:import java.util.concurrent.TimeUnit))

(def caches
  [[:get-haut-cache
    (in-memory/map->InMemoryCache
     {:name          "in-memory-get-haut"
      :loader        (cache/->FunctionCacheLoader
                      (fn [key]
                        (case key
                          :haut             (application-store/get-haut)
                          :direct-form-haut (application-store/get-direct-form-haut))))
      :expires-after [(get-in config [:cache :ttl-amounts :in-memory-get-haut] 3) TimeUnit/DAYS]
      :refresh-after [5 TimeUnit/MINUTES]})]
   [:organizations-hierarchy-cache
    (in-memory/map->InMemoryCache
     {:name          "in-memory-organizations-hierarchy"
      :loader        (cache/->FunctionCacheLoader
                      (fn [key] (organization-client/get-organizations key)))
      :expires-after [(get-in config [:cache :ttl-amounts :in-memory-organizations-hierarchy] 3) TimeUnit/DAYS]
      :refresh-after [60 TimeUnit/MINUTES]})]
   [:all-organization-groups-cache
    (in-memory/map->InMemoryCache
     {:name          "in-memory-all-organization-groups"
      :loader        (cache/->FunctionCacheLoader
                      (fn [_] (organization-client/get-groups)))
      :expires-after [(get-in config [:cache :ttl-amounts :in-memory-all-organization-groups] 3) TimeUnit/DAYS]
      :refresh-after [5 TimeUnit/MINUTES]})]
   [:localizations-cache
    (in-memory/map->InMemoryCache
     {:name          "in-memory-localizations"
      :loader        (cache/->FunctionCacheLoader lokalisointi-service/get-localizations)
      :expires-after [(get-in config [:cache :ttl-amounts :in-memory-localizations] 3) TimeUnit/DAYS]
      :refresh-after [5 TimeUnit/MINUTES]})]

   [:kouta-hakukohde-cache-loader
    (component/using
     (kouta-client/map->HakukohdeCacheLoader {})
     {:cas-client                    :kouta-internal-cas-client
      :organization-service          :organization-service
      :hakukohderyhmapalvelu-service :hakukohderyhmapalvelu-service
      :hakukohderyhma-settings-cache :hakukohderyhma-settings-cache})]
   [:hakukohderyhma-settings-cache-loader
    (component/using
      (kouta-client/map->HakukohderyhmaSettingsLoader {})
      {:hakukohderyhmapalvelu-service :hakukohderyhmapalvelu-service})]
   [:hakukohde-union-cache-loader
    (component/using
     (union-cache/map->CacheLoader
      {:low-priority-loader (cache/->FunctionCacheLoader tarjonta-client/get-hakukohde
                                                          tarjonta-client/hakukohde-checker)})
     {:high-priority-loader :kouta-hakukohde-cache-loader})]
   [:hakukohde-redis-cache
    (component/using
     (redis/map->Cache
      {:name          "hakukohde"
       :ttl           [(get-in config [:cache :ttl-amounts :hakukohde] 3) TimeUnit/DAYS]
       :refresh-after [15 TimeUnit/MINUTES]
       :lock-timeout  [20 TimeUnit/SECONDS]})
     {:redis  :redis
      :loader :hakukohde-union-cache-loader})]
   [:hakukohderyhma-settings-cache
    (component/using
      (redis/map->Cache
        {:name          "hakukohderyhma-settings"
         :ttl           [(get-in config [:cache :ttl-amounts :hakukohderyhma-settings] 3) TimeUnit/DAYS]
         :refresh-after [15 TimeUnit/MINUTES]
         :lock-timeout  [20 TimeUnit/SECONDS]})
      {:redis   :redis
       :loader  :hakukohderyhma-settings-cache-loader})]
   [:suoritusrekisteri-cas-client (cas/new-client "/suoritusrekisteri" "/j_spring_cas_security_check"
                                                 "JSESSIONID" (-> config :public-config :virkailija-caller-id))]
   [:oppilaitoksen-opiskelijat-cache-loader
    (component/using
      (suoritus-service/map->OppilaitoksenOpiskelijatCacheLoader {})
      {:cas-client    :suoritusrekisteri-cas-client})]
   [:oppilaitoksen-opiskelijat-cache
    (component/using
      (redis/map->Cache
        {:name          "oppilaitoksenopiskelijat"
         :ttl           [(get-in config [:cache :ttl-amounts :oppilaitoksenopiskelijat] 3) TimeUnit/DAYS]
         :refresh-after [12 TimeUnit/HOURS]
         :lock-timeout  [60 TimeUnit/SECONDS]})
      {:redis   :redis
       :loader  :oppilaitoksen-opiskelijat-cache-loader})]
   [:oppilaitoksen-luokat-cache-loader
    (component/using
      (suoritus-service/map->OppilaitoksenLuokatCacheLoader {})
      {:cas-client    :suoritusrekisteri-cas-client})]
   [:oppilaitoksen-luokat-cache
    (component/using
      (redis/map->Cache
        {:name          "oppilaitoksenluokat"
         :ttl           [(get-in config [:cache :ttl-amounts :oppilaitoksenluokat] 3) TimeUnit/DAYS]
         :refresh-after [12 TimeUnit/HOURS]
         :lock-timeout  [60 TimeUnit/SECONDS]})
      {:redis   :redis
       :loader  :oppilaitoksen-luokat-cache-loader})]

   [:hakukohde-cache
    (component/using
     (two-layer/map->Cache
      {:name                "in-memory-hakukohde"
       :expire-after-access [(get-in config [:cache :ttl-amounts :in-memory-hakukohde] 3) TimeUnit/DAYS]
       :refresh-after       [5 TimeUnit/MINUTES]})
     {:redis-cache :hakukohde-redis-cache})]
   [:kouta-internal-cas-client
    (cas/new-client "/kouta-internal" "/auth/login" "session" (-> config :public-config :virkailija-caller-id))]
   [:kouta-haku-cache-loader
    (component/using
     (kouta-client/map->CacheLoader {})
     {:cas-client :kouta-internal-cas-client})]
   [:haku-union-cache-loader
    (component/using
     (union-cache/map->CacheLoader
      {:low-priority-loader (cache/->FunctionCacheLoader
                              tarjonta-client/get-haku
                              tarjonta-client/haku-checker)})
     {:high-priority-loader :kouta-haku-cache-loader})]
   [:haku-redis-cache
    (component/using
     (redis/map->Cache
      {:name          "haku"
       :ttl           [(get-in config [:cache :ttl-amounts :haku] 3) TimeUnit/DAYS]
       :refresh-after [15 TimeUnit/MINUTES]
       :lock-timeout  [20 TimeUnit/SECONDS]})
     {:redis  :redis
      :loader :haku-union-cache-loader})]
   [:haku-cache
    (component/using
     (two-layer/map->Cache
      {:name                "in-memory-haku"
       :expire-after-access [(get-in config [:cache :ttl-amounts :in-memory-haku] 3) TimeUnit/DAYS]
       :refresh-after       [5 TimeUnit/MINUTES]})
     {:redis-cache :haku-redis-cache})]

   [:kouta-hakus-by-form-key-cache-loader
    (component/using
     (kouta-client/map->HakusByFormKeyCacheLoader {})
     {:cas-client :kouta-internal-cas-client})]
   [:kouta-hakus-by-form-key-redis-cache
    (component/using
     (redis/map->Cache
      {:name          "kouta-hakus-by-form-key"
       :ttl           [(get-in config [:cache :ttl-amounts :kouta-hakus-by-form-key] 3) TimeUnit/DAYS]
       :refresh-after [15 TimeUnit/MINUTES]
       :lock-timeout  [10 TimeUnit/SECONDS]})
     {:loader :kouta-hakus-by-form-key-cache-loader
      :redis  :redis})]
   [:kouta-hakus-by-form-key-cache
    (component/using
     (two-layer/map->Cache
      {:name                "in-memory-kouta-hakus-by-form-key"
       :expire-after-access [(get-in config [:cache :ttl-amounts :in-memory-kouta-hakus-by-form-key] 3) TimeUnit/DAYS]
       :refresh-after       [5 TimeUnit/MINUTES]})
     {:redis-cache :kouta-hakus-by-form-key-redis-cache})]

   [:forms-in-use-redis-cache
    (component/using
     (redis/map->Cache
      {:name          "forms-in-use"
       :ttl           [(get-in config [:cache :ttl-amounts :forms-in-use] 3) TimeUnit/DAYS]
       :refresh-after [15 TimeUnit/MINUTES]
       :lock-timeout  [20 TimeUnit/SECONDS]
       :loader        (cache/->FunctionCacheLoader tarjonta-client/get-forms-in-use)})
     [:redis])]
   [:forms-in-use-cache
    (component/using
     (two-layer/map->Cache
      {:name                "in-memory-forms-in-use"
       :size                10
       :expire-after-access [(get-in config [:cache :ttl-amounts :in-memory-forms-in-use] 3) TimeUnit/DAYS]
       :refresh-after       [5 TimeUnit/MINUTES]})
     {:redis-cache :forms-in-use-redis-cache})]

   [:ohjausparametrit-cache
    (component/using
     (redis/map->Cache
      {:name          "ohjausparametrit"
       :ttl           [(get-in config [:cache :ttl-amounts :ohjausparametrit] 3) TimeUnit/DAYS]
       :refresh-after [15 TimeUnit/MINUTES]
       :lock-timeout  [10 TimeUnit/SECONDS]
       :loader        (cache/->FunctionCacheLoader ohjausparametrit-client/get-ohjausparametrit)})
     [:redis])]

   [:koulutus-union-cache-loader
    (union-cache/map->CacheLoader
     {:low-priority-loader (cache/->FunctionCacheLoader tarjonta-client/get-koulutus
                                                         tarjonta-client/koulutus-checker)
      :high-priority-loader  (cache/->FunctionCacheLoader kouta-client/get-toteutus
                                                         kouta-client/toteutus-checker)})]
   [:koulutus-redis-cache
    (component/using
     (redis/map->Cache
      {:name          "koulutus"
       :ttl           [(get-in config [:cache :ttl-amounts :koulutus] 3) TimeUnit/DAYS]
       :refresh-after [15 TimeUnit/MINUTES]
       :lock-timeout  [20 TimeUnit/SECONDS]})
     {:redis  :redis
      :loader :koulutus-union-cache-loader})]
   [:koulutus-cache
    (component/using
     (two-layer/map->Cache
      {:name                "in-memory-koulutus"
       :expire-after-access [(get-in config [:cache :ttl-amounts :in-memory-koulutus] 3) TimeUnit/DAYS]
       :refresh-after       [7 TimeUnit/MINUTES]})
     {:redis-cache :koulutus-redis-cache})]

   [:henkilo-redis-cache
    (component/using
     (redis/map->Cache
      {:name          "henkilo"
       :ttl           [(get-in config [:cache :ttl-amounts :henkilo] 3) TimeUnit/DAYS]
       :refresh-after [1 TimeUnit/DAYS]
       :lock-timeout  [10 TimeUnit/SECONDS]})
     {:redis  :redis
      :loader :henkilo-cache-loader})]
   [:henkilo-cache
    (component/using
     (two-layer/map->Cache
      {:name                "in-memory-henkilo"
       :size                200000
       :expire-after-access [(get-in config [:cache :ttl-amounts :in-memory-henkilo] 3) TimeUnit/DAYS]
       :refresh-after       [1 TimeUnit/SECONDS]})
     {:redis-cache :henkilo-redis-cache})]

   [:kouta-hakukohde-search-cache-loader
    (component/using
     (kouta-client/map->HakukohdeSearchCacheLoader {})
     {:cas-client :kouta-internal-cas-client})]
   [:hakukohde-search-union-cache-loader
    (component/using
     (union-cache/map->CacheLoader
      {:low-priority-loader (cache/->FunctionCacheLoader
                              (fn [key]
                                (let [[haku-oid organization-oid] (s/split key #"#")]
                                  (tarjonta-client/hakukohde-search haku-oid organization-oid)))
                              tarjonta-client/hakukohde-search-checker)})
     {:high-priority-loader :kouta-hakukohde-search-cache-loader})]
   [:hakukohde-search-cache
    (component/using
     (redis/map->Cache
      {:name          "hakukohde-search"
       :ttl           [(get-in config [:cache :ttl-amounts :hakukohde-search] 3) TimeUnit/DAYS]
       :refresh-after [15 TimeUnit/MINUTES]
       :lock-timeout  [20 TimeUnit/SECONDS]})
     {:redis  :redis
      :loader :hakukohde-search-union-cache-loader})]

   [:statistics-month-cache
    (component/using
     (redis/map->Cache
      {:name         "statistics-month"
       :ttl          [(get-in config [:cache :ttl-amounts :statistics-month] 10) TimeUnit/HOURS]
       :lock-timeout [10 TimeUnit/SECONDS]
       :loader       (cache/->FunctionCacheLoader stats/get-and-parse-application-stats)})
     [:redis])]
   [:statistics-week-cache
    (component/using
     (redis/map->Cache
      {:name         "statistics-week"
       :ttl          [(get-in config [:cache :ttl-amounts :statistics-week] 1) TimeUnit/HOURS]
       :lock-timeout [10 TimeUnit/SECONDS]
       :loader       (cache/->FunctionCacheLoader stats/get-and-parse-application-stats)})
     [:redis])]
   [:statistics-day-cache
    (component/using
     (redis/map->Cache
      {:name         "statistics-day"
       :ttl          [(get-in config [:cache :ttl-amounts :statistics-day] 5) TimeUnit/MINUTES]
       :lock-timeout [10 TimeUnit/SECONDS]
       :loader       (cache/->FunctionCacheLoader stats/get-and-parse-application-stats)})
     [:redis])]
   [:koodisto-redis-cache
    (component/using
     (redis/map->Cache
      {:name          "koodisto"
       :ttl           [(get-in config [:cache :ttl-amounts :koodisto] 3) TimeUnit/DAYS]
       :refresh-after [15 TimeUnit/MINUTES]
       :lock-timeout  [60 TimeUnit/SECONDS]
       :loader        (cache/->FunctionCacheLoader koodisto-cache/get-koodi-options
                                                   koodisto-cache/koodisto-checker)})
     [:redis])]
   [:koodisto-cache
    (component/using
     (two-layer/map->Cache
      {:name                "in-memory-koodisto"
       :expire-after-access [3 TimeUnit/DAYS]
       :refresh-after       [7 TimeUnit/MINUTES]})
     {:redis-cache :koodisto-redis-cache})]
   [:form-by-id-cache
    (in-memory/map->InMemoryCache
     {:name          "in-memory-form-by-id"
      :loader        (cache/->FunctionCacheLoader
                      (fn [key] (form-store/fetch-by-id (Integer/valueOf key))))
      :size          10
      :expires-after [(get-in config [:cache :ttl-amounts :in-memory-form-by-id] 3) TimeUnit/DAYS]
      :refresh-after [1 TimeUnit/DAYS]})]
   [:form-by-haku-oid-str-cache
    (component/using
     (redis/map->Cache
      {:name          "form-by-haku-oid-str"
       :ttl           [(get-in config [:cache :ttl-amounts :form-by-haku-oid-str] 3) TimeUnit/DAYS]
       :refresh-after [5 TimeUnit/SECONDS]
       :lock-timeout  [1 TimeUnit/MINUTES]})
     {:redis  :redis
      :loader :form-by-haku-oid-str-cache-loader})]

   [:lahtokoulut-redis-cache
    (component/using
     (redis/map->Cache
      {:name          "lahtokoulut"
       :ttl           [(get-in config [:cache :ttl-amounts :lahtokoulut] 24) TimeUnit/HOURS]
       :refresh-after [30 TimeUnit/MINUTES]
       :lock-timeout  [60 TimeUnit/SECONDS]
       :loader        (cache/->FunctionCacheLoader #(suorituspalvelu-client/lahtokoulut %))})
      [:redis])]

   [:lahtokoulut-cache
    (component/using
     (two-layer/map->Cache
      {:name                "in-memory-lahtokoulut"
       :expire-after-access [24 TimeUnit/HOURS]
       :refresh-after       [15 TimeUnit/MINUTES]})
     {:redis-cache :lahtokoulut-redis-cache})]])