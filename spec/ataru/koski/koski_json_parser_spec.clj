(ns ataru.koski.koski-json-parser-spec
  (:require [speclj.core :refer :all]
            [ataru.koski.koski-json-parser :as parser]
            [cheshire.core :as json]))

(defn- read-opiskeluoikeudet-from-json [json-name]
  (:opiskeluoikeudet (json/parse-string (slurp (str "dev-resources/koski/" json-name)) true)))

(defn- levels [parsed]
  (distinct (map :level parsed)))


(describe "Parsing tutkinnot from Koski-opiskeluoikeudet"
          (tags :unit)
          (it "should parse perusopetus -tutkinnot from Koski-JSON"
              (let [parsed (parser/parse-koski-tutkinnot
                             ["perusopetus"] (read-opiskeluoikeudet-from-json "perusopetus.json"))
                    tutkinnot (filter #(= "perusopetus" (:level %)) parsed)]
                (should= 1 (count (levels parsed)))
                (should= 3 (count tutkinnot))
                (should= "1.2.246.562.10.67887034139_201101_2016-06-03" (:id (nth tutkinnot 0)))
                (should= "Perusopetus" (get-in (nth tutkinnot 0) [:tutkintonimi :fi]))
                (should= "1.2.246.562.10.81044480515_201101_2000-05-21" (:id (nth tutkinnot 1)))
                (should= "Aikuisten perusopetus" (get-in (nth tutkinnot 1) [:tutkintonimi :fi]))
                (should= "1.2.246.562.10.32727448402_201101_2019-05-21" (:id (nth tutkinnot 2)))
                (should= "Aikuisten perusopetus" (get-in (nth tutkinnot 2) [:tutkintonimi :fi]))))

          (it "should parse lukiokoulutukset from Koski-JSON"
              (let [parsed (parser/parse-koski-tutkinnot
                             ["lukiokoulutus"] (read-opiskeluoikeudet-from-json "ylioppilas.json"))
                    tutkinnot (filter #(= "lukiokoulutus" (:level %)) parsed)]
                (should= 1 (count (levels parsed)))
                (should= 1 (count tutkinnot))
                (should= "1.2.246.562.10.14613773812_309902_2016-06-08" (:id (nth tutkinnot 0)))
                (should= "Lukiokoulutus" (get-in (nth tutkinnot 0) [:tutkintonimi :fi]))))

          (it "should parse yo -tutkinnot from Koski-JSON"
              (let [parsed (parser/parse-koski-tutkinnot
                             ["yo"] (read-opiskeluoikeudet-from-json "ylioppilas.json"))
                    tutkinnot (filter #(= "yo" (:level %)) parsed)]
                (should= 1 (count (levels parsed)))
                (should= 3 (count tutkinnot))
                (should= "1.2.246.562.10.43628088406_301000_2012-06-02" (:id (nth tutkinnot 0)))
                (should= "Ylioppilastutkinto" (get-in (nth tutkinnot 0) [:tutkintonimi :fi]))
                (should= "1.2.246.562.10.45093614456_301103_2016-06-04" (:id (nth tutkinnot 1)))
                (should= "DIA-tutkinto" (get-in (nth tutkinnot 1) [:koulutusohjelmanimi :fi]))
                (should= "1.2.246.562.10.13349113236_301104_2023-06-15" (:id (nth tutkinnot 2)))
                (should= "EB-tutkinto (European Baccalaureate)" (get-in (nth tutkinnot 2) [:tutkintonimi :fi]))))

          (it "should parse ammatilliset tutkinnot from Koski-JSON"
              (let [parsed (parser/parse-koski-tutkinnot
                             ["amm" "amm-perus" "amm-erikois"] (read-opiskeluoikeudet-from-json "ammatilliset.json"))
                    amm (filter #(= "amm" (:level %)) parsed)
                    amm-perus (filter #(= "amm-perus" (:level %)) parsed)
                    amm-erikois (filter #(= "amm-erikois" (:level %)) parsed)]
                (should= 3 (count (levels parsed)))
                (should= 1 (count amm))
                (should= 1 (count amm-perus))
                (should= 1 (count amm-erikois))
                (should= "1.2.246.562.10.52251087186_354345_2023-08-30" (:id (nth amm 0)))
                (should= "Ammattitutkinto" (get-in (nth amm 0) [:koulutusohjelmanimi :fi]))
                (should= "1.2.246.562.10.56139411567_351301_2016-01-09" (:id (nth amm-perus 0)))
                (should= "Ammatillinen perustutkinto" (get-in (nth amm-perus 0) [:koulutusohjelmanimi :fi]))
                (should= "1.2.246.562.10.54019331674_437109_2020-11-16" (:id (nth amm-erikois 0)))
                (should= "Erikoisammattitutkinto" (get-in (nth amm-erikois 0) [:koulutusohjelmanimi :fi]))))

          (it "should parse korkeakoulu-tutkinnot from Koski-JSON"
              (let [parsed (parser/parse-koski-tutkinnot
                             ["kk-alemmat" "kk-ylemmat" "lisensiaatti" "tohtori"]
                             (read-opiskeluoikeudet-from-json "korkeakoulutukset.json"))
                    kk-alemmat (filter #(= "kk-alemmat" (:level %)) parsed)
                    kk-ylemmat (filter #(= "kk-ylemmat" (:level %)) parsed)
                    lisensiaatti (filter #(= "lisensiaatti" (:level %)) parsed)
                    tohtori (filter #(= "tohtori" (:level %)) parsed)]
                (should= 4 (count (levels parsed)))
                (should= 4 (count kk-alemmat))
                (should= 8 (count kk-ylemmat))
                (should= 1 (count lisensiaatti))
                (should= 1 (count tohtori))
                (should= ["1.2.246.562.10.38515028629_672501_2011-12-07" "1.2.246.562.10.78305677532_623404_2010-09-20"
                          "1.2.246.562.10.38515028629_642102_2010-12-10" "1.2.246.562.10.38515028629_633501_2013-04-15"]
                         (mapv :id kk-alemmat))
                (should= ["1.2.246.562.10.38515028629_772501_2014-08-06" "1.2.246.562.10.78305677532_726404_2012-06-20"
                          "1.2.246.562.10.38515028629_772501_2016-12-19" "1.2.246.562.10.38515028629_772401_2009-11-16"
                          "1.2.246.562.10.38515028629_672401_2006-05-17" "1.2.246.562.10.38515028629_733501_2014-10-07"
                          "1.2.246.562.10.57572539237_771218_2014-12-11" "1.2.246.562.10.38515028629_733203_2004-10-22"]
                         (mapv :id kk-ylemmat))
                (should= "1.2.246.562.10.38515028629_772201_2017-01-25" (:id (first lisensiaatti)))
                (should= "1.2.246.562.10.38515028629_875401_2017-10-17" (:id (first tohtori)))))

          (it "should return empty when returned tutkinnot not in requested level list"
              (let [parsed (parser/parse-koski-tutkinnot
                             ["perusopetus"] (read-opiskeluoikeudet-from-json "korkeakoulutukset.json"))]
                (should= true (empty? parsed))))

          (it "should return empty when koski response was empty"
              (let [parsed (parser/parse-koski-tutkinnot
                             ["perusopetus"] {:opiskeluoikeudet []})]
                (should= true (empty? parsed))))
          (it "should ignore incomplete data returned from koski"
              (let [parsed (parser/parse-koski-tutkinnot
                             ["perusopetus"] (read-opiskeluoikeudet-from-json "incomplete.json"))]
                (should= 1 (count (levels parsed)))
                (should= 1 (count parsed)))))
