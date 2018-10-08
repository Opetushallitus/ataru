(ns ataru.hakija.resumable-upload
  (:require [ataru.hakija.hakija-ajax :as ajax]
            [ajax.core :refer [GET POST]]
            [re-frame.core :refer [dispatch reg-event-fx]]
            [goog.crypt :as crypt]
            [goog.crypt.Md5 :as Md5]))

(def ^:private json-params {:format :json :response-format :json :keywords? true})
(def max-part-size (* 1024 100))

(defn- hex-md5-hash
  [array-buffer]
  (let [md5 (goog.crypt.Md5.)]
    (.update md5 (js/Uint8Array. array-buffer))
    (crypt/byteArrayToHex (.digest md5))))

(defn- actually-upload
  [_ [_ url file-id complete-dispatch error-dispatch file file-part-number]]
  (let [file-part-start (* max-part-size file-part-number)
        file-part-end   (min (* max-part-size (inc file-part-number)) (.-size file))
        file-part       (.slice file file-part-start file-part-end)
        file-name       (.-name file)]
    (POST url (merge json-params
                     {:body          (doto (js/FormData.)
                                       (.append "file-part" file-part file-name)
                                       (.append "file-id" file-id)
                                       (.append "file-size" (.-size file))
                                       (.append "file-part-number" file-part-number))
                      :handler       (fn [{:keys [status stored-file] :as resp}]
                                       (case status
                                         "send-next" (dispatch [:application-file-upload/attempt-file-part-upload
                                                                url
                                                                file-id
                                                                complete-dispatch
                                                                error-dispatch
                                                                file
                                                                (inc file-part-number)])
                                         "retransmit" (dispatch error-dispatch)
                                         "complete" (dispatch (conj complete-dispatch stored-file))))
                      :error-handler #(dispatch error-dispatch)}))
    nil))

(defn- attempt-part-upload
  [_ [_ url file-id complete-dispatch error-dispatch file file-part-number]]
  (GET url (merge json-params
                  {:params        {:file-id          file-id
                                   :file-size        (.-size file)
                                   :file-name        (.-name file)
                                   :file-part-number file-part-number}
                   :handler       #(dispatch [:application-file-upload/attempt-file-part-upload url file-id complete-dispatch error-dispatch file (inc file-part-number)])
                   :error-handler (fn [{:keys [status]}]
                                    (if (= 404 status)
                                      ; TODO if file found and last part, retransmit anyway
                                      (dispatch [:application-file-upload/actually-upload-file-part url file-id complete-dispatch error-dispatch file file-part-number])
                                      (dispatch error-dispatch)))}))
  nil)

(defn upload-file
  [url file complete-dispatch error-dispatch]
  (let [fr (js/FileReader.)]
    (.addEventListener fr "loadend" #(dispatch [:application-file-upload/attempt-file-part-upload url
                                                (hex-md5-hash (.-result fr)) complete-dispatch error-dispatch file 0]))
    (.readAsArrayBuffer fr file)))


(reg-event-fx
  :application-file-upload/attempt-file-part-upload
  attempt-part-upload)

(reg-event-fx
  :application-file-upload/actually-upload-file-part
  actually-upload)