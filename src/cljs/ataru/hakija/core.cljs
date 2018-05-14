(ns ataru.hakija.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-frisk.core :as re-frisk]
            [taoensso.timbre :refer-macros [spy info]]
            [ataru.cljs-util :as cljs-util]
            [ataru.hakija.hakija-ajax :refer [post]]
            [ataru.hakija.application-view :refer [form-view]]
            [ataru.hakija.application-handlers] ;; required although no explicit dependency
            [ataru.hakija.application-hakukohde-handlers] ;; required although no explicit dependency
            [ataru.hakija.subs] ;; required although no explicit dependency
            [ataru.application-common.fx] ; ataru.application-common.fx must be required to have common fx handlers enabled
            [ataru.cljs-util :as cljs-util]
            [ataru.util :as u]
            [clojure.string :as str]))

(enable-console-print!)

(defn- path-match
  [path re]
  (when-let [re-match (re-matches re path)]
    (nth re-match 1)))

(defn- dispatch-form-load
  []
  (let [path              (cljs-util/get-path)
        hakukohde-oid     (path-match path #"/hakemus/hakukohde/(.+)/?")
        haku-oid          (path-match path #"/hakemus/haku/(.+)/?")
        form-key          (path-match path #"/hakemus/(.+)/?")
        query-params      (cljs-util/extract-query-params)
        hakija-secret     (:modify query-params)
        virkailija-secret (:virkailija-secret query-params)]
    (cond
      (u/not-blank? hakukohde-oid)
      (re-frame/dispatch [:application/get-latest-form-by-hakukohde hakukohde-oid virkailija-secret])

      (u/not-blank? haku-oid)
      (re-frame/dispatch [:application/get-latest-form-by-haku haku-oid virkailija-secret])

      (u/not-blank? form-key)
      (re-frame/dispatch [:application/get-latest-form-by-key form-key virkailija-secret])

      (u/not-blank? hakija-secret)
      (re-frame/dispatch [:application/get-application-by-hakija-secret hakija-secret])

      (u/not-blank? virkailija-secret)
      (re-frame/dispatch [:application/get-application-by-virkailija-secret virkailija-secret]))))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [form-view]
                  (.getElementById js/document "app")))

(defn- re-frisk-environment?
  []
  (let [cfg (js->clj js/config)]
    (or (get cfg "enable-re-frisk")
        (= (get cfg "environment-name") "luokka"))))

(defn ^:export init []
  (cljs-util/set-global-error-handler! #(post "/hakemus/api/client-error" %))
  (mount-root)
  (re-frame/dispatch-sync [:application/initialize-db])
  (when (re-frisk-environment?)
    (re-frisk/enable-re-frisk!))
  (dispatch-form-load))
