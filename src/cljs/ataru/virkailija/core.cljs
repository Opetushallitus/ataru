(ns ataru.virkailija.core
  (:require [reagent.core :as reagent]
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
            [ataru.virkailija.virkailija-fx] ; virkailija specific fx handlers
            [ataru.virkailija.views.banner :as banner]
            [ataru.virkailija.application.view :as app-handling-view]))

(enable-console-print!)

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))



(defn init-scroll-listeners []
  (let [debounces (atom {})
        debounce  (fn [id ms fn]
                    (js/clearTimeout (@debounces id))
                    (swap! debounces assoc id (js/setTimeout
                                                fn
                                                ms)))]
    (.addEventListener js/window "scroll" #(debounce 100 :banner (banner/create-banner-position-handler)))
    (.addEventListener js/window "scroll" #(debounce 100 :review (app-handling-view/create-review-position-handler)))
    (.addEventListener js/window "scroll" #(debounce 500 :paging (app-handling-view/create-application-paging-scroll-handler)))))

(defn ^:export init []
  (set-global-error-handler! #(post "/lomake-editori/api/client-error" % identity))
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (when (-> js/config
            js->clj
            (get "enable-re-frisk"))
    (re-frisk/enable-re-frisk!))
  (re-frame/dispatch [:editor/get-user-info])
  (re-frame/dispatch [:editor/do-organization-query])
  (mount-root)
  (init-scroll-listeners))
