(ns ataru.virkailija.virkailija-fx
  (:require [ataru.virkailija.virkailija-ajax :as http]
            [re-frame.core :as re-frame]))

(re-frame/reg-fx :http
  (fn [{:keys [method path params handler-or-dispatch]}]
    (case method
      :post (http/post path params handler-or-dispatch)
      (http/http method path handler-or-dispatch))))
