(ns ataru.virkailija.core-test
  (:require [cljs.test :refer-macros [async deftest is testing use-fixtures]]
            [cljs.core.async :refer [chan >! <! close!]]
            [goog.dom :as dom]
            [goog.dom.DomHelper :as dh]
            [jayq.core :as jq]
            [ataru.virkailija.core :as core])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn app-frame
  []
  (-> (jq/$ :#test)
      (first)
      (.-contentWindow)
      (.-document)
      (jq/$)))

(defn setup
  []
  (async done
    (doto (jq/$ :#test)
      (jq/attr :src "/lomake-editori/")
      (jq/attr :width "1024")
      (jq/attr :height "768"))
    (done)))

(defn timeout
  [ms]
  (let [c (chan)]
    (js/setTimeout (fn [] (close! c)) ms)
    c))

(defn await
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
        wait-c (await fn)
        timeout (timeout ms)]
    (go
      (let [[v _] (alts! [wait-c timeout])]
        (if (nil? v)
          (close! out-c)
          (>! out-c v))))
    out-c))

(defn not-nil?
  [x]
  (not (nil? x)))

(defn get-element
  [xpath]
  (let [elements (-> (app-frame)
                     (.xpath xpath))]
    (when (= 1 (count elements))
      (first elements))))

(defn editor-link
  []
  (get-element "//span[@class='active-section']/span[text()='Lomake-editori']"))

(use-fixtures :once {:before setup})

(deftest ui-header
  (let [header-link-set? #(-> (editor-link) (not-nil?))
        test-result-ch (await-timeout 5000 header-link-set?)]
    (async done
      (go
        (is (boolean (<! test-result-ch)))
        (done)))))
