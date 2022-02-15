(ns ataru.temp-file-storage.temp-file-store)

(defprotocol TempFileStorage
  (signed-upload-url [this file-name])
  (signed-download-url [this file-key content-type content-disposition])
  (file-exists? [this file-name]))
