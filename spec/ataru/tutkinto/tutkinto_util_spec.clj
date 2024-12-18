(ns ataru.tutkinto.tutkinto-util-spec
  (:require [speclj.core :refer :all]
            [cheshire.core :as json]
            [ataru.tutkinto.tutkinto-util :as tutkinto-util]))

(defn- read-application-json [file-name]
  (:application (json/parse-string (slurp (str "dev-resources/koski/" file-name)) true)))

(describe "Finding tutkinto-data from application"
          (tags :unit)
          (it "should detect tutkinto id fields in application"
              (should= true (tutkinto-util/koski-tutkinnot-in-application? (read-application-json "application-with-koski-tutkinnot.json")))
              ))