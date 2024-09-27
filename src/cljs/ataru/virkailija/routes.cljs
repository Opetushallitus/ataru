(ns ataru.virkailija.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:require [ataru.cljs-util :refer [dispatch-after-state]]
            [clojure.string]
            [secretary.core :as secretary]
            [re-frame.core :refer [dispatch]]
            [accountant.core :as accountant]))

(accountant/configure-navigation!
 {:nav-handler  (fn [path]
                  (secretary/dispatch! path))
  :path-exists? (fn [path]
                  (secretary/locate-route (first (clojure.string/split path #"\?"))))})

(defn set-history!
  [path]
  (accountant/navigate! path))

(defn navigate-to-click-handler
  [path & _]
  (when (secretary/locate-route (first (clojure.string/split path #"\?")))
    (set-history! path)))

(defn- select-editor-form-if-not-deleted
  [form]
  (if (:deleted form)
    (do
      (.replaceState js/history nil nil "/lomake-editori/editor")
      (secretary/dispatch! "/lomake-editori/editor"))
    (dispatch [:editor/select-form (:key form)])))

(defn common-actions []
  (dispatch [:application/get-virkailija-texts]))

(defn common-actions-for-applications-route []
  (dispatch [:set-active-panel :application])
  (dispatch [:application/get-virkailija-settings]))

(defn app-routes []
  (defroute "/lomake-editori/" []
    (secretary/dispatch! "/lomake-editori/editor"))

  (defroute "/lomake-editori/editor" []
    (common-actions)
    (dispatch [:set-active-panel :editor])
    (dispatch [:application/stop-loading-applications])
    (dispatch [:editor/select-form nil])
    (dispatch [:editor/refresh-forms-for-editor]))

  (defroute #"^/lomake-editori/editor/(.*)" [key]
    (common-actions)
    (dispatch [:set-active-panel :editor])
    (dispatch [:application/stop-loading-applications])
    (dispatch [:editor/refresh-forms-if-empty key])
    (dispatch-after-state
     :predicate
     (fn [db]
       (not-empty (get-in db [:editor :forms key])))
     :handler select-editor-form-if-not-deleted))

  (defroute #"^/lomake-editori/applications/?" []
    (secretary/dispatch! "/lomake-editori/applications/incomplete"))

  (defroute #"^/lomake-editori/applications/incomplete/" []
    (secretary/dispatch! "/lomake-editori/applications/incomplete"))

  (defroute #"^/lomake-editori/applications/incomplete" []
    (common-actions)
    (common-actions-for-applications-route)
    (dispatch [:application/stop-loading-applications])
    (dispatch [:application/refresh-haut-and-hakukohteet nil nil false []])
    (dispatch [:application/show-incomplete-haut-list]))

  (defroute #"^/lomake-editori/applications/complete/" []
    (secretary/dispatch! "/lomake-editori/applications/complete"))

  (defroute #"^/lomake-editori/applications/complete" []
    (common-actions)
    (common-actions-for-applications-route)
    (dispatch [:application/stop-loading-applications])
    (dispatch [:application/refresh-haut-and-hakukohteet nil nil false []])
    (dispatch [:application/show-complete-haut-list]))

  (defroute "/lomake-editori/applications/search/" []
    (secretary/dispatch! "/lomake-editori/applications/search"))

  (defroute "/lomake-editori/applications/search"
    [query-params]
    (common-actions)
    (dispatch [:set-active-panel :application])
    (dispatch [:application/show-search-term])
    (dispatch [:application/search-all-applications (or (:term query-params) "")]))

  (defroute "/lomake-editori/applications/hakukohde/:hakukohde-oid"
    [hakukohde-oid query-params]
    (common-actions)
    (common-actions-for-applications-route)
    (dispatch [:application/close-search-control])
    (dispatch [:application/set-filters-from-query])
    (dispatch [:application/select-hakukohde hakukohde-oid]))

  (defroute "/lomake-editori/applications/haku/:haku-oid/hakukohderyhma/:hakukohderyhma-oid"
    [haku-oid hakukohderyhma-oid query-params]
    (common-actions)
    (common-actions-for-applications-route)
    (dispatch [:application/close-search-control])
    (dispatch [:application/set-filters-from-query])
    (dispatch [:application/select-hakukohderyhma [haku-oid hakukohderyhma-oid]]))

  (defroute "/lomake-editori/applications/haku/:haku-oid"
    [haku-oid]
    (common-actions)
    (common-actions-for-applications-route)
    (dispatch [:application/close-search-control])
    (dispatch [:application/set-filters-from-query])
    (dispatch [:application/select-haku haku-oid]))

  (defroute "/lomake-editori/applications/:key"
    [key]
    (common-actions)
    (common-actions-for-applications-route)
    (dispatch [:application/close-search-control])
    (dispatch-after-state
      :predicate
        (fn [db]
          (not-empty (get-in db [:forms key])))
      :handler #(dispatch [:application/set-filters-from-query key]))
    (dispatch [:application/select-form key]))

  (defroute #"^/lomake-editori/virhe?"
    []
    (common-actions)
    (dispatch [:set-active-panel :error])
    (dispatch [:application/stop-loading-applications]))

  (accountant/dispatch-current!))
