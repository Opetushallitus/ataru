(ns lomake-editori.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.response :refer [Renderable]]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [ring.middleware.params :as params]
            [byte-streams :as bs]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [clojure.core.async :as a]
            [ring.util.response :refer [file-response resource-response]]
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
  (route/files "/" {:root "resources/public"})
  (GET "/" [] (file-response "index.html" {:root "resources/templates"}))

  (GET "/vendor/precompiled/js/soresu.js" []
    ; provided by oph/soresu 0.1.0-SNAPSHOT
    (resource-response "/public/js/soresu.js"))
  (GET "/vendor/precompiled/js/soresu.js.map" []
    ; provided by oph/soresu 0.1.0-SNAPSHOT
    (resource-response "/public/js/soresu.js.map"))

  (route/not-found "Not found"))

(def handler
  (-> routes
      (params/wrap-params)))
