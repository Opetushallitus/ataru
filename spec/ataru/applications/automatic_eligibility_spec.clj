(ns ataru.applications.automatic-eligibility-spec
  (:require [ataru.applications.automatic-eligibility :as ae]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [speclj.core :refer :all]))

(defn input-gen
  [haku-gen ph-ahp-gen raw-now-gen ylioppilas?-gen]
  (gen/let [haku haku-gen
            hakukohde-oids (if (nil? haku)
                             (gen/return [])
                             (gen/fmap (fn [i]
                                         (map #(str "hakukohde-" % "-oid")
                                              (range i)))
                                       (gen/choose 1 10)))]
    (gen/hash-map
     :application (gen/hash-map :key (gen/return "application-key")
                                :person-oid (gen/return "person-oid")
                                :haku-oid (gen/return (:oid haku))
                                :hakukohde-oids (gen/return hakukohde-oids))
     :haku (gen/return haku)
     :ohjausparametrit (if (nil? haku)
                         (gen/return nil)
                         (gen/hash-map :PH_AHP ph-ahp-gen))
     :raw-now raw-now-gen
     :hakukohteet      (apply gen/tuple (map #(gen/hash-map :oid (gen/return %)
                                                            :ylioppilastutkintoAntaaHakukelpoisuuden gen/boolean)
                                             hakukohde-oids))
     :ylioppilas? ylioppilas?-gen
     :eligibility-automatically-set (apply gen/hash-map
                                           (mapcat #(vector % gen/boolean)
                                                   hakukohde-oids)))))

(def no-haku-input-gen
  (input-gen (gen/return nil)
             (gen/hash-map :date gen/nat)
             gen/nat
             gen/boolean))

(def haku-input-gen
  (input-gen (gen/return {:oid                                     "haku-oid"
                          :ylioppilastutkintoAntaaHakukelpoisuuden true})
             (gen/return {:date 2})
             (gen/return 1)
             gen/boolean))

(def automatic-eligibility-if-ylioppilas-not-in-use-input-gen
  (input-gen (gen/return {:oid                                     "haku-oid"
                          :ylioppilastutkintoAntaaHakukelpoisuuden false})
             (gen/hash-map :date gen/nat)
             gen/nat
             gen/boolean))

(def ph-ahp-passed-input-gen
  (input-gen (gen/hash-map :oid (gen/return "haku-oid")
                           :ylioppilastutkintoAntaaHakukelpoisuuden gen/boolean)
             (gen/return {:date 2})
             (gen/return 3)
             gen/boolean))

(def ylioppilas-input-gen
  (input-gen (gen/return {:oid                                     "haku-oid"
                          :ylioppilastutkintoAntaaHakukelpoisuuden true})
             (gen/return {:date 2})
             (gen/return 1)
             (gen/return true)))

(def not-ylioppilas-input-gen
  (input-gen (gen/return {:oid                                     "haku-oid"
                          :ylioppilastutkintoAntaaHakukelpoisuuden true})
             (gen/return {:date 2})
             (gen/return 1)
             (gen/return false)))

(defn- call-ae
  [inputs]
  (ae/automatic-eligibility-if-ylioppilas
   (:application inputs)
   (:haku inputs)
   (:ohjausparametrit inputs)
   (coerce/from-long (:raw-now inputs))
   (:hakukohteet inputs)
   (:ylioppilas? inputs)
   (:eligibility-automatically-set inputs)))

(defn- check [times prop]
  (let [result (tc/quick-check times prop)]
    (when-not (:result result)
      (let [input (-> result :shrunk :smallest first)]
        (-fail (str (with-out-str (clojure.pprint/pprint (list 'call-ae input))) "\n"
                    (with-out-str (clojure.pprint/pprint (call-ae input)))))))))

(describe "Automatic eligibility if ylioppilas"
  (tags :unit)

  (it "returns no updates when application has no haku-oid"
    (check 100 (prop/for-all [inputs no-haku-input-gen]
                 (empty? (call-ae inputs)))))

  (it "returns no updates when automatic eligibility if ylioppilas not in use"
    (check 100 (prop/for-all [inputs automatic-eligibility-if-ylioppilas-not-in-use-input-gen]
                 (empty? (call-ae inputs)))))

  (it "returns no updates when now is pass PH_AHP"
    (check 100 (prop/for-all [inputs ph-ahp-passed-input-gen]
                 (empty? (call-ae inputs)))))

  (it "returns only updates where eligibility automatically set for hakukohde when not ylioppilas"
    (check 100 (prop/for-all [inputs not-ylioppilas-input-gen]
                 (every? (fn [{:keys [hakukohde]}]
                           (get-in inputs [:eligibility-automatically-set
                                           (get-in inputs [:hakukohteet (:oid hakukohde)])]))
                         (call-ae inputs)))))

  (it "returns updates from unreviewed to eligible when ylioppilas"
    (check 100 (prop/for-all [inputs ylioppilas-input-gen]
                 (every? #(and (= "unreviewed" (:from %))
                               (= "eligible" (:to %)))
                         (call-ae inputs)))))

  (it "returns updates from eligible to unreviewed when not ylioppilas"
    (check 100 (prop/for-all [inputs not-ylioppilas-input-gen]
                 (every? #(and (= "eligible" (:from %))
                               (= "unreviewed" (:to %)))
                         (call-ae inputs)))))

  (it "returns updates only for hakukohteet where automatic eligibility if ylioppilas is in use"
    (check 100 (prop/for-all [inputs haku-input-gen]
                 (every? #(:ylioppilastutkintoAntaaHakukelpoisuuden (:hakukohde %))
                         (call-ae inputs)))))

  (it "returns application as given in updates"
    (check 100 (prop/for-all [inputs haku-input-gen]
                 (every? #(= (:application inputs) (:application %))
                         (call-ae inputs))))))
