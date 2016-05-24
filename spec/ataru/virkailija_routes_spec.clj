(ns ataru.virkailija-routes-spec
  (:require [ataru.virkailija.virkailija-routes :as v]
            [ataru.test-utils :refer [login should-have-header]]
            [ring.mock.request :as mock]
            [ataru.fixtures.form :as fixtures]
            [speclj.core :refer :all]
            [cheshire.core :as json]
            [taoensso.timbre :refer [spy debug]]))

(defmacro with-static-resource
  [name path]
  `(with ~name (-> (mock/request :get ~path)
                   (update-in [:headers] assoc "cookie" (login))
                   v/clerk-routes)))

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
    (should-have-header "Content-Type" "text/html; charset=utf-8" @resp))

  (it "should have Cache-Control: no-cache header"
    (should-have-header "Cache-Control" "no-cache" @resp)))

(describe "Getting a static resource"
  (with-static-resource resp "/lomake-editori/js/compiled/app.js")

  (it "should provide the resource found from the resources/ directory"
    (should-not-be-nil @resp))

  (it "should return HTTP 200"
    (should= 200 (:status @resp)))

  (it "should have Cache-Control: max-age=86400 header"
    (should-have-header "Cache-Control" "max-age=86400" @resp)))

(describe "Storing a form"
    (with resp
          (-> (mock/request :post "/lomake-editori/api/form"
                            (json/generate-string fixtures/form-with-content))
              (update-in [:headers] assoc "cookie" (login))
              (mock/content-type "application/json")
              v/clerk-routes
              (update :body (comp (fn [content] (json/parse-string content true)) slurp))))

  (before
    (println (:body @resp)))

  (it "Should respond ok"
    (should= 200 (:status @resp)))

  (it "Should have an id"
    (should (some? (-> @resp :body :id))))

  (it "Should have updated modified-time"
    (should (some? (-> @resp :body :modified-time))))

  (it "Should have changed :modified-by"
    (should= "DEVELOPER" (-> @resp :body :modified-by)))

  (it "Should have :content with it"
     (should= (:content fixtures/form-with-content) (-> @resp :body :content))))

(run-specs)
