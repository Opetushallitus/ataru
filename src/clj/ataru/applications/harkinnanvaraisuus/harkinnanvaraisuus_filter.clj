(ns ataru.applications.harkinnanvaraisuus.harkinnanvaraisuus-filter
  (:require [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-types :as ht]))

(defn- harkinnanvarainen-application-id
  [hakukohteet-filter application-harkinnanvaraisuus]
  (let [hakutoiveet (:hakutoiveet application-harkinnanvaraisuus)]
    (when (->> hakutoiveet
               (some
                 (fn [hakutoive]
                   (and
                     (ht/harkinnanvarainen? (:harkinnanvaraisuudenSyy hakutoive))
                     (or
                       (nil? hakukohteet-filter)
                       (contains? hakukohteet-filter (:hakukohdeOid hakutoive)))))))
      (:hakemusOid application-harkinnanvaraisuus))))

(defn- filter-harkinnanvaraiset-applications
  [hakemusten-harkinnanvaraisuus-valintalaskennasta hakukohteet-filter applications]
  (let [application-keys      (map :key applications)
        harkinnanvaraisuus    (vals (hakemusten-harkinnanvaraisuus-valintalaskennasta application-keys))
        harkinnanvaraiset-ids (->> harkinnanvaraisuus
                                   (keep (partial harkinnanvarainen-application-id hakukohteet-filter))
                                   set)]
    (filter (comp harkinnanvaraiset-ids :key) applications)))

(defn filter-applications-by-harkinnanvaraisuus
  [hakemusten-harkinnanvaraisuus-valintalaskennasta applications filters]
  (let [only-harkinnanvaraiset? (-> filters :filters :harkinnanvaraisuus :only-harkinnanvaraiset)
        hakukohteet             (-> filters :selected-hakukohteet)]
    (if (and only-harkinnanvaraiset? (seq applications))
      (filter-harkinnanvaraiset-applications
        hakemusten-harkinnanvaraisuus-valintalaskennasta
        hakukohteet
        applications)
      applications)))
