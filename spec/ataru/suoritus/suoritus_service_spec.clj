(ns ataru.suoritus.suoritus-service-spec
  (:require [speclj.core :refer :all]
            [ataru.suoritus.suoritus-service :as suoritus-service]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [ataru.cache.cache-service :as cache]))

(def service (suoritus-service/map->HttpSuoritusService {}))
(def lahtokoulu1 {:oppilaitosOid "1.2.3" :alkuPaivamaara "2024-01-01" :loppuPaivamaara "2024-09-01" :luokka "9A"})
(def lahtokoulu2 {:oppilaitosOid "2.3.4" :alkuPaivamaara "2024-09-01" :loppuPaivamaara "2025-06-30" :luokka "9A"})
(def lahtokoulut-response {:lahtokoulut [lahtokoulu1 lahtokoulu2]})

(def test-jatkuva-haku {:hakutapa-uri "hakutapa_03"})
(def test-yhteishaku {:hakutapa-uri "joku-hakutapa"
                      :yhteishaku true})

(describe "suoritus-service-jatkuva-haku"
          (tags :unit :suoritus)
          (with-stubs)

          (around [spec]
                  (with-redefs [tarjonta/get-haku (stub :haku
                                                        {:return test-jatkuva-haku})
                                cache/get-from (stub :lahtokoulut
                                                     {:return lahtokoulut-response})]
                    (spec)))

          (it "palauttaa jatkuvalle haulle hakemuksen luomishetken lähtökoulun"
                        (let [data (suoritus-service/hakemuksen-lahtokoulut service {:created-time "2024-06-02T21:00:00.000Z"})]
                          (should= #{lahtokoulu1}
                                   data))))

(describe "suoritus-service-yhteishaku-haku"
          (tags :unit :suoritus)
          (with-stubs)

          (around [spec]
                  (with-redefs [tarjonta/get-haku (stub :haku
                                                        {:return test-yhteishaku})
                                cache/get-from (stub :lahtokoulut
                                                     {:return lahtokoulut-response})
                                suoritus-service/get-leikkuripvm (stub :lahtokoulut
                                                                       {:return "2024-09-02T21:00:00.000Z"})]
                    (spec)))

          (it "palauttaa yhteishaulle leikkupäivän lähtökoulun"
                        (let [data (suoritus-service/hakemuksen-lahtokoulut service {:created-time "2024-06-02T21:00:00.000Z"})]
                          (should= #{lahtokoulu2}
                                   data))))
