(ns ataru.middleware.cache-control-spec
  (:require [ataru.middleware.cache-control :as cache]
            [ataru.test-utils :refer [should-have-header should-not-have-header]]
            [ring.mock.request :as mock]
            [ring.util.http-response :as response]
            [speclj.core :refer :all]))

(defn ^:private stub-handler
  [_]
  (response/ok {}))

(defmacro with-resp
  [binding & body]
  `(let [wrap-fn# (cache/wrap-cache-control stub-handler)
         req# (mock/request :get ~(second binding))
         ~(first binding) (wrap-fn# req#)]
     ~@body))

(describe "cache-control middleware"
  (it "should add Cache-Control: max-age=86400 header for static resources"
    (with-resp [resp "/lomake-editori/static-resource.js"]
      (should-not-be-nil resp)
      (should-have-header "Cache-Control" "max-age=86400" resp)))

  (it "should add Cache-Control: no-cache header for app root"
    (with-resp [resp "/lomake-editori/"]
      (should-not-be-nil resp)
      (should-have-header "Cache-Control" "no-cache" resp)))

  (it "should add Cache-Control: no-store for requests to /lomake-editori/api"
    (with-resp [resp "/lomake-editori/api/forms"]
      (should-not-be-nil resp)
      (should-have-header "Cache-Control" "no-store" resp))))
