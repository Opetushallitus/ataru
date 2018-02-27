(ns ataru.virkailija.virkailija-fx
  (:require [ataru.virkailija.autosave :as autosave]
            [ataru.virkailija.routes :as routes]
            [ataru.virkailija.virkailija-ajax :as http]
            [ataru.virkailija.tarjonta :as tarjonta]
            [ataru.virkailija.organization :as organization]
            [cljs.core.async :as async]
            [re-frame.core :as re-frame]))

(re-frame/reg-fx :http
  (fn [{:keys [method
               path
               params
               handler-or-dispatch
               skip-parse-times?
               handler-args]}]
    (case method
      :post
      (http/post path
                 params
                 handler-or-dispatch
                 :skip-parse-times? skip-parse-times?
                 :handler-args handler-args)

      (http/http method
                 path
                 handler-or-dispatch
                 :skip-parse-times? skip-parse-times?
                 :handler-args handler-args))))

(re-frame/reg-fx :stop-autosave
  (fn stop-autosave [autosave-fn]
    (autosave/stop-autosave! autosave-fn)))

(re-frame/reg-fx :navigate
  (fn navigate [path]
    (routes/navigate-to-click-handler path)))

(re-frame/reg-fx
 :fetch-haut-with-hakukohteet
 (fn fetch-haut-with-hakukohteet [[c organization-oids haku-oids]]
   (async/pipe (tarjonta/fetch-haut-with-hakukohteet haku-oids organization-oids) c)))

(re-frame/reg-fx
  :fetch-hakukohde-groups
  (fn fetch-hakukohde-groups [[c]]
    (organization/fetch-hakukohderyhmat c)))
