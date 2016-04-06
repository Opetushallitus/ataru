(ns lomake-editori.soresu.components
  (:require [reagent.core :as r :refer [adapt-react-class]]
            [cljs.core.match :refer-macros [match]]
            [camel-snake-kebab.core :as csk]
            [taoensso.timbre :refer-macros [spy info]]))

(def soresu js/soresu)

(defn- function? [js-f] (= "function"
                           (js* "typeof " js-f)))

(defn- adapt-components [js-obj]
  (reduce-kv
    (fn [m key react-class]
      (info key (map? react-class))
      (.dir js/console react-class)
      (match [react-class (-> key csk/->kebab-case keyword)]
             [(module :guard map?) k] (assoc m k (adapt-components module))
             [(c :guard function?) k] (assoc m k (r/adapt-react-class c))
             :else m))
    {}
    (js->clj js-obj)))

(def form (-> soresu .-form adapt-components))
(def component (-> soresu .-component adapt-components))
(def edit (-> soresu .-edit adapt-components))
(def img (into {}
           (for [[k base64image] (-> soresu .-img js->clj)
                 :let [img (apply str (drop 2 k))]]
             {img base64image})))
(def preview (-> soresu .-preview adapt-components))
