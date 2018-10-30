(ns ataru.temp-file-storage.s3-temp-file-store
  (:require [ataru.config.core :refer [config]]
            [clojure.tools.logging :as log])
  (:import (ataru.temp_file_storage.temp_file_store TempFileStorage)
           (java.io File FileOutputStream)
           (org.apache.commons.io IOUtils)))

(defn- bucket-name []
  (get-in config [:aws :temp-files :bucket]))

(defrecord S3TempFileStore [s3-client config]
  TempFileStorage

  (put-file [_ file file-name]
    (log/info "Putting file:" (bucket-name) "/" file-name (.length file))
    (.putObject (:s3-client s3-client) (bucket-name) file-name file))

  (delete-file [_ file-name]
    (log/info "Deleting file:" (bucket-name) "/" file-name)
    (.deleteObject (:s3-client s3-client) (bucket-name) file-name))

  (get-file [_ file-name]
    (log/info "Getting file:" (bucket-name) "/" file-name)
    (-> (.getObject (:s3-client s3-client) (bucket-name) file-name)
        (.getObjectContent)))

  (file-exists? [_ file-name]
    (.doesObjectExist (:s3-client s3-client) (bucket-name) file-name))

  (filenames-with-prefix [_ prefix]
    (log/info "Getting filenames:" (bucket-name) prefix)
    (->>
      (.listObjects (:s3-client s3-client) (bucket-name) prefix)
      (.getObjectSummaries)
      (map #(.getKey %)))))

(defn new-store []
  (map->S3TempFileStore {}))