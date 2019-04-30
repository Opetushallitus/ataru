(ns ataru.forms.hakukohderyhmat-spec
  (:require [ataru.forms.hakukohderyhmat :as h]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [clj-time.core :as time]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [speclj.core :refer :all]))

(defn- prioriteetin-hakukohteet
  [i n]
  (map #(str "hakukohde-oid-" i "-" %) (range n)))

(defn- ->hakukohde
  [oid kuuluu?]
  {:oid            oid
   :ryhmaliitokset (if kuuluu?
                     ["hakukohderyhma-oid"]
                     [])})

(def priorisoiva-hakukohderyhma-gen
  (gen/let [prioriteetit (gen/fmap #(map-indexed prioriteetin-hakukohteet %)
                                   (gen/vector gen/nat))
            hakukohde-oids (gen/return (mapcat identity prioriteetit))
            kuuluu (gen/vector (gen/frequency [[9 (gen/return true)]
                                               [1 (gen/return false)]])
                               (count hakukohde-oids))]
    (gen/hash-map
     :hakukohteet (gen/return (map ->hakukohde hakukohde-oids kuuluu))
     :ryhma (gen/hash-map
             :haku-oid (gen/return "haku-oid")
             :hakukohderyhma-oid (gen/return "hakukohderyhma-oid")
             :prioriteetit (gen/return prioriteetit)
             :last-modified (gen/return (time/now))))))

(describe "Hakukohderyhmät"
  (tags :unit)

  (it "returns excatly the hakukohteet belonging to the hakukohderyhmä"
    (let [expected (fn [{:keys [hakukohteet]}]
                     (keep #(when (not-empty (:ryhmaliitokset %))
                              (:oid %))
                           hakukohteet))
          result   (fn [{:keys [hakukohteet ryhma]}]
                     (with-redefs [h/db-exec                (fn [_ _] [ryhma])
                                   tarjonta/get-hakukohteet (fn [_ _] hakukohteet)]
                       (->> (h/priorisoivat-hakukohderyhmat nil "haku-oid")
                            :ryhmat
                            first
                            :prioriteetit
                            (mapcat identity))))
          r        (tc/quick-check 100 (prop/for-all [input priorisoiva-hakukohderyhma-gen]
                                         (= (expected input) (result input))))]
      (when-not (:result r)
        (let [input (-> r :shrunk :smallest first)]
          (should= (expected input) (result input))))))

  (it "does not return empty priorities"
    (let [result (fn [{:keys [hakukohteet ryhma]}]
                   (with-redefs [h/db-exec                (fn [_ _] [ryhma])
                                 tarjonta/get-hakukohteet (fn [_ _] hakukohteet)]
                     (->> (h/priorisoivat-hakukohderyhmat nil "haku-oid")
                          :ryhmat
                          first
                          :prioriteetit)))
          r      (tc/quick-check 100 (prop/for-all [input priorisoiva-hakukohderyhma-gen]
                                       (every? not-empty (result input))))]
      (when-not (:result r)
        (let [input (-> r :shrunk :smallest first)]
          (should-not-contain [] (result input)))))))
