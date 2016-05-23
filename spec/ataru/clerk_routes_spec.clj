(ns ataru.clerk-routes-spec
  (:require [ataru.virkailija.virkailija-routes :as v]
            [ataru.test-utils :refer [login should-have-header]]
            [ring.mock.request :as mock]
            [speclj.core :refer :all]
            [taoensso.timbre :refer [spy debug]]))

(defmacro with-static-resource
  [name path]
  `(with ~name (-> (mock/request :get ~path)
                   (update-in [:headers] assoc "cookie" (login))
                   v/virkailija-routes)))

(describe "GET /lomake-editori/"
  (with-static-resource resp "/lomake-editori")

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

(run-specs)
