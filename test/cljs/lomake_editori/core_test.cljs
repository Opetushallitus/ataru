(ns lomake-editori.core-test
  (:require [cljs.test :refer-macros [async deftest is testing use-fixtures]]
            [goog.dom :as dom]
            [goog.dom.DomHelper :as dh]
            [jayq.core :as jq]
            [lomake-editori.core :as core]))

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
    (js/setTimeout done 3000)))

(defn editor-link
  [app-frame]
  (let [links (.xpath app-frame "//span[@class='active-section']/span[text()='Lomake-editori']")]
    (when (= 1 (count links))
      (first links))))

(use-fixtures :once {:before setup})

(deftest ui-header
  (testing "header has editor link"
    (let [header-link-set? (-> (app-frame)
                               (editor-link)
                               (nil?)
                               (not))]
      (is header-link-set?))))
