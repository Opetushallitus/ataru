(ns ataru.temp-file-storage.s3-temp-file-store
  (:require [ataru.config.core :refer [config]]
            [ataru.temp-file-storage.temp-file-store :refer [TempFileStorage]]
            [taoensso.timbre :as log])
  (:import java.time.Duration
           (software.amazon.awssdk.services.s3 S3Client)
           (software.amazon.awssdk.services.s3.model GetObjectRequest HeadObjectRequest NoSuchKeyException PutObjectRequest)
           software.amazon.awssdk.services.s3.presigner.S3Presigner
           (software.amazon.awssdk.services.s3.presigner.model GetObjectPresignRequest PresignedGetObjectRequest PresignedPutObjectRequest PutObjectPresignRequest)))

(defn- bucket-name []
  (get-in config [:aws :liiteri-files :bucket]))

(defn- put-request [bucket-name file-name]
  (-> (PutObjectRequest/builder)
      (.bucket bucket-name)
      (.key file-name)
      (.build)))

(defn- get-request [bucket-name file-name content-type content-disposition]
  (-> (GetObjectRequest/builder)
      (.bucket bucket-name)
      (.key file-name)
      (.responseContentType content-type)
      (.responseContentDisposition content-disposition)
      (.build)))

(defn- put-presign-request ^PutObjectPresignRequest [^Duration duration ^PutObjectRequest object-request]
  (-> (PutObjectPresignRequest/builder)
      (.signatureDuration duration)
      (.putObjectRequest object-request)
      (.build)))

(defn- get-presign-request ^GetObjectPresignRequest [^Duration duration ^GetObjectRequest object-request]
  (-> (GetObjectPresignRequest/builder)
      (.signatureDuration duration)
      (.getObjectRequest object-request)
      (.build)))

(defn- presigned-put-request ^PresignedPutObjectRequest [bucket-name file-name]
  (with-open [^S3Presigner presigner (S3Presigner/create)]
    (let [duration        (Duration/ofHours 24)
          object-request  (put-request bucket-name file-name)
          presign-request (put-presign-request duration object-request)]
      (.presignPutObject presigner presign-request))))

(defn- presigned-get-request ^PresignedGetObjectRequest [bucket-name file-name content-type content-disposition]
  (with-open [^S3Presigner presigner (S3Presigner/create)]
    (let [duration        (Duration/ofMinutes 5)
          object-request  (get-request bucket-name file-name content-type content-disposition)
          presign-request (get-presign-request duration object-request)]
      (.presignGetObject presigner presign-request))))

(defrecord S3TempFileStore [s3-client config]
  TempFileStorage

  (signed-upload-url [_ file-name]
    (let [bucket-name   (bucket-name)
          presigned-url (presigned-put-request bucket-name file-name)]
      (log/info "Signed upload link: " (.url presigned-url) " expires" (.expiration presigned-url))
      (.toString (.url presigned-url))))

  (signed-download-url [_ file-key content-type content-disposition]
    (let [bucket-name   (bucket-name)
          presigned-url (presigned-get-request bucket-name file-key content-type content-disposition)]
      (log/info "Signed download link: " (.url presigned-url) " expires" (.expiration presigned-url))
      (.toString (.url presigned-url))))

  (file-exists? [_ file-name]
    (let [^HeadObjectRequest head-request (-> (HeadObjectRequest/builder)
                                              (.bucket (bucket-name))
                                              (.key file-name)
                                              (.build))]
      (try
        (not (nil? (.headObject ^S3Client (:s3-client s3-client) head-request)))
        (catch NoSuchKeyException _
          false)))))

(defn new-store []
  (map->S3TempFileStore {}))
