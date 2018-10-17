(ns ataru.temp-file-storage.s3-temp-file-store
  (:import (ataru.temp_file_storage.temp_file_store TempFileStorage)
           (java.io File FileOutputStream)
           (org.apache.commons.io IOUtils)))

(defn- bucket-name [config]
  (get-in config [:file-store :s3 :bucket]))

(defrecord S3TempFileStore [s3-client config]
  TempFileStorage

  (put-file [_ file file-name]
    (.putObject (:s3-client s3-client) (bucket-name config) file-name file))

  (delete-file [_ file-name]
    (.deleteObject (:s3-client s3-client) (bucket-name config) file-name))

  (get-file [_ file-name]
    (-> (.getObject (:s3-client s3-client) (bucket-name config) file-name)
        (.getObjectContent)))

  (file-exists? [_ file-name]
    (.doesObjectExist (:s3-client s3-client) (bucket-name config) file-name)))

(defn new-store []
  (map->S3TempFileStore {}))