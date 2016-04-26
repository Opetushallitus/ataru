(ns lomake-editori.clerk-routes-spec
  (:require [lomake-editori.clerk-routes :as clerk]
            [ring.mock.request :as mock]
            [speclj.core :refer :all]))

(defmacro with-static-resource
  [name path]
  `(with ~name (-> (mock/request :get ~path)
                   (clerk/clerk-routes))))

(describe "GET /lomake-editori"
  (with-static-resource resp "/lomake-editori")

  (it "should not return nil"
    (should-not-be-nil @resp))

  (it "should return HTTP 302"
    (should= 302 (:status @resp)))

  (it "should redirect to /lomake-editori/"
    (let [headers (:headers @resp)]
      (should-not-be-nil headers)
      (should-contain "Location" headers)
      (should= "http://localhost/lomake-editori/" (get headers "Location")))))

(run-specs)
