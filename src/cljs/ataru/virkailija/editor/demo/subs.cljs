(ns ataru.virkailija.editor.demo.subs
  (:require [re-frame.core :as re-frame]
            [cljs-time.core :as time]
            [cljs-time.format :as time-format]
            [clojure.string :as string]
            [ataru.application-common.demo :as demo-common]
            [ataru.demo-config :as demo-config]))

(defn- str->date
  [str]
  (if (string/blank? str)
    nil
    (time-format/parse str)))

(defn- date->str
  [date]
  (if (some? date)
    (time-format/unparse {:format-str "yyyy-MM-dd"} date)
    ""))

(re-frame/reg-sub
  :editor/demo-validity-start
  (fn [_ _]
    (re-frame/subscribe [:editor/form-properties]))
  (fn [form-properties]
    (-> (get form-properties :demo-validity-start)
      str->date)))

(re-frame/reg-sub
  :editor/demo-validity-end
  (fn [_ _]
    (re-frame/subscribe [:editor/form-properties]))
  (fn [form-properties]
    (-> (get form-properties :demo-validity-end)
      str->date)))

(re-frame/reg-sub
  :editor/demo-allowed
  (fn [_ _]
    [(re-frame/subscribe [:editor/demo-validity-start])
     (re-frame/subscribe [:editor/demo-validity-end])
     (re-frame/subscribe [:editor/today])])
  (fn [[demo-validity-start demo-validity-end today]]
    (demo-common/demo-allowed? demo-validity-start demo-validity-end today)))

(defn- first-time
  [times]
  (->> times
    (remove nil?)
    (sort time/before?)
    first))

(re-frame/reg-sub
  :editor/first-hakuaika-start
  (fn [_ _]
    (re-frame/subscribe [:editor/used-by-haut-haut]))
  (fn [haut]
    (->> haut
      vals
      (mapcat :hakuajat)
      (map :start)
      (map time-format/parse)
      first-time)))

(re-frame/reg-sub
  :editor/last-possible-demo-date
  (fn [_ _]
    (re-frame/subscribe [:editor/first-hakuaika-start]))
  (fn [first-hakuaika-start]
    (when first-hakuaika-start
      (time/minus first-hakuaika-start (time/days demo-config/demo-validity-grace-period-days)))))

(re-frame/reg-sub
  :editor/demo-validity-start-max
  (fn [_ _]
    [(re-frame/subscribe [:editor/demo-validity-end])
     (re-frame/subscribe [:editor/last-possible-demo-date])])
  (fn [[demo-validity-end last-possible-demo-date]]
    (let [times [demo-validity-end
                 last-possible-demo-date]]
      (first-time times))))

(re-frame/reg-sub
  :editor/demo-validity-end-min
  (fn [_ _]
    (re-frame/subscribe [:editor/demo-validity-start]))
  identity)

(re-frame/reg-sub
  :editor/demo-validity-end-max
  (fn [_ _]
    (re-frame/subscribe [:editor/last-possible-demo-date]))
  identity)

(re-frame/reg-sub
  :editor/demo-validity-start-str
  (fn [_ _]
    (re-frame/subscribe [:editor/demo-validity-start]))
  date->str)

(re-frame/reg-sub
  :editor/demo-validity-start-max-str
  (fn [_ _]
    (re-frame/subscribe [:editor/demo-validity-start-max]))
  date->str)

(re-frame/reg-sub
  :editor/demo-validity-end-str
  (fn [_ _]
    (re-frame/subscribe [:editor/demo-validity-end]))
  date->str)

(re-frame/reg-sub
  :editor/demo-validity-end-min-str
  (fn [_ _]
    (re-frame/subscribe [:editor/demo-validity-end-min]))
  date->str)

(re-frame/reg-sub
  :editor/demo-validity-end-max-str
  (fn [_ _]
    (re-frame/subscribe [:editor/demo-validity-end-max]))
  date->str)
