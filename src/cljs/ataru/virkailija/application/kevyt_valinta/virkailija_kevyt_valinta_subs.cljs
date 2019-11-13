(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-subs
  (:require [re-frame.core :as re-frame]
            [ataru.feature-config :as fc]))

(defn- valintalaskenta-in-hakukohteet
  [db]
  (->> (get-in db [:application :selected-review-hakukohde-oids])
       (map #(get-in db [:application :valintalaskentakoostepalvelu % :valintalaskenta]))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/show-selection-state-dropdown?
  (fn [db _]
    (or (not (fc/feature-enabled? :kevyt-valinta))
        ;; true?, koska nil tarkoittaa ettei tietoa ole vielä ladattu
        ;; backendiltä ja nil? palauttaisi väärän positiivisen tiedon
        (every? true? (valintalaskenta-in-hakukohteet db)))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/show-kevyt-valinta?
  (fn [db _]
    (let [haku-oid (get-in db [:application :selected-application-and-form :application :haku])]
      (and (fc/feature-enabled? :kevyt-valinta)
           (not (get-in db [:haut haku-oid :sijoittelu]))
           ;; false?, koska nil tarkoittaa ettei tietoa ole vielä ladattu
           ;; backendiltä ja nil? palauttaisi väärän positiivisen tiedon
           (every? false? (valintalaskenta-in-hakukohteet db))))))
