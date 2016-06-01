(ns ataru.hakija.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [taoensso.timbre :refer-macros [spy info]]
            [ataru.hakija.form-view :refer [form-view]]
            [ataru.hakija.application-handlers] ;; required although no explicit dependency
            [ataru.hakija.subs] ;; required although no explicit dependency
            [clojure.string :as str]))

(enable-console-print!)

(defn- form-id-from-url []
  (last (str/split (-> js/window .-location .-pathname) #"/")))

(defn mount-root []
  (reagent/render [form-view]
                  (.getElementById js/document "app")))


(defn ^:export init []
  (mount-root)
  (re-frame/dispatch-sync [:application/initialize-db])
  (re-frame/dispatch [:application/get-form (form-id-from-url)]))
