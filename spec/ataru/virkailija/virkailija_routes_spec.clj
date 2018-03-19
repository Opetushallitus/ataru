(ns ataru.virkailija.virkailija-routes-spec
  (:require [ataru.virkailija.virkailija-routes :as v]
            [ataru.test-utils :refer [login should-have-header virkailija-routes]]
            [ring.mock.request :as mock]
            [ataru.fixtures.form :as fixtures]
            [speclj.core :refer :all]
            [cheshire.core :as json]
            [ataru.virkailija.editor.form-diff :as form-diff]
            [taoensso.timbre :refer [spy debug]]))

(defmacro with-static-resource
  [name path]
  `(with ~name (-> (mock/request :get ~path)
                   (update-in [:headers] assoc "cookie" (login))
                   virkailija-routes)))

(defn- get-form [id]
  (-> (mock/request :get (str "/lomake-editori/api/forms/" id))
      (update-in [:headers] assoc "cookie" (login))
      (mock/content-type "application/json")
      virkailija-routes
      (update :body (comp (fn [content] (json/parse-string content true)) slurp))))

(defn- post-form [form]
  (-> (mock/request :post "/lomake-editori/api/forms"
        (json/generate-string form))
      (update-in [:headers] assoc "cookie" (login))
      (mock/content-type "application/json")
      virkailija-routes
      (update :body (comp (fn [content] (json/parse-string content true)) slurp))))

(defn- batch-form [id fragments]
  (-> (mock/request :put (str "/lomake-editori/api/forms/" id)
        (json/generate-string fragments))
      (update-in [:headers] assoc "cookie" (login))
      (mock/content-type "application/json")
      virkailija-routes
      (update :body (comp (fn [content] (json/parse-string content true)) slurp))))

(describe "GET /lomake-editori"
  (tags :unit)

  (with-static-resource resp "/lomake-editori")

  (it "should not return nil"
    (should-not-be-nil @resp))

  (it "should return HTTP 302"
    (should= 302 (:status @resp)))

  (it "should redirect to /lomake-editori/"
    (should-have-header "Location" "http://localhost:8350/lomake-editori/" @resp)))

(describe "GET /lomake-editori/"
  (tags :unit)

  (with-static-resource resp "/lomake-editori/")

  (it "should not return nil"
    (should-not-be-nil @resp))

  (it "should return HTTP 200"
    (should= 200 (:status @resp)))

  (it "should refer to the compiled app.js in response body"
    (let [body (:body @resp)]
      (should-not-be-nil (re-matches #"(?s).*<script src=\".*virkailija-app.js\?fingerprint=\d{13}\"></script>.*" body))))

  (it "should have text/html as content type"
    (should-have-header "Content-Type" "text/html; charset=utf-8" @resp))

  (it "should have Cache-Control: no-store header"
    (should-have-header "Cache-Control" "no-store" @resp)))

(describe "Getting a static resource"
  (tags :unit)

  (with-static-resource resp "/lomake-editori/js/compiled/virkailija-app.js")

  (it "should provide the resource found from the resources/ directory"
    (should-not-be-nil @resp))

  (it "should have Cache-Control: max-age  header"
    (should-have-header "Cache-Control" "public, max-age=2592000" @resp)))

(describe "Storing a form"
  (tags :unit :route-store-form)

  (with resp
    (post-form fixtures/form-with-content))

  (before
    (println (:body @resp)))

  (it "Should respond ok"
    (should= 200 (:status @resp)))

  (it "Should have an id"
    (should (some? (-> @resp :body :id))))

  (it "Should have :content with it"
    (should= (:content fixtures/form-with-content) (-> @resp :body :content))))

(defn- swap [v i1 i2]
  (assoc v i2 (v i1) i1 (v i2)))

(describe "Storing a fragment"
          (tags :unit :route-store-fragment)

  (it "Should handle delete"
      (let [resp (post-form fixtures/form-with-content)
            form (:body resp)
            with-update (-> form
                            (update :content (fn [v] [(first v)])))
            operations (form-diff/as-operations form with-update)
            batch-response (batch-form (:id form) operations)]
        (should= 2 (count operations))))

  (it "Should handle updates"
      (let [resp (post-form fixtures/form-with-content)
            form (:body resp)
            with-updates (-> form
                             (update-in [:content 0 :label :sv] (fn [e] "Some-sv"))
                             (update-in [:content 1 :label :sv] (fn [e] "Other-sv")))
            operations (form-diff/as-operations form with-updates)
            batch-response (batch-form (:id form) operations)
            latest-form-response (get-form (:id form))]
        (should= 2 (count operations))
        (should= 200 (:status batch-response))
        (should= 200 (:status latest-form-response))
        (should= (:content (:body latest-form-response)) (:content with-updates))))

  (it "Should handle relocation"
      (let [resp (post-form fixtures/form-with-content)
            form (:body resp)
            with-update (-> form
                            (update :content (fn [v] (swap v 0 1))))
            operations (form-diff/as-operations form with-update)
            batch-response (batch-form (:id form) operations)
            latest-form-response (get-form (:id form))]
        (should= (:content (:body latest-form-response)) (:content with-update))))


  )

(run-specs)
