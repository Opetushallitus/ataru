(ns ataru.hakija.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-frisk.core :as re-frisk]
            [taoensso.timbre :refer-macros [spy info]]
            [ataru.cljs-util :as cljs-util]
            [ataru.hakija.hakija-ajax :refer [post]]
            [ataru.hakija.application-view :refer [form-view]]
            [ataru.hakija.application-handlers] ;; required although no explicit dependency
            [ataru.hakija.subs] ;; required although no explicit dependency
            [clojure.string :as str]))

(enable-console-print!)

(defn- form-key-from-url []
  (let [path (cljs-util/get-path)]
    (nth (clojure.string/split path #"/") 2)))

(defn- dispatch-form-load
  []
  (let [path (cljs-util/get-path)
        hakukohde-match (re-matches #"/hakemus/hakukohde/(.+)/?" path)]
    (if-let [hakukohde-oid (when hakukohde-match (nth hakukohde-match 1))]
      (re-frame/dispatch [:application/get-latest-form-by-hakukohde hakukohde-oid])
      (re-frame/dispatch [:application/get-latest-form-by-key (form-key-from-url)]))))

(defn mount-root []
  (reagent/render [form-view]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (cljs-util/set-global-error-handler! #(post "/hakemus/api/client-error" %))
  (mount-root)
  (re-frame/dispatch-sync [:application/initialize-db])
  (when (-> js/config
            js->clj
            (get "enable-re-frisk"))
    (re-frisk/enable-re-frisk!))
  (dispatch-form-load))
