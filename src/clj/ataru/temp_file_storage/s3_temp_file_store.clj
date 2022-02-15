(ns ataru.temp-file-storage.s3-temp-file-store
  (:require [ataru.config.core :refer [config]]
            [ataru.temp-file-storage.temp-file-store :refer [TempFileStorage]]
            [taoensso.timbre :as log])
  (:import (com.amazonaws HttpMethod)
           (java.util Date)
           (com.amazonaws.services.s3.model GeneratePresignedUrlRequest ResponseHeaderOverrides)))

(defn- bucket-name []
  (get-in config [:aws :liiteri-files :bucket]))

(defrecord S3TempFileStore [s3-client config]
  TempFileStorage

  (signed-upload-url [_ file-name]
    (let [bucket-name (bucket-name)
          expiration  (Date. (+ (System/currentTimeMillis) (* 1000 60 60 24)))
          method      HttpMethod/PUT
          url         (.generatePresignedUrl (:s3-client s3-client) bucket-name file-name expiration method)]
      (log/info "Signed upload link: " url " expires" expiration)
      (.toString url)))

  (signed-download-url [_ file-key content-type content-disposition]
    (let [bucket-name (bucket-name)
          expiration  (Date. (+ (System/currentTimeMillis) (* 1000 60 5)))
          method      HttpMethod/GET
          request     (-> (new GeneratePresignedUrlRequest bucket-name file-key method)
                          (.withExpiration expiration)
                          (.withResponseHeaders (-> (new ResponseHeaderOverrides)
                                                    (.withContentType content-type)
                                                    (.withContentDisposition content-disposition))))
          url         (.generatePresignedUrl (:s3-client s3-client) request)]
      (log/info "Signed download link: " url " expires" expiration)
      (.toString url)))

  (file-exists? [_ file-name]
    (.doesObjectExist (:s3-client s3-client) (bucket-name) file-name)))

(defn new-store []
  (map->S3TempFileStore {}))
