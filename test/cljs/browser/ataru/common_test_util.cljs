(ns ataru.common-test-util
  (:require [cljs.test :refer-macros [async deftest is testing use-fixtures]]
            [cljs.core.async :refer [chan >! <! close!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn- timeout
  [ms]
  (let [c (chan)]
    (js/setTimeout (fn [] (close! c)) ms)
    c))

(defn- wait-for
  [fn]
  (let [c (chan)]
    (go
      (loop [fn-result (fn)]
        (if fn-result
          (>! c fn-result)
          (<! (timeout 200)))
        (recur (fn))))
    c))

(defn await-timeout
  [ms fn]
  (let [out-c (chan)
        wait-c (wait-for fn)
        timeout (timeout ms)]
    (go
      (let [[v _] (alts! [wait-c timeout])]
        (if (not (boolean v))
          (close! out-c)
          (>! out-c v))))
    out-c))

(defn sleep
  [ms]
  (async done
    (go
      (<! (timeout ms))
      (done))))

(def await (partial await-timeout 5000))