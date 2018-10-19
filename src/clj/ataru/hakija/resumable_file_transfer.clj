(ns ataru.hakija.resumable-file-transfer
  (:require [ataru.util.http-util :as http-util]
            [cheshire.core :as json]
            [ataru.config.core :refer [config]]
            [ataru.config.url-helper :refer [resolve-url]]
            [clojure.java.io :as io]
            [pandect.algo.md5 :refer [md5]]
            [ataru.temp-file-storage.temp-file-store :as temp-file-store])
  (:import (java.text Normalizer Normalizer$Form)
           (java.io File)))

(def max-part-size (get-in config [:public-config :attachment-file-part-max-size-bytes] (* 1024 1024)))

(defn- build-file-name
  [file-id file-name parts-count part-number]
  (str file-id "_" part-number "_" parts-count "_"
       (Normalizer/normalize file-name Normalizer$Form/NFD)))

(defn- count-parts
  [file-size]
  (-> (/ file-size max-part-size)
      (Math/ceil)
      (int)))

(defn- store-part
  [file-store file file-part-name]
  (temp-file-store/put-file file-store file file-part-name))

(defn file-part-exists?
  [file-store file-id file-name file-size part-number]
  (let [stored-file-part-name (build-file-name file-id file-name (count-parts file-size) part-number)]
    (temp-file-store/file-exists? file-store stored-file-part-name)))

(defn- combine-files!
  [file-store input-file-names output-file]
  (with-open [output-stream (io/output-stream output-file)]
    (doseq [input-file-name input-file-names]
      (io/copy (temp-file-store/get-file file-store input-file-name) output-stream))
    (doseq [input-file-name input-file-names]
      (temp-file-store/delete-file file-store input-file-name))
    output-file))

(defn- assert-valid-file
  [file file-id file-size]
  (assert (= file-size (.length file)) "invalid combined file size")
  (assert (= file-id (md5 file))) "invalid combined file hash")

(defn- combine-file-parts
  [file-store file-id file-name file-size]
  (let [parts-count     (count-parts file-size)
        file-part-names (map (partial build-file-name file-id file-name parts-count) (range parts-count))
        output-file     (File/createTempFile "combined-file" ".output")]
    (.deleteOnExit output-file)
    (combine-files! file-store file-part-names output-file)
    (assert-valid-file output-file file-id file-size)
    output-file))

(defn- all-parts-exist?
  [file-store file-id file-name file-size]
  (let [parts-count (count-parts file-size)
        parts-exist (map (partial file-part-exists? file-store file-id file-name file-size) (range parts-count))]
    (every? true? parts-exist)))

(defn upload-file-to-liiteri
  [file-store file-id file-name file-size]
  (let [url           (resolve-url :liiteri.files)
        combined-file (combine-file-parts file-store file-id file-name file-size)
        resp          (http-util/do-post url {:multipart [{:name     "file"
                                                           :content  combined-file
                                                           :filename (Normalizer/normalize file-name Normalizer$Form/NFD)}]})]
    (when (= (:status resp) 200)
      (.delete combined-file)
      (-> (:body resp)
          (json/parse-string true)
          (dissoc :version :deleted)))))

(defn store-file-part!
  [file-store file-id file-size part-number file-part]
  (let [num-parts      (count-parts file-size)
        last-part?     (= num-parts (inc part-number))
        file-name      (:filename file-part)
        file           (:tempfile file-part)
        file-part-name (build-file-name file-id file-name num-parts part-number)]
    (store-part file-store file file-part-name)
    (if last-part?
      (if (all-parts-exist? file-store file-id file-name file-size)
        (merge {:status "complete"} {:stored-file (upload-file-to-liiteri file-store file-id file-name file-size)})
        {:status "retransmit"})
      {:status "send-next"})))
