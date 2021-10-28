(ns ataru.temp-file-storage.temp-file-store)

(defprotocol TempFileStorage
  (signed-upload-url [this file-name])
  (file-exists? [this file-name]))
