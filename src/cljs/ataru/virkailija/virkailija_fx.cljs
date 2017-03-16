(ns ataru.virkailija.virkailija-fx
  (:require [ataru.virkailija.autosave :as autosave]
            [ataru.virkailija.virkailija-ajax :as http]
            [re-frame.core :as re-frame]))

(re-frame/reg-fx :http
  (fn [{:keys [method path params handler-or-dispatch]}]
    (case method
      :post (http/post path params handler-or-dispatch)
      (http/http method path handler-or-dispatch))))

(re-frame/reg-fx :stop-autosave
  (fn stop-autosave [autosave-fn]
    (autosave/stop-autosave! autosave-fn)))
