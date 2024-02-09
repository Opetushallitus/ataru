(ns ataru.virkailija.core
  (:require [reagent.dom :as reagent-dom]
            [re-frame.core :as re-frame]
            ataru.virkailija.application.hyvaksynnan-ehto.handlers
            [ataru.virkailija.handlers]
            [ataru.virkailija.subs]
            [ataru.cljs-util :refer [set-global-error-handler!]]
            [ataru.virkailija.virkailija-fx] ; don't remove this, this is used to register all virkailija fx handlers
            [ataru.virkailija.virkailija-cofx]
            [ataru.virkailija.virkailija-ajax :refer [post]]
            [ataru.virkailija.routes :as routes]
            [ataru.virkailija.views :as views]
            [ataru.virkailija.editor.handlers]
            [ataru.virkailija.views.banner :as banner]
            [ataru.virkailija.application.view :as app-handling-view]
            [ataru.schema-validation :as schema-validation]))

(enable-console-print!)

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (re-frame/dispatch [:application/get-virkailija-texts-from-server])
  (reagent-dom/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn init-scroll-listeners []
  (let [debounces (atom {})
        debounce  (fn [ms id fn]
                    (js/clearTimeout (@debounces id))
                    (swap! debounces assoc id (js/setTimeout
                                                fn
                                                ms)))]
    (.addEventListener js/window "scroll" (banner/create-banner-position-handler))
    (.addEventListener js/window "scroll" #(debounce 500 :paging (app-handling-view/create-application-paging-scroll-handler)))))

(defn ^:export init []
  (schema-validation/enable-schema-fn-validation)
  (set-global-error-handler! #(post "/lomake-editori/api/client-error" % identity)
                             (constantly "Virkailija lomake-editori"))
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (re-frame/dispatch [:editor/get-user-info])
  (re-frame/dispatch [:hyvaksynnan-ehto/get-koodit])
  (re-frame/dispatch [:editor/do-organization-query])
  (mount-root)
  (init-scroll-listeners))
