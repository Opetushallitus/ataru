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

(defn- update-form [id fragments]
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

(defn- get-names [content]
  (map #(get-in % [:label :fi]) content))

(defn- get-structure-as-names [content]
  (map (fn [element] (if (= "questionGroup" (:fieldClass element ))
                       (get-names (:children element))
                       (get-in element [:label :fi]))) content))

(defn- create-element [name]
  {:id name,
   :label {:fi name},
   :fieldType "textField",
   :fieldClass "formField"})

(defn- create-form [& elements]
  {:name        {:fi (clojure.string/join "" (get-names elements))}
   :created-by "DEVELOPER"
   :content elements})

(defn- create-wrapper [& elements]
  (let [name (str "W" (clojure.string/join "" (get-names elements)))]
    {:id         name,
     :label      {:fi name},
     :children elements
     :fieldType  "fieldset",
     :fieldClass "questionGroup"}))

(defn- get-content-from-response [response]
  (get-in response [:body :content]))

(defn- update-and-get-form [id operations]
  (update-form id operations)
  (get-form id))

(describe "Storing a fragment"
          (tags :unit :route-store-fragment)

  (it "Should handle delete"
      (let [resp (post-form (create-form (create-element "A")
                                         (create-element "B")
                                         (create-element "C")))
            form (:body resp)
            with-update (-> form
                            (update :content (fn [v] [(first v) (last v)])))
            operations (form-diff/as-operations form with-update)
            new-content (get-content-from-response (update-and-get-form (:id form) operations))]
        (should= ["A" "C"] (get-names new-content))))

  (it "Should handle updates"
      (let [resp (post-form (create-form (create-element "A")
                                         (create-element "B")
                                         (create-element "C")))
            form (:body resp)
            with-updates (-> form
                             (update-in [:content 0 :label :fi] (fn [e] "AA"))
                             (update-in [:content 1 :label :fi] (fn [e] "BB")))
            operations (form-diff/as-operations form with-updates)
            new-content (get-content-from-response (update-and-get-form (:id form) operations))]
        (should= ["AA" "BB" "C"] (get-names new-content))))

  (it "Should handle (different users) update and relocate"
      (let [resp (post-form (create-form (create-element "A")
                                         (create-element "B")
                                         (create-element "C")))
            form (:body resp)
            with-updates (-> form
                             (update-in [:content 0 :label :fi] (fn [e] "AA")))
            with-relocate (-> form
                              (update :content (fn [v] (swap v 0 1))))
            after-update (update-form (:id form) (form-diff/as-operations form with-updates))
            after-relocate (update-form (:id form) (form-diff/as-operations form with-relocate))
            new-content (get-content-from-response (get-form (:id form)))]
        (should= ["B" "AA" "C"] (get-names new-content))))

  (it "Should handle relocation"
      (let [resp (post-form (create-form (create-element "A")
                                         (create-element "B")
                                         (create-element "C")))
            form (:body resp)
            with-update (-> form
                            (update :content (fn [v] (swap v 0 1))))
            operations (form-diff/as-operations form with-update)
            new-content (get-content-from-response (update-and-get-form (:id form) operations))]
        (should= ["B" "A" "C"] (get-names new-content))))

  (it "Should handle move out of wrapper element"
      (let [resp        (post-form (create-form (create-wrapper (create-element "A1") (create-element "A2"))
                                                (create-element "B")
                                                (create-element "C")))
            form        (:body resp)
            with-update (-> form
                            (update :content (fn [content]
                                                 (let [[wrapper & rest] content
                                                       a1    (get-in content [0 :children 0])
                                                       a2    (get-in content [0 :children 1])
                                                       wa2   (assoc wrapper :children [a2])
                                                       new-c (concat [a1 wa2] rest)]
                                                   new-c))))
            operations (form-diff/as-operations form with-update)
            new-content (get-content-from-response (update-and-get-form (:id form) (form-diff/as-operations form with-update)))]
        (should= ["A1" ["A2"] "B" "C"] (get-structure-as-names new-content))))

  (it "Shouldn't allow conflicting updates"
      (let [resp (post-form (create-form (create-element "A")
                                         (create-element "B")
                                         (create-element "C")))
            form (:body resp)
            with-updates (-> form
                             (update-in [:content 0 :label :fi] (fn [e] "AA")))
            with-conflicting-updates (-> form
                             (update-in [:content 0 :label :fi] (fn [e] "ABC")))
            success-response (update-form (:id form) (form-diff/as-operations form with-updates))
            failure-response (update-form (:id form) (form-diff/as-operations form with-conflicting-updates))]
        (should= 200 (:status success-response))
        (should= 400 (:status failure-response))))

  (it "Should allow updating form details"
      (let [resp (post-form (create-form (create-element "A")
                                         (create-element "B")
                                         (create-element "C")))
            form (:body resp)
            with-updates (-> form (assoc :name {:fi "A" :en "B"}))
            operations (form-diff/as-operations form with-updates)
            success-response (update-and-get-form (:id form) operations)
            new-content (get-content-from-response success-response)]
        (should= ["A" "B" "C"] (get-names new-content))
        (should= {:fi "A" :en "B"} (get-in success-response [:body :name]))))
  
  )

(run-specs)
