(ns ataru.virkailija.core-test
  (:require [cljs.test :refer-macros [async deftest is use-fixtures]]
            [cljs.core.async :refer [<!]]
            [ataru.common-test-util :as common-util]
            [ataru.virkailija.test-util :as util])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn setup
  []
  (util/setup))

(defn editor-link
  []
  (util/get-element "//span[@class='active-section']/span[text()='Lomake-editori']"))

(use-fixtures :once {:before setup})

(defn not-nil?
  [x]
  (not (nil? x)))

(deftest ui-header
  (let [header-link-set? #(-> (editor-link) (not-nil?))
        test-result-ch (common-util/await-timeout 5000 header-link-set?)]
    (async done
      (go
        (is (boolean (<! test-result-ch)))
        (done)))))
