(ns ataru.config
  #?(:clj
     (:require [ataru.config.core :refer [config]])))

(defn get-public-config [path]
  #?(:clj  (get-in config path)
     :cljs (some-> js/config
                   (js->clj :keywordize-keys true)
                   (get-in path))))
