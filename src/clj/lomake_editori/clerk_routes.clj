(ns lomake-editori.clerk-routes
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
            [ring.util.http-response :refer [ok not-found]]
            [lomake-editori.db.form-store :as form-store]
            [ring.util.response :refer [response]]
            [taoensso.timbre :refer [spy]])
  (:import  [manifold.deferred.Deferred]))

;; Compojure will normally dereference deferreds and return the realized value.
;; This unfortunately blocks the thread. Since aleph can accept the un-realized
;; deferred, we extend compojure's Renderable protocol to pass the deferred
;; through unchanged so that the thread won't be blocked.
(extend-protocol Renderable
                 manifold.deferred.Deferred
                 (render [d _] d))

;https://github.com/ztellman/aleph/blob/master/examples%2Fsrc%2Faleph%2Fexamples%2Fhttp.clj

(defn wrap-dev-only [handler]
  (fn [req]
    (if (:dev? env)
      (handler req)
      (not-found "Not found"))))

(defroutes dev-routes
  (context "/dev-resources" []
    (GET "/:file" [file]
      (file-response file {:root "dev-resources"}))))

(defroutes app-routes
  (GET "/" [] (redirect "/lomake-editori/"))
  (GET "/lomake-editori" [] (redirect "/lomake-editori/")) ;; Without slash -> 404 unless we do this redirect
  (GET "/lomake-editori/" [] (file-response "index.html" {:root "resources/templates"})))

(s/defschema Form
  {:id s/Str :name s/Str})

(defn api-routes []
  (api/api
    {:swagger {:spec "/lomake-editori/swagger.json"
               :ui "/lomake-editori/api-docs"
               :data {:info {:version "1.0.0"
                             :title "Ataru Clerk API"
                             :description "Specifiecs the clerk API for Ataru"}}
               :tags [{:name "form-api" :description "Form handling"}]}}
    (api/context "/lomake-editori/api" []
                 :tags ["form-api"]
      (api/GET "/forms" []
               (ok
                 {:forms (form-store/get-forms)}))
      (api/POST "/form" []
                :body [form Form]
                (form-store/upsert-form form)
                (ok {})))))

(defroutes resource-routes
  (route/files "/lomake-editori" {:root "resources/public"}))

(def clerk-routes
  (-> (routes (wrap-routes dev-routes wrap-dev-only)
                    resource-routes
                    app-routes
                    (api-routes)
                    (route/not-found "Not found"))
      (wrap-defaults (-> site-defaults
                         (update-in [:security] dissoc :content-type-options)
                         (update-in [:security] dissoc :anti-forgery)
                         (update-in [:responses] dissoc :content-types)))
      (wrap-gzip)))
