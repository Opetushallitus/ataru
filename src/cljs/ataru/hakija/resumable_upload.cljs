(ns ataru.hakija.resumable-upload
  (:require [ajax.core :refer [GET POST]]
            [re-frame.core :refer [dispatch reg-event-fx reg-event-db]]
            [goog.crypt :as crypt]
            [goog.crypt.Md5 :as Md5]
            [cljs-time.core :as c]))

(def ^:private json-params {:format :json :response-format :json :keywords? true})
(def max-part-size (* 1024 100))

(defn- hex-md5-hash
  [array-buffer]
  (let [md5 (goog.crypt.Md5.)]
    (.update md5 (js/Uint8Array. array-buffer))
    (crypt/byteArrayToHex (.digest md5))))

(defn upload-file
  [url file handlers]
  (let [fr (js/FileReader.)]
    (.addEventListener fr "loadend" #(dispatch [:application-file-upload/check-file-part-status-and-upload url handlers (hex-md5-hash (.-result fr)) file 0]))
    (.readAsArrayBuffer fr file)))

(reg-event-fx
  :application-file-upload/check-file-part-status-and-upload
  (fn [_ [_ url {:keys [error-handler started-handler] :as handlers} file-id file file-part-number]]
    {:http }
    (let [req (GET url (merge json-params
                              {:params        {:file-id          file-id
                                               :file-size        (.-size file)
                                               :file-name        (.-name file)
                                               :file-part-number file-part-number}
                               :handler       (fn [{:keys [file-exists]}]
                                                (if file-exists
                                                  (dispatch [:application-file-upload/check-file-part-status-and-upload url handlers file-id file (inc file-part-number)])
                                                  (dispatch [:application-file-upload/upload-file-part url handlers file-id file file-part-number])))
                               :error-handler #(dispatch (conj error-handler %))}))]
      (dispatch (conj started-handler req)))))

(reg-event-fx
  :application-file-upload/upload-file-part
  (fn [_ [_ url {:keys [handler started-handler progress-handler error-handler] :as handlers} file-id file file-part-number]]
    (println "do upload" file file-part-number)
    (let [file-part-start (* max-part-size file-part-number)
          file-part-end   (min (* max-part-size (inc file-part-number)) (.-size file))
          file-part       (.slice file file-part-start file-part-end)
          file-name       (.-name file)
          params          (merge json-params
                                 {:handler          (fn [{:keys [status stored-file]}]
                                                      (case status
                                                        "send-next" (dispatch [:application-file-upload/check-file-part-status-and-upload
                                                                               url
                                                                               handlers
                                                                               file-id
                                                                               file
                                                                               (inc file-part-number)])
                                                        "retransmit" (dispatch error-handler)
                                                        "complete" (dispatch (conj handler stored-file))))
                                  :error-handler    #(dispatch (conj error-handler %))
                                  :progress-handler #(dispatch (conj progress-handler % file-part-number))
                                  :body             (doto (js/FormData.)
                                                      (.append "file-part" file-part file-name)
                                                      (.append "file-id" file-id)
                                                      (.append "file-size" (.-size file))
                                                      (.append "file-part-number" file-part-number))})
          req             (POST url params)]
      (dispatch (conj started-handler req)))))

(reg-event-db
  :application/handle-attachment-progress-resumable
  (fn [db [_ field-descriptor attachment-idx question-group-idx evt file-part-number]]
    (if (.-lengthComputable evt)
      (let [now           (c/now)
            path          (cond-> [:application :answers (keyword (:id field-descriptor)) :values]
                                  (some? question-group-idx)
                                  (conj question-group-idx)
                                  true
                                  (conj attachment-idx))
            prev-uploaded (get-in db (conj path :uploaded-size) 0)
            uploaded-size (+ (.-loaded evt) (* file-part-number max-part-size))
            last-progress (get-in db (conj path :last-progress))
            speed         (* 1000
                             (/ (- uploaded-size prev-uploaded)
                                (c/in-millis (c/interval last-progress now))))]
        (println "progress" uploaded-size)
        (-> db
            (assoc-in (conj path :uploaded-size) uploaded-size)
            (assoc-in (conj path :last-progress) now)
            (assoc-in (conj path :speed) speed)))
      db)))