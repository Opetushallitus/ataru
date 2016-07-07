(ns ataru.applications.application-store-spec
  (:require [ataru.applications.application-store :as store]
            [ataru.fixtures.application :as fixtures]
            [speclj.core :refer :all]))

(def form-id (:id fixtures/form))

(describe "fetch-applications"
  (tags :unit)

  (around [spec]
    (with-redefs [store/exec-db (fn [ds-key query-fn params]
                                  (should= :db ds-key)
                                  (should= "yesql-application-query-by-modified" (-> query-fn .meta :name))
                                  (should= {:form_id 703, :limit 100, :lang "fi"} params)
                                  fixtures/applications)]
      (spec)))

  (it "should return all applications belonging to a form"
    (store/fetch-applications form-id {})))