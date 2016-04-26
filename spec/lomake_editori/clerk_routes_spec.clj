(ns lomake-editori.clerk-routes-spec
  (:require [lomake-editori.clerk-routes :as clerk]
            [ring.mock.request :as mock]
            [speclj.core :refer :all]))

(defmacro with-static-resource
  [name path]
  `(with ~name (-> (mock/request :get ~path)
                   (clerk/clerk-routes))))

(defn ^:private should-have-header
  [header expected-val resp]
  (let [headers (:headers @resp)]
    (should-not-be-nil headers)
    (should-contain header headers)
    (should= expected-val (get headers header))))

(describe "GET /lomake-editori"
  (with-static-resource resp "/lomake-editori")

  (it "should not return nil"
    (should-not-be-nil @resp))

  (it "should return HTTP 302"
    (should= 302 (:status @resp)))

  (it "should redirect to /lomake-editori/"
    (should-have-header "Location" "http://localhost/lomake-editori/" resp)))

(describe "GET /lomake-editori/"
  (with-static-resource resp "/lomake-editori/")

  (it "should not return nil"
    (should-not-be-nil @resp))

  (it "should return HTTP 200"
    (should= 200 (:status @resp)))

  (it "should refer to the compiled app.js in response body"
    (let [body (slurp (:body @resp))]
      (should-not-be-nil (re-matches #"(?s).*<script src=\"js/compiled/app.js\"></script>.*" body)))))

(run-specs)
