(ns ataru.virkailija.virkailija-cofx
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-cofx
  :virkailija/scroll-y
  (fn [cofx]
    (let [scroll-y (.-scrollY js/window)]
      (assoc cofx :scroll-y scroll-y))))

(re-frame/reg-cofx
  :virkailija/resolve-url
  (fn [cofx {url-key :url-key target-key :target-key}]
    (let [url (.url js/window (name url-key))]
      (assoc cofx target-key url))))
