(ns ataru.files.file-store
  (:require [ataru.config.url-helper :refer [resolve-url]]
            [cheshire.core :as json]
            [ataru.cas.client :as cas]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [clojure.string :as str])
  (:import [java.util.zip ZipOutputStream ZipEntry]))

(defn delete-file [cas-client file-key]
  (let [url  (resolve-url :liiteri.file file-key)
        resp (cas/cas-authenticated-delete cas-client url)]
    (when (= (:status resp) 200)
      (json/parse-string (:body resp) true))))

(defn get-metadata [cas-client file-keys]
  (if (seq file-keys)
    (let [resp (cas/cas-authenticated-post
                 cas-client
                 (resolve-url :liiteri.metadata)
                 {:keys file-keys})]
      (if (= (:status resp) 200)
        (vec (json/parse-string (:body resp) true))
        (throw (new RuntimeException
                    (str "Could not get metadata for keys "
                         (clojure.string/join ", " file-keys)
                         ". Got status " (:status resp)
                         ", body " (:body resp))))))
    file-keys))

(defn get-file [cas-client key]
  (let [url  (resolve-url :liiteri.file key)
        resp (cas/cas-authenticated-get-as-stream cas-client url)
        status (:status resp)]
    (log/info "DEBUG GET file status:" status "key:" key)
    (log/info "DEBUG Headers for file key:" key ":" (:headers resp))

    (if (= status 200)
      {:body                (:body resp)
       :content-disposition (-> resp :headers :content-disposition)}
      (do
        (log/error "Failed to fetch file:" key "status:" status)
        nil))))

(defn- decode-rfc5987
  "Decode RFC5987 percent-encoded filename (very small util). Returns the decoded string or the original."
  [s]
  (try
    (java.net.URLDecoder/decode s "UTF-8")
    (catch Exception _
      s)))

(defn extract-filename
  "Extract filename from Content-Disposition header.
   Returns the filename string or nil.
   Handles:
     - filename=\"foo.jpg\"
     - filename=foo.jpg
     - filename*=UTF-8''foo.jpg  (will be URL-decoded)
   Uses re-find so the filename can appear anywhere in the header string."
  [content-disposition]
  (when content-disposition
    (let [cd content-disposition
          ;; 1) RFC 5987: filename*=UTF-8''foo%20bar.jpg
          rfc5987 (some-> (re-find #"(?i)filename\*\s*=\s*UTF-8''([^;,\s]+)" cd) second)
          ;; 2) quoted filename: filename="foo.jpg"
          quoted  (some-> (re-find #"(?i)filename\s*=\s*\"([^\"]+)\"" cd) second)
          ;; 3) unquoted filename: filename=foo.jpg (up to ; or end)
          unquoted (some-> (re-find #"(?i)filename\s*=\s*([^;,\s]+)" cd) second)]
      (cond
        rfc5987   (decode-rfc5987 rfc5987)
        quoted    quoted
        unquoted  unquoted
        :else     nil))))

(defn generate-filename [filename counter]
  (let [parts     (str/split filename #"\.")
        name      (str/join "." (butlast parts))
        extension (last parts)
        safe-name (apply str (take 240 name))]
    (str safe-name counter "." extension)))

(defn get-file-zip
  [liiteri-cas-client keys output-stream]
  (with-open [zip-out (ZipOutputStream. output-stream)]
    (let [seen-filenames (atom #{})
          counter (atom 0)]
      (doseq [key keys]
        (log/info "DEBUG ZIP processing key:" key)
        (if-let [{:keys [body content-disposition] :as file} (get-file liiteri-cas-client key)]
          (do
            ;; Debug the raw header value (use pr-str so quotes are visible)
            (log/info "DEBUG RAW Content-Disposition for key" key ":" (pr-str content-disposition))
            ;; run extractor and log result
            (let [header-filename (extract-filename content-disposition)]
              (log/info "DEBUG extract-filename ->" (pr-str header-filename))

              (if header-filename
                (let [base-filename  (generate-filename header-filename "")
                      final-filename (if (contains? @seen-filenames base-filename)
                                       (generate-filename header-filename (swap! counter inc))
                                       base-filename)]
                  (swap! seen-filenames conj final-filename)
                  (log/info "DEBUG Adding ZIP entry:" final-filename "for key:" key)
                  (.putNextEntry zip-out (ZipEntry. final-filename))
                  (with-open [file-stream body]
                    (io/copy file-stream zip-out))
                  (.closeEntry zip-out))
                (do
                  ;; Header present but extraction failed — log full headers map for inspection
                  (log/error "Missing or unparsable Content-Disposition for key:" key)
                  (log/error "Full file map for key:" key ":" (pr-str (select-keys file [:content-disposition])))
                  ;; skip adding this file to zip
                  nil))))
          (log/error "Could not fetch file for key:" key))))))
