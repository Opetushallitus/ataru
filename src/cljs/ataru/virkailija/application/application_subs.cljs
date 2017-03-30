(ns ataru.virkailija.application.application-subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :application/list-heading
 (fn [db]
   (let [selected-haku       (get-in db [:application :selected-haku])
         selected-hakukohde  (get-in db [:application :selected-hakukohde])
         selected-form-key   (get-in db [:application :selected-form-key])
         forms               (get-in db [:application :forms])]
    (or (:name (get forms selected-form-key))
        (:name selected-hakukohde)
        (:name selected-haku)
        "Valitse haku/hakukohde"))))
