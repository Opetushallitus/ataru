(ns ataru.middleware.filename-normalizer-middleware
  (:require [ataru.filename-normalizer :as normalizer]))

(defn wrap-query-params-filename-normalizer [handler]
  (fn normalize-query-params-filename [request]
    (let [filename (-> request :query-params (get "file-name"))
          request  (cond-> request
                           (some? filename)
                           (update-in [:query-params "file-name"] normalizer/normalize-filename))]
      (handler request))))
