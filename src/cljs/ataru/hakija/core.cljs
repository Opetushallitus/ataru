(ns ataru.hakija.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [taoensso.timbre :refer-macros [spy info]]
            [ataru.cljs-util :refer [set-global-error-handler!]]
            [ataru.hakija.hakija-ajax :refer [post]]
            [ataru.hakija.application-view :refer [form-view]]
            [ataru.hakija.application-handlers] ;; required although no explicit dependency
            [ataru.hakija.subs] ;; required although no explicit dependency
            [clojure.string :as str]))

(enable-console-print!)

(def ^:private key-pred
  (partial re-matches #"(?i)^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"))

(defn- form-key-from-url []
  (let [path (.. js/window -location -pathname)]
    (some key-pred (clojure.string/split path #"/"))))

(defn mount-root []
  (reagent/render [form-view]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (set-global-error-handler! #(post "/hakemus/api/client-error" %))
  (mount-root)
  (re-frame/dispatch-sync [:application/initialize-db])
  (re-frame/dispatch [:application/get-latest-form-by-key (form-key-from-url)]))
