(ns ataru.hakija.hakija-fx
  (:require [cemerick.url :as url]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [re-frame.core :as re-frame]))

(def ^:private ->kebab-case-kw (partial transform-keys ->kebab-case-keyword))

(re-frame/reg-cofx
  :query-params
  (fn [cofx _]
    (let [query-params (-> (.. js/window -location -href)
                           (url/url)
                           (:query)
                           (->kebab-case-kw))]
      (assoc cofx :query-params query-params))))
