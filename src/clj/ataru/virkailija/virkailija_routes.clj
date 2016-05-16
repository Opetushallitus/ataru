(ns ataru.virkailija.virkailija-routes
  (:require [compojure.core :refer [GET POST PUT defroutes context routes wrap-routes]]
            [compojure.response :refer [Renderable]]
            [compojure.route :as route]
            [compojure.api.sweet :as api]
            [schema.core :as s]
            [environ.core :refer [env]]
            [manifold.deferred :as d]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.util.response :refer [file-response resource-response redirect]]
            [ring.util.http-response :refer [ok internal-server-error not-found content-type]]
            [ataru.virkailija.form-store :as form-store]
            [ataru.middleware.cache-control :as cache-control]
            [ataru.middleware.session-store :refer [create-store]]
            [ataru.virkailija.authentication.auth-middleware :as auth-middleware]
            [ataru.virkailija.authentication.auth-routes :refer [auth-routes]]
            [ring.util.response :refer [response]]
            [taoensso.timbre :refer [spy error]]
            [selmer.parser :as selmer])
  (:import  [manifold.deferred.Deferred]))

;; Compojure will normally dereference deferreds and return the realized value.
;; This unfortunately blocks the thread. Since aleph can accept the un-realized
;; deferred, we extend compojure's Renderable protocol to pass the deferred
;; through unchanged so that the thread won't be blocked.
(extend-protocol Renderable
                 manifold.deferred.Deferred
                 (render [d _] d))

;https://github.com/ztellman/aleph/blob/master/examples%2Fsrc%2Faleph%2Fexamples%2Fhttp.clj

(defn trying [f]
  (try (ok (f))
       (catch Exception e
         (error e)
         (internal-server-error))))

(defn wrap-dev-only [handler]
  (fn [req]
    (if (:dev? env)
      (handler req)
      (not-found "Not found"))))

(defroutes dev-routes
  (context "/dev-resources" []
    (GET "/:file" [file]
      (file-response file {:root "dev-resources"}))))

(def ^:private cache-fingerprint (System/currentTimeMillis))

(defroutes app-routes
  (GET "/" [] (selmer/render-file "templates/index.html" {:cache-fingerprint cache-fingerprint})))

(defroutes test-routes
  (GET "/test.html" []
    (if (:dev? env)
        (selmer/render-file "templates/test.html" {})
        (not-found "Not found"))))

(s/defschema Form
  {(s/optional-key :id) (s/maybe s/Int)
   :name s/Str
   s/Any s/Any})

(def api-routes
  (api/api
    {:swagger {:spec "/lomake-editori/swagger.json"
               :ui "/lomake-editori/api-docs"
               :data {:info {:version "1.0.0"
                             :title "Ataru Clerk API"
                             :description "Specifies the clerk API for Ataru"}}
               :tags [{:name "form-api" :description "Form handling"}]}}
    (api/context "/api" []
                 :tags ["form-api"]
                 (api/GET "/user-info" {session :session}
                          (ok {:username (-> session :identity :username)}))
                 (api/GET "/forms" []
                          (ok
                           {:forms (form-store/get-forms)}))
                 (api/POST "/form" []
                           :body [form Form]
                           (trying #(form-store/upsert-form form))))))

(defroutes resource-routes
  (route/resources "/"))

(def clerk-routes
  (-> (routes (wrap-routes dev-routes wrap-dev-only)
              (GET "/" [] (redirect "/lomake-editori/"))
              (context "/lomake-editori" []
                resource-routes
                app-routes
                test-routes
                api-routes
                auth-routes)
              (route/not-found "Not found"))
      (auth-middleware/with-authentication)
      (wrap-defaults (-> site-defaults
                         (update-in [:session] assoc :store (create-store))
                         (update-in [:security] dissoc :content-type-options)
                         (update-in [:security] dissoc :anti-forgery)
                         (update-in [:responses] dissoc :content-types)))
      (wrap-gzip)
      (cache-control/wrap-cache-control)))
