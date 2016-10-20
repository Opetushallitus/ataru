(ns ataru.virkailija.core
  (:require [devtools.core :as devtools]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-frisk.core :as re-frisk]
            [ataru.virkailija.handlers]
            [ataru.virkailija.subs]
            [ataru.cljs-util :refer [set-global-error-handler!]]
            [ataru.virkailija.virkailija-fx] ; don't remove this, this is used to register all virkailija fx handlers
            [ataru.virkailija.virkailija-ajax :refer [post]]
            [ataru.virkailija.routes :as routes]
            [ataru.virkailija.views :as views]
            [ataru.virkailija.config :as config]
            [ataru.virkailija.editor.handlers]
            [taoensso.timbre :refer-macros [spy info]]
            [ataru.application-common.fx] ; ataru.application-common.fx must be required to have common fx handlers enabled
            [ataru.virkailija.virkailija-fx])) ; virkailija specific fx handlers

(enable-console-print!)

(when config/debug?
  (info "dev mode")
  (devtools/install!))

(defn mount-root []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (set-global-error-handler! #(post "/lomake-editori/api/client-error" % identity))
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (when (-> js/config
            js->clj
            (get "enable-re-frisk"))
    (re-frisk/enable-re-frisk!))
  (re-frame/dispatch [:editor/get-user-info])
  (mount-root))
