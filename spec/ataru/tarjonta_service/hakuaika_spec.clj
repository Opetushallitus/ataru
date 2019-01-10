(ns ataru.tarjonta-service.hakuaika-spec
  (:require [ataru.tarjonta-service.hakuaika :as hakuaika]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
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
        (-fail (with-out-str (clojure.pprint/pprint input)))))))

(defn- relevant-hakuajat [input]
  (let [oids (set (concat (:belongs-to-hakukohteet (:field-descriptor input))
                          (:belongs-to-hakukohderyhma (:field-descriptor input))))]
    (set
     (if (seq oids)
       (keep (fn [hakukohde]
               (when (seq (clojure.set/intersection
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
                    true)))))
