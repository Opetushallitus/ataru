(ns ataru.application-common.cofx
  (:require [re-frame.core :as re-frame]
            [cljs-time.coerce :as time-coerce]))

(re-frame/reg-cofx
  :now
  (fn [coeffects _]
    (let [now (time-coerce/from-date (js/Date.))]
      (assoc coeffects :now now))))
