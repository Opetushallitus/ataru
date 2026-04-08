(ns ataru.tarjonta-service.hakuaika-spec
  (:require [ataru.tarjonta-service.hakuaika :as hakuaika]
            [clj-time.coerce :as coerce]
            [clj-time.core :as t]
            [clojure.pprint :refer [pprint]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.set :as set]
            [speclj.core :refer :all]))

(defn- hakuaika-gen [now]
  (gen/let [start (gen/frequency [[8 (gen/large-integer* {:min (max 0 (- now 10)) :max (dec now)})]
                                  [2 (gen/return now)]
                                  [8 (gen/large-integer* {:min now :max (+ now 10)})]])
            end (gen/frequency [[1 (gen/return nil)]
                                [9 (gen/large-integer* {:min (inc start) :max (+ start 10)})]])]
    (gen/return
     {:start start
      :end   end
      :on    (hakuaika/hakuaika-on (coerce/from-long now) start end)})))

(defn hakukohteet-gen [hakuajat hakukohde-oids hakukohderyhma-oids]
  (gen/let [hakuajat (gen/vector (gen/elements hakuajat)
                                 (count hakukohde-oids))
            hakukohderyhmat (gen/vector (if (empty? hakukohderyhma-oids)
                                          (gen/return [])
                                          (gen/vector-distinct
                                           (gen/elements hakukohderyhma-oids)))
                                        (count hakukohde-oids))]
    (gen/return
     (map #(hash-map :oid %1 :hakuaika %2 :hakukohderyhmat %3)
          hakukohde-oids
          hakuajat
          hakukohderyhmat))))

(defn field-descriptor-gen [hakukohde-oids hakukohderyhma-oids]
  (gen/hash-map
   :belongs-to-hakukohteet (gen/vector-distinct (gen/elements hakukohde-oids))
   :belongs-to-hakukohderyhma (if (empty? hakukohderyhma-oids)
                                (gen/return [])
                                (gen/vector-distinct (gen/elements hakukohderyhma-oids)))))

(defn input-gen [now]
  (gen/let [hakukohde-oids (gen/not-empty (gen/set (gen/fmap #(str "hakukohde-oid-" %) gen/nat)))
            hakukohderyhma-oids (gen/set (gen/fmap #(str "hakukohderyhma-oid-" %) gen/nat))
            hakuajat (gen/not-empty (gen/set (hakuaika-gen now)))
            hakukohteet (hakukohteet-gen hakuajat hakukohde-oids hakukohderyhma-oids)]
    (gen/hash-map
     :hakukohteet (gen/return hakukohteet)
     :field-descriptor (field-descriptor-gen
                        hakukohde-oids
                        (distinct (mapcat :hakukohderyhmat hakukohteet))))))

(defn- check [times prop]
  (let [result (tc/quick-check times prop)]
    (when-not (:result result)
      (let [input (-> result :shrunk :smallest first)]
        (-fail (with-out-str (pprint input)))))))

(defn- relevant-hakuajat [input]
  (let [oids (set (concat (:belongs-to-hakukohteet (:field-descriptor input))
                          (:belongs-to-hakukohderyhma (:field-descriptor input))))]
    (set
     (if (seq oids)
       (keep (fn [hakukohde]
               (when (seq (set/intersection
                           oids
                           (set (cons (:oid hakukohde)
                                      (:hakukohderyhmat hakukohde)))))
                 (:hakuaika hakukohde)))
             (:hakukohteet input))
       (map :hakuaika (:hakukohteet input))))))

(describe "Hakuaika"
  (tags :unit)

  (it "should pick field specific hakuaika"
    (check 100 (prop/for-all [input (input-gen 1000)]
                 (contains? (relevant-hakuajat input)
                            (hakuaika/select-hakuaika-for-field
                             (coerce/from-long 1000)
                             (:field-descriptor input)
                             (hakuaika/index-hakuajat (:hakukohteet input)))))))

  (it "should pick open hakuaika, if one is present"
    (check 100 (prop/for-all [input (input-gen 1000)]
                  (if (some :on (relevant-hakuajat input))
                    (:on (hakuaika/select-hakuaika-for-field
                          (coerce/from-long 1000)
                          (:field-descriptor input)
                          (hakuaika/index-hakuajat (:hakukohteet input))))
                    true))))

  (it "should pick last ended hakuaika if one is present and none is open"
    (check 100 (prop/for-all [input (input-gen 1000)]
                 (let [hakuajat   (relevant-hakuajat input)
                       paattyneet (filter #(hakuaika/ended? (t/now) %) hakuajat)]
                   (if (and (every? #(not (:on %)) hakuajat)
                            (not-empty paattyneet))
                     (= (hakuaika/select-hakuaika-for-field
                         (coerce/from-long 1000)
                         (:field-descriptor input)
                         (hakuaika/index-hakuajat (:hakukohteet input)))
                        (max-key :end paattyneet))
                     true)))))

  (it "hakuaika should be on with haku"
      (should= true (:on (let [now (t/now)
                     haku {:hakuajat [{:start (t/minus now (t/days 2)) :end (t/plus now (t/days 2))}]}]
                 (hakuaika/haun-hakuaika-end-and-on haku)))))

  (it "hakuaika should not be on with haku"
      (should= false (:on (let [now (t/now)
                               haku {:hakuajat [{:start (t/minus now (t/days 4)) :end (t/minus now (t/days 2))}]}]
                           (hakuaika/haun-hakuaika-end-and-on haku)))))

  (it "should find active hakuaika from haku"
      (should= true (:on (let [now (t/now)
                                haku {:hakuajat [{:start (t/minus now (t/days 4)) :end (t/minus now (t/days 2))}
                                                 {:start (t/minus now (t/hours 2)) :end (t/plus now (t/hours 2))}
                                                 {:start (t/minus now (t/hours 4)) :end (t/minus now (t/hours 2))}]}]
                            (hakuaika/haun-hakuaika-end-and-on haku)))))

  (it "should not find active hakuaika from haku"
      (should= false (:on (let [now (t/now)
                               haku {:hakuajat [{:start (t/minus now (t/days 4)) :end (t/minus now (t/days 2))}
                                                {:start (t/minus now (t/hours 4)) :end (t/minus now (t/hours 2))}]}]
                           (hakuaika/haun-hakuaika-end-and-on haku))))))

(describe "Localized datetime"
          (tags :unit)

          (it "Returns datetime in expected format for fi during Winter time"
              (let [datetime (t/date-time 2024 10 30 9)]
                (should= "30.10.2024 klo 11:00" (hakuaika/date-timez->localized-date-time datetime :fi))))

          (it "Returns datetime in expected format for fi during Summer time"
              (let [datetime (t/date-time 2024 6 30 9)]
                (should= "30.6.2024 klo 12:00" (hakuaika/date-timez->localized-date-time datetime :fi))))

          (it "Returns datetime in expected format for se during Winter time"
              (let [datetime (t/date-time 2024 10 30 9)]
                (should= "30.10.2024 kl. 11:00 UTC+2" (hakuaika/date-timez->localized-date-time datetime :sv))))

          (it "Returns datetime in expected format for se during Summer time"
              (let [datetime (t/date-time 2024 6 30 9)]
                (should= "30.6.2024 kl. 12:00 UTC+3" (hakuaika/date-timez->localized-date-time datetime :sv))))

          (it "Returns datetime in expected format for en during Winter time"
              (let [datetime (t/date-time 2024 10 30 9)]
                (should= "Oct. 30, 2024 at 11:00 AM UTC+2" (hakuaika/date-timez->localized-date-time datetime :en))))

          (it "Returns datetime in expected format for en during Summer time"
              (let [datetime (t/date-time 2024 6 30 9)]
                (should= "Jun. 30, 2024 at 12:00 PM UTC+3" (hakuaika/date-timez->localized-date-time datetime :en)))))

(describe "Localized time"
          (tags :unit)

          (it "Returns datetime in expected format for fi during Winter time"
              (let [datetime (t/date-time 2024 10 30 9)]
                (should= "klo 11:00" (hakuaika/date-timez->localized-time datetime :fi))))

          (it "Returns datetime in expected format for fi during Summer time"
              (let [datetime (t/date-time 2024 6 30 9)]
                (should= "klo 12:00" (hakuaika/date-timez->localized-time datetime :fi))))

          (it "Returns datetime in expected format for se during Winter time"
              (let [datetime (t/date-time 2024 10 30 9)]
                (should= "kl. 11:00 UTC+2" (hakuaika/date-timez->localized-time datetime :sv))))

          (it "Returns datetime in expected format for se during Summer time"
              (let [datetime (t/date-time 2024 6 30 9)]
                (should= "kl. 12:00 UTC+3" (hakuaika/date-timez->localized-time datetime :sv))))

          (it "Returns datetime in expected format for en during Winter time"
              (let [datetime (t/date-time 2024 10 30 9)]
                (should= "at 11:00 AM UTC+2" (hakuaika/date-timez->localized-time datetime :en))))

          (it "Returns datetime in expected format for en during Summer time"
              (let [datetime (t/date-time 2024 6 30 9)]
                (should= "at 12:00 PM UTC+3" (hakuaika/date-timez->localized-time datetime :en)))))
