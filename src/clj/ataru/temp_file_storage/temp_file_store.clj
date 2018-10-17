(ns ataru.temp-file-storage.temp-file-store)

(defprotocol TempFileStorage
  (get-file [this file-name])
  (put-file [this temp-file file-name])
  (delete-file [this file-name])
  (file-exists? [this file-name]))
