(ns ataru.virkailija.virkailija-cofx
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-cofx
  :virkailija/scroll-y
  (fn [cofx]
    (let [scroll-y (.-scrollY js/window)]
      (assoc cofx :scroll-y scroll-y))))