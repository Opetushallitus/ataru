(ns ataru.hakija.resumable-file-transfer
  (:require [cheshire.core :as json]
            [ataru.config.core :refer [config]]
            [ataru.config.url-helper :refer [resolve-url]]
            [ataru.util.http-util :as http]
            [clojure.java.io :as io]
            [pandect.algo.md5 :refer [md5]]
            [ataru.temp-file-storage.temp-file-store :as temp-file-store]
            [clojure.core.match :refer [match]]
            [taoensso.timbre :as log]
            [string-normalizer.filename-normalizer :as normalizer])
  (:import (java.io File FileInputStream)))

(def max-part-size (get-in config [:public-config :attachment-file-part-max-size-bytes] (* 1024 1024)))

(defn- file-name-prefix
  [file-id file-name parts-count]
  (str file-id "_" parts-count "_" (normalizer/normalize-filename file-name)))

(defn- build-file-name
  [file-id file-name parts-count part-number]
  (str (file-name-prefix file-id file-name parts-count) "_" part-number))

(defn- count-parts
  [file-size]
  (-> (/ file-size max-part-size)
      (Math/ceil)
      (int)))

(defn- md5-from-file-id
  [file-id]
  (let [md5-length 32]
    (subs file-id (- (count file-id) md5-length))))

(defn- assert-valid-file
  [file file-id file-size]
  (assert (= file-size (.length file)) "invalid combined file size")
  (assert (= (md5-from-file-id file-id) (md5 file))) "invalid combined file hash")

(defn- assert-valid-part-number
  [num-parts part-number]
  (assert (< part-number num-parts) "Invalid file part number"))

(defn- store-part
  [file-store file file-part-name]
  (temp-file-store/put-file file-store file file-part-name))

(defn file-part-exists?
  [file-store file-id file-name file-size part-number]
  (let [num-parts             (count-parts file-size)
        stored-file-part-name (build-file-name file-id file-name num-parts part-number)]
    (assert-valid-part-number num-parts part-number)
    [(temp-file-store/file-exists? file-store stored-file-part-name)
     (= (+ part-number 2) num-parts)]))

(defn- combine-files!
  [file-store input-file-names output-file]
  (log/info "Combining" (count input-file-names) "files to" output-file)
  (with-open [output-stream (io/output-stream output-file)]
    (doseq [input-file-name input-file-names]
      (io/copy (temp-file-store/get-file file-store input-file-name) output-stream))
    (future (doseq [input-file-name input-file-names]
              (temp-file-store/delete-file file-store input-file-name)))
    output-file))

(defn- with-temp-file
  [prefix suffix f]
  (let [file (File/createTempFile prefix suffix)]
    (try
      (f file)
      (finally
        (.delete file)))))

(defn- combine-file-parts!
  [file-store output-file file-id file-name file-size]
  (let [parts-count     (count-parts file-size)
        file-part-names (map (partial build-file-name file-id file-name parts-count) (range parts-count))]
    (combine-files! file-store file-part-names output-file)
    (assert-valid-file output-file file-id file-size)))

(defn- all-parts-exist?
  [file-store file-id file-name file-size]
  (let [parts-count    (count-parts file-size)
        prefix         (file-name-prefix file-id file-name (count-parts file-size))
        required-files (->> (range parts-count)
                            (map (partial build-file-name file-id file-name parts-count))
                            (set))
        existing-files (set (temp-file-store/filenames-with-prefix file-store prefix))]
    (clojure.set/superset? existing-files required-files)))

(defn upload-file-to-liiteri
  [file file-name]
  (log/info "Uploading to liiteri:" file-name (.length file) "bytes")
  (let [url                         (resolve-url :liiteri.files)
        start-time                  (System/currentTimeMillis)
        {:keys [status body error]} (http/do-post url {:socket-timeout (* 1000 60 10)
                                                       :cookie-policy  :standard
                                                       :multipart      [{:part-name "file"
                                                                         :content   (FileInputStream. file)
                                                                         :name      (normalizer/normalize-filename file-name)}]})]
    (cond (= status 200)
          (do
            (log/info "Uploaded file" file-name "to liiteri in" (- (System/currentTimeMillis) start-time) "ms:" body)
            [:complete (dissoc (json/parse-string body true) :version :deleted)])
          (= status 400)
          (do
            (log/info "Error uploading file to liiteri at " url " :" file-name status error body)
            [:bad-request nil])
          :else
          (do
            (log/error "Error uploading file to liiteri at " url " :" file-name status error body)
            [:liiteri-error nil]))))

(defn store-file-part!
  [file-store file-id file-size part-number file-part]
  (let [num-parts      (count-parts file-size)
        last-part?     (= num-parts (inc part-number))
        file-name      (:filename file-part)
        file           (:tempfile file-part)
        file-part-name (build-file-name file-id file-name num-parts part-number)]
    (if (= num-parts 1)
      (upload-file-to-liiteri file file-name)
      (do
        (assert-valid-part-number num-parts part-number)
        (store-part file-store file file-part-name)
        (if last-part?
          (if (all-parts-exist? file-store file-id file-name file-size)
            (with-temp-file "combined-file" ".output"
              (fn [file]
                (combine-file-parts! file-store file file-id file-name file-size)
                (upload-file-to-liiteri file file-name)))
            [:retransmit nil])
          [:send-next nil])))))
