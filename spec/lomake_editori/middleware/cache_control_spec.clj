(ns lomake-editori.middleware.cache-control-spec
  (:require [lomake-editori.middleware.cache-control :as cache]
            [ring.mock.request :as mock]
            [ring.util.http-response :as response]
            [speclj.core :refer :all]))

(defn ^:private stub-handler
  [_]
  (response/ok {}))

(describe "cache-control middleware"
  (it "should add Cache-Control: max-age=86400 header"
    (let [wrap-fn (cache/wrap-cache-control stub-handler)
          req     (mock/request :get "/lomake-editori/static-resource.js")
          resp    (wrap-fn req)]
      (should-not-be-nil resp)
      (let [headers (:headers resp)]
        (should-not-be-nil headers)
        (should-contain "Cache-Control" headers)
        (should= "max-age=86400" (get headers "Cache-Control"))))))
