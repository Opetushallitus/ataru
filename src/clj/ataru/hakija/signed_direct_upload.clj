(ns ataru.hakija.signed-direct-upload
  (:require [ataru.temp-file-storage.temp-file-store :as temp-file-store]
            [taoensso.timbre :as log]))

(defn signed-url-for-direct-upload
  [file-store file-name file-size file-id]
  (log/info "Permission to upload file" file-id "with name" file-name "with size" file-size)
  (if (temp-file-store/file-exists? file-store file-id)
    (throw (RuntimeException. (str "File with" file-id "already exist!")))
    (temp-file-store/signed-upload-url file-store file-id)))
