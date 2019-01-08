(ns ataru.applications.automatic-eligibility-spec
  (:require [ataru.applications.automatic-eligibility :as ae]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [speclj.core :refer :all]))

(defn- ->ohjausparametrit-gen
  [haku]
  (if (nil? haku)
    (gen/return nil)
    (gen/one-of [(gen/return {})
                 (gen/return {:PH_AHP nil})
                 (gen/return {:PH_AHP {:date nil}})
                 (gen/fmap #(hash-map :PH_AHP (hash-map :date %))
                           (gen/choose 1 3))])))

(def input-gen
  (gen/let [haku (gen/one-of [(gen/return nil)
                              (gen/hash-map
                               :oid (gen/return "haku-oid")
                               :ylioppilastutkintoAntaaHakukelpoisuuden gen/boolean)])
            hakukohde-oids (if (nil? haku)
                             (gen/return [])
                             (gen/fmap (fn [i]
                                         (map #(str "hakukohde-" % "-oid")
                                              (range i)))
                                       (gen/choose 1 6)))
            yah?           (gen/vector gen/boolean (count hakukohde-oids))]
    (gen/hash-map
     :application      (gen/hash-map :key (gen/return "application-key")
                                     :person-oid (gen/return "person-oid")
                                     :haku-oid (gen/return (:oid haku))
                                     :hakukohde-oids (gen/return hakukohde-oids))
     :haku             (gen/return haku)
     :ohjausparametrit (->ohjausparametrit-gen haku)
     :now              (gen/fmap coerce/from-long (gen/choose 1 3))
     :hakukohteet      (gen/return (map #(hash-map :oid %1
                                                   :ylioppilastutkintoAntaaHakukelpoisuuden %2)
                                        hakukohde-oids
                                        yah?))
     :suoritus?        gen/boolean)))

(defn- call-ae
  [inputs]
  (ae/automatic-eligibility-if-ylioppilas
   (:application inputs)
   (:haku inputs)
   (:ohjausparametrit inputs)
   (:now inputs)
   (:hakukohteet inputs)
   (:suoritus? inputs)))

(defn- check [times prop]
  (let [result (tc/quick-check times prop)]
    (when-not (:result result)
      (let [input (-> result :shrunk :smallest first)]
        (-fail (str (with-out-str (clojure.pprint/pprint (list 'call-ae input))) "\n"
                    (with-out-str (clojure.pprint/pprint (call-ae input)))))))))

(describe "Automatic eligibility"
  (tags :unit)

  (it "returns no updates when application has no haku-oid"
    (check 100 (prop/for-all [inputs input-gen]
                 (if (nil? (:haku inputs))
                   (empty? (call-ae inputs))
                   true))))

  (it "returns no updates when automatic eligibility if ylioppilas not in use"
    (check 100 (prop/for-all [inputs input-gen]
                 (if (false? (get-in inputs [:haku :ylioppilastutkintoAntaaHakukelpoisuuden]))
                   (empty? (call-ae inputs))
                   true))))

  (it "returns updates only for hakukohteet where automatic eligibility if ylioppilas is in use"
    (check 100 (prop/for-all [inputs input-gen]
                 (every? #(:ylioppilastutkintoAntaaHakukelpoisuuden (:hakukohde %))
                         (call-ae inputs)))))

  (it "returns no updates when now is pass PH_AHP"
    (check 100 (prop/for-all [inputs input-gen]
                 (if (some->> (get-in inputs [:ohjausparametrit :PH_AHP :date])
                              coerce/from-long
                              (time/after? (:now inputs)))
                   (empty? (call-ae inputs))
                   true))))

  (it "returns updates from unreviewed to eligible when ylioppilas or ammatillinen"
    (check 100 (prop/for-all [inputs input-gen]
                 (if (:suoritus? inputs)
                   (every? #(and (= "unreviewed" (:from %))
                                 (= "eligible" (:to %)))
                           (call-ae inputs))
                   true))))

  (it "returns updates from eligible to unreviewed when not ylioppilas or ammatillinen"
    (check 100 (prop/for-all [inputs input-gen]
                 (if (not (:suoritus? inputs))
                   (every? #(and (= "eligible" (:from %))
                                 (= "unreviewed" (:to %)))
                           (call-ae inputs))
                   true))))

  (it "returns application as given in updates"
    (check 100 (prop/for-all [inputs input-gen]
                 (every? #(= (:application inputs) (:application %))
                         (call-ae inputs))))))
