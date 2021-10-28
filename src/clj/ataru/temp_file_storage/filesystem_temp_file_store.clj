(ns ataru.temp-file-storage.filesystem-temp-file-store
  (:require [ataru.config.core :refer [config]]
            [ataru.temp-file-storage.temp-file-store :refer [TempFileStorage]])
  (:import (java.io File)))

(defn- base-path []
  (get-in config [:temp-files :filesystem :base-path]))

(defrecord FilesystemTempFileStore []
  TempFileStorage

  (signed-upload-url [_ _]
    (throw (RuntimeException. "Signed URL unimplemented on file system store!")))

  (file-exists? [_ file-name]
    (let [path (str (base-path) "/" file-name)]
      (.exists (File. path)))))

(defn new-store []
  (map->FilesystemTempFileStore {}))