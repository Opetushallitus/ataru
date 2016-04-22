(ns lomake-editori.handler
  (:require [compojure.core :refer [ANY GET POST PUT defroutes context routes wrap-routes]]
            [compojure.response :refer [Renderable]]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [manifold.deferred :as d]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.util.response :refer [file-response resource-response]]
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
  (GET "/" [] (file-response "index.html" {:root "resources/templates"})))

(defroutes api-routes
  (context "/api" []
    (GET "/forms" []
      (ok
        {:forms (form-store/get-forms)}))
    (POST "/form" []
      (ok {}))))

(def handler
  (-> (routes (wrap-routes dev-routes wrap-dev-only)
              app-routes
              api-routes
              (route/not-found "Not found"))
      (wrap-defaults (-> site-defaults
                         (update-in [:security] dissoc :content-type-options)
                         (update-in [:security] dissoc :anti-forgery)
                         (update-in [:responses] dissoc :content-types)))
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (wrap-gzip)))
