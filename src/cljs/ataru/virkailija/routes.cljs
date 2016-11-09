(ns ataru.virkailija.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import [goog Uri])
  (:require [ataru.cljs-util :refer [dispatch-after-state]]
            [secretary.core :as secretary]
            [re-frame.core :refer [dispatch]]
            [accountant.core :as accountant]))

(accountant/configure-navigation! {:nav-handler  (fn [path]
                                                   (secretary/dispatch! path))
                                   :path-exists? (fn [path]
                                                   (secretary/locate-route path))})

(defn set-history!
  [path]
  (accountant/navigate! path))

(defn anchor-click-handler
  [event]
  (.preventDefault event)
  (let [path (.getPath (.parse Uri (.-href (.-target event))))
        matches-path? (secretary/locate-route path)]
    (when matches-path?
      (set-history! path))))

(defn app-routes []
  (defroute "/lomake-editori/" []
    (secretary/dispatch! "/lomake-editori/editor"))

  (defroute "/lomake-editori/editor" []
    (dispatch [:set-active-panel :editor])
    (dispatch [:editor/select-form nil])
    (dispatch [:editor/refresh-forms])
    (dispatch [:editor/refresh-forms-in-use]))

  (defroute #"^/lomake-editori/editor/(.*)" [key]
    (dispatch [:set-active-panel :editor])
    (dispatch [:editor/refresh-forms])
    (dispatch [:editor/refresh-forms-in-use])
    (dispatch-after-state
     :predicate
     (fn [db]
       (not-empty (get-in db [:editor :forms key])))
     :handler
     (fn [form]
       (dispatch [:editor/select-form (:key form)]))))

  (defroute #"^/lomake-editori/applications/" []
    (dispatch [:editor/refresh-forms-with-deleteds])
    (dispatch [:editor/refresh-hakukohteet-from-applications])
    (dispatch-after-state
     :predicate
     (fn [db] (not-empty (get-in db [:editor :forms])))
     :handler
     (fn [forms]
       (let [form (-> forms first val)]
         (.replaceState js/history nil nil (str "/lomake-editori/applications/" (:key form)))
         (dispatch [:editor/select-form (:key form)])
         (dispatch [:application/fetch-applications (:key form)])))
     (dispatch [:set-active-panel :application])))

  (defroute #"^/lomake-editori/applications/hakukohde/(.*)" [hakukohde-oid]
    (dispatch [:editor/refresh-hakukohteet-from-applications])
    (dispatch [:editor/refresh-forms-with-deleteds])
    (dispatch-after-state
      :predicate
      (fn [db]
        (some #(when (= hakukohde-oid (:hakukohde %)) %)
              (get-in db [:editor :hakukohteet])))
      :handler
      (fn [hakukohde]
        (dispatch [:editor/select-hakukohde hakukohde])
        (dispatch [:application/fetch-applications-by-hakukohde (:hakukohde hakukohde)])))
    (dispatch [:set-active-panel :application]))

  (defroute #"^/lomake-editori/applications/(.*)" [key]
    (dispatch [:editor/refresh-forms-with-deleteds])
    (dispatch [:editor/refresh-hakukohteet-from-applications])
    (dispatch-after-state
     :predicate
     (fn [db] (not-empty (get-in db [:editor :forms key])))
     :handler
     (fn [form]
       (dispatch [:editor/select-form (:key form)])
       (dispatch [:application/fetch-applications (:key form)])))
    (dispatch [:set-active-panel :application]))

  (accountant/dispatch-current!))
