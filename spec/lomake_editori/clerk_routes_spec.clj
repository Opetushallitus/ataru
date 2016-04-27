(ns lomake-editori.clerk-routes-spec
  (:require [lomake-editori.clerk-routes :as clerk]
            [lomake-editori.test-utils :refer [should-have-header]]
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
    (should-have-header "Location" "http://localhost/lomake-editori/" @resp)))

(describe "GET /lomake-editori/"
  (with-static-resource resp "/lomake-editori/")

  (it "should not return nil"
    (should-not-be-nil @resp))

  (it "should return HTTP 200"
    (should= 200 (:status @resp)))

  (it "should refer to the compiled app.js in response body"
    (let [body (:body @resp)]
      (should-not-be-nil (re-matches #"(?s).*<script src=\"js/compiled/app.js\?fingerprint=\d{13}\"></script>.*" body))))

  (it "should have text/html as content type"
    (should-have-header "Content-Type" "text/html; charset=utf-8" @resp)))

(describe "Getting a static resource"
  (with-static-resource resp "/lomake-editori/js/compiled/app.js")

  (it "should provide the resource found from the resources/ directory"
    (should-not-be-nil @resp))

  (it "should return HTTP 200"
    (should= 200 (:status @resp))))

(run-specs)
