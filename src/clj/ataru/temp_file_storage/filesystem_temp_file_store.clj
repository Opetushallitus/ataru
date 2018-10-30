(ns ataru.temp-file-storage.filesystem-temp-file-store
  (:require [clojure.java.io :as io]
            [ataru.config.core :refer [config]])
  (:import (ataru.temp_file_storage.temp_file_store TempFileStorage)
           (java.io File FileInputStream)))

(defn- base-path []
  (get-in config [:temp-files :filesystem :base-path]))

(defrecord FilesystemTempFileStore []
  TempFileStorage

  (put-file [_ temp-file file-name]
    (let [dir       (io/file (base-path))
          dest-file (io/file (str (base-path) "/" file-name))]
      (.mkdirs dir)
      (io/copy temp-file dest-file)))

  (delete-file [_ file-name]
    (let [file (io/file (str (base-path) "/" file-name))]
      (io/delete-file file true)))

  (get-file [_ file-name]
    (let [path (str (base-path) "/" file-name)]
      (FileInputStream. (io/file path))))

  (file-exists? [_ file-name]
    (let [path (str (base-path) "/" file-name)]
      (.exists (File. path))))

  (filenames-with-prefix [_ prefix]
    (let [directory (File. (base-path))]
      (->>
        (file-seq directory)
        (map #(.getName %))
        (filter #(.startsWith % prefix))))))

(defn new-store []
  (map->FilesystemTempFileStore {}))