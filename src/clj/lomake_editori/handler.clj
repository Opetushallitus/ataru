(ns lomake-editori.handler
  (:require [byte-streams :as bs]
            [clojure.core.async :as a]
            [compojure.response :refer [Renderable]]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [lomake-editori.db.extensions]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [ring.util.response :refer [response]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.util.response :refer [file-response resource-response]]
            [compojure.core :refer [GET POST PUT defroutes]]
            [taoensso.timbre :refer [spy]]))

;; Compojure will normally dereference deferreds and return the realized value.
;; This unfortunately blocks the thread. Since aleph can accept the un-realized
;; deferred, we extend compojure's Renderable protocol to pass the deferred
;; through unchanged so that the thread won't be blocked.
(extend-protocol Renderable
                 manifold.deferred.Deferred
                 (render [d _] d))

;https://github.com/ztellman/aleph/blob/master/examples%2Fsrc%2Faleph%2Fexamples%2Fhttp.clj

(defroutes routes
  (GET "/" [] (file-response "index.html" {:root "resources/templates"}))
  (route/not-found "Not found"))

(def handler
  (-> routes
      (wrap-defaults (-> site-defaults
                         (update-in [:security] dissoc :content-type-options)
                         (update-in [:responses] dissoc :content-types)))
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (wrap-gzip)))
