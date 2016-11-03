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
  (tags :unit)

  (it "should add Cache-Control: max-age header for static resources"
    (with-resp [resp "/lomake-editori/static-resource.js"]
      (should-not-be-nil resp)
      (should-have-header "Cache-Control" "public, max-age=2592000" resp)))

  (it "should add Cache-Control: no-store header for app root"
    (with-resp [resp "/lomake-editori/"]
      (should-not-be-nil resp)
      (should-have-header "Cache-Control" "no-store" resp)))

  (it "should add Cache-Control: no-store for requests to /lomake-editori/api"
    (with-resp [resp "/lomake-editori/api/forms"]
      (should-not-be-nil resp)
      (should-have-header "Cache-Control" "no-store" resp))))
