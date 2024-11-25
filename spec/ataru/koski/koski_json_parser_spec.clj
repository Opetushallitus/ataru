(ns ataru.koski.koski-json-parser-spec
  (:require [speclj.core :refer :all]
            [ataru.koski.koski-json-parser :as parser]
            [cheshire.core :as json]
            [schema.core :as s]
            [ataru.schema.koski-tutkinnot-schema :as ks]))

(defn- read-opiskeluoikeudet-from-json [json-name]
  (:opiskeluoikeudet (json/parse-string (slurp (str "dev-resources/koski/" json-name)) true)))

(describe "Parsing tutkinnot from Koski-opiskeluoikeudet"
          (tags :unit)
          (it "should parse perusopetus -tutkinnot from Koski-JSON"
              (let [parsed (parser/parse-koski-tutkinnot
                             (read-opiskeluoikeudet-from-json "perusopetus.json") ["perusopetus"])
                    tutkinnot (:perusopetus parsed)]
                (should= 1 (count (keys parsed)))
                (should= 3 (count tutkinnot))
                (should= "1.2.246.562.10.67887034139_201101_2016-06-03" (:id (get tutkinnot 0)))
                (should= "Perusopetus" (get-in (get tutkinnot 0) [:tutkintonimi :fi]))
                (should= "1.2.246.562.10.81044480515_201101_2000-05-21" (:id (get tutkinnot 1)))
                (should= "Aikuisten perusopetus" (get-in (get tutkinnot 1) [:tutkintonimi :fi]))
                (should= "1.2.246.562.10.32727448402_201101_2019-05-21" (:id (get tutkinnot 2)))
                (should= "Aikuisten perusopetus" (get-in (get tutkinnot 2) [:tutkintonimi :fi]))))

          (it "should parse yo -tutkinnot from Koski-JSON"
              (let [parsed (parser/parse-koski-tutkinnot
                             (read-opiskeluoikeudet-from-json "ylioppilas.json") ["yo"])
                    tutkinnot (:yo parsed)]
                (should= 1 (count (keys parsed)))
                (should= 3 (count tutkinnot))
                (should= "1.2.246.562.10.43628088406_301000_2012-06-02" (:id (get tutkinnot 0)))
                (should= "Ylioppilastutkinto" (get-in (get tutkinnot 0) [:tutkintonimi :fi]))
                (should= "1.2.246.562.10.45093614456_301103_2016-06-04" (:id (get tutkinnot 1)))
                (should= "DIA-tutkinto" (get-in (get tutkinnot 1) [:koulutusohjelmanimi :fi]))
                (should= "1.2.246.562.10.13349113236_301104_2023-06-15" (:id (get tutkinnot 2)))
                (should= "EB-tutkinto (European Baccalaureate)" (get-in (get tutkinnot 2) [:tutkintonimi :fi]))))

          (it "should parse ammatilliset tutkinnot from Koski-JSON"
              (let [parsed (parser/parse-koski-tutkinnot
                             (read-opiskeluoikeudet-from-json "ammatilliset.json") ["amm" "amm-perus" "amm-erikois"])
                    amm (:amm parsed)
                    amm-perus (:amm-perus parsed)
                    amm-erikois (:amm-erikois parsed)]
                (should= 3 (count (keys parsed)))
                (should= 1 (count amm))
                (should= 1 (count amm-perus))
                (should= 1 (count amm-erikois))
                (should= "1.2.246.562.10.52251087186_354345_2023-08-30" (:id (get amm 0)))
                (should= "Ammattitutkinto" (get-in (get amm 0) [:koulutusohjelmanimi :fi]))
                (should= "1.2.246.562.10.56139411567_351301_2016-01-09" (:id (get amm-perus 0)))
                (should= "Ammatillinen perustutkinto" (get-in (get amm-perus 0) [:koulutusohjelmanimi :fi]))
                (should= "1.2.246.562.10.54019331674_437109_2020-11-16" (:id (get amm-erikois 0)))
                (should= "Erikoisammattitutkinto" (get-in (get amm-erikois 0) [:koulutusohjelmanimi :fi]))))

          (it "should parse korkeakoulu-tutkinnot from Koski-JSON"
              (let [parsed (parser/parse-koski-tutkinnot
                             (read-opiskeluoikeudet-from-json "korkeakoulutukset.json") ["kk-alemmat" "kk-ylemmat" "tohtori"])
                    kk-alemmat (:kk-alemmat parsed)
                    kk-ylemmat (:kk-ylemmat parsed)
                    tohtori (:tohtori parsed)]
                (should= 3 (count (keys parsed)))
                (should= 4 (count kk-alemmat))
                (should= 8 (count kk-ylemmat))
                (should= 1 (count tohtori))
                (should= ["1.2.246.562.10.38515028629_672501_2011-12-07" "1.2.246.562.10.78305677532_623404_2010-09-20"
                          "1.2.246.562.10.38515028629_642102_2010-12-10" "1.2.246.562.10.38515028629_633501_2013-04-15"]
                         (mapv :id kk-alemmat))
                (should= ["1.2.246.562.10.38515028629_772501_2014-08-06" "1.2.246.562.10.78305677532_726404_2012-06-20"
                          "1.2.246.562.10.38515028629_772501_2016-12-19" "1.2.246.562.10.38515028629_772401_2009-11-16"
                          "1.2.246.562.10.38515028629_672401_2006-05-17" "1.2.246.562.10.38515028629_733501_2014-10-07"
                          "1.2.246.562.10.38515028629_772201_2017-01-25" "1.2.246.562.10.38515028629_733203_2004-10-22"]
                         (mapv :id kk-ylemmat))
                (should= "1.2.246.562.10.38515028629_875401_2017-10-17" (:id (first tohtori)))))

          (it "should return empty when returned tutkinnot not in requested level list"
              (let [parsed (parser/parse-koski-tutkinnot
                             (read-opiskeluoikeudet-from-json "korkeakoulutukset.json") ["perusopetus"])]
                (should= true (empty? parsed))))

          (it "should return empty when koski response was empty"
              (let [parsed (parser/parse-koski-tutkinnot
                             {:opiskeluoikeudet []} ["perusopetus"])]
                (should= true (empty? parsed))))
          (it "should ignore incomplete data returned from koski"
              (let [parsed (parser/parse-koski-tutkinnot
                             (read-opiskeluoikeudet-from-json "incomplete.json") ["perusopetus"])]
                (should= 1 (count (keys parsed)))
                (should= 1 (count (:perusopetus parsed))))))
