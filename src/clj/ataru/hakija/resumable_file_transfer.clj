(ns ataru.hakija.resumable-file-transfer
  (:require [cheshire.core :as json]
            [ataru.config.core :refer [config]]
            [ataru.cas.client :as cas]
            [ataru.config.url-helper :refer [resolve-url]]
            [taoensso.timbre :as log]
            [string-normalizer.filename-normalizer :as normalizer]))

(def max-part-size (get-in config [:public-config :attachment-file-part-max-size-bytes] (* 1024 1024)))
(def origin-system "ataru")

(defn mark-upload-delivered-to-liiteri
  [cas-client file-id file-name]
  (log/info "Mark upload delivered to liiteri:" file-name "with file id" file-id)
  (let [url                         (resolve-url :liiteri.delivered file-id)
        start-time                  (System/currentTimeMillis)
        {:keys [status body error]} (cas/cas-authenticated-multipart-post
                                      cas-client
                                      url
                                      (fn []
                                        {:socket-timeout (* 1000 60 10)
                                         :query-params {:filename (normalizer/normalize-filename file-name)
                                                        :origin-system origin-system}}))]
    (cond (= status 200)
          (do
            (log/info "Uploaded file" file-name "to liiteri in" (- (System/currentTimeMillis) start-time) "ms:" body)
            [:complete (dissoc (json/parse-string body true) :version :deleted)])
          (= status 400)
          (do
            (log/error "Error uploading file to liiteri at " url " :" file-name status error body)
            [:bad-request nil])
          :else
          (do
            (log/error "Error uploading file to liiteri at " url " :" file-name status error body)
            [:liiteri-error nil]))))
