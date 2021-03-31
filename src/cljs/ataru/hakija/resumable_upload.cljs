(ns ataru.hakija.resumable-upload
  (:require [ajax.core :refer [GET POST]]
            [re-frame.core :refer [dispatch reg-event-fx reg-event-db]]
            [goog.crypt :as crypt]
            [goog.crypt.Md5]
            [cljs-time.core :as c]
            [string-normalizer.filename-normalizer :as normalizer]
            [ataru.cljs-util :as util]
            [clojure.string]))

(def ^:private json-params {:format :json :response-format :json :keywords? true})
(def max-part-size
  (get (js->clj js/config) "attachment-file-part-max-size-bytes" (* 1024 1024 5)))

(defn- hex-md5-hash
  ([file cb]
   (hex-md5-hash file cb (new goog.crypt.Md5) 0))
  ([file cb md5 i]
   (let [start (* i max-part-size)
         end   (min (* (inc i) max-part-size) (.-size file))
         fr    (new js/FileReader)]
     (.addEventListener fr
                        "loadend"
                        (fn []
                          (.update md5 (new js/Uint8Array (.-result fr)))
                          (if (< end (.-size file))
                            (hex-md5-hash file cb md5 (inc i))
                            (cb (crypt/byteArrayToHex (.digest md5))))))
     (.readAsArrayBuffer fr (.slice file start end)))))

(defn upload-file
  [url file field-id attachment-idx application-attachments-id handlers]
  {:pre [(every? (complement clojure.string/blank?) [url field-id application-attachments-id])
         (every? some? [file attachment-idx handlers])]}
  (hex-md5-hash
   file
   (fn [md5-hash]
     (dispatch [(if (<= (.-size file) max-part-size)
                  :application-file-upload/upload-file-part
                  :application-file-upload/check-file-part-status-and-upload)
                url
                handlers
                (str
                 application-attachments-id "-"
                 field-id "-"
                 attachment-idx "-"
                 md5-hash)
                file
                0]))))

(reg-event-fx
  :application-file-upload/check-file-part-status-and-upload
  (fn [_ [_ url {:keys [error-handler started-handler] :as handlers} file-id file file-part-number]]
    (let [have-file-dispatch     [:application-file-upload/check-file-part-status-and-upload url handlers file-id file (inc file-part-number)]
          not-have-file-dispatch [:application-file-upload/upload-file-part url handlers file-id file file-part-number]
          last-file-dispatch     [:application-file-upload/upload-file-part url handlers file-id file (inc file-part-number)]
          params                 (merge json-params
                                        {:params        {:file-id          file-id
                                                         :file-size        (.-size file)
                                                         :file-name        (normalizer/normalize-filename (.-name file))
                                                         :file-part-number file-part-number}
                                         :handler       (fn [{:keys [next-is-last]}]
                                                          (if next-is-last
                                                            (dispatch last-file-dispatch)
                                                            (dispatch have-file-dispatch)))
                                         :headers          {"Caller-Id" (aget js/config "hakija-caller-id")
                                                            "CSRF"      (util/csrf-token)}
                                         :error-handler (fn [{:keys [status]}]
                                                          (if (= status 404)
                                                            (dispatch not-have-file-dispatch)
                                                            (dispatch (conj error-handler status))))})
          req                    (GET url params)]
      {:dispatch (conj started-handler req)})))

(defn- get-xhrio-json
  [xhrio]
  (js->clj (.parse js/JSON (.getResponse xhrio)) :keywordize-keys true))

(reg-event-fx
  :application-file-upload/upload-file-part
  (fn [_ [_ url {:keys [handler started-handler progress-handler error-handler] :as handlers} file-id file file-part-number]]
    (let [file-part-start         (* max-part-size file-part-number)
          file-part-end           (min (* max-part-size (inc file-part-number)) (.-size file))
          file-part               (.slice file file-part-start file-part-end)
          send-next-part-dispatch [:application-file-upload/upload-file-part url handlers file-id file (inc file-part-number)]
          response-handler        (fn [xhrio]
                                    (let [status (or (:status xhrio) (.getStatus xhrio))]
                                      (case status
                                        200 (dispatch send-next-part-dispatch)
                                        201 (dispatch (conj handler (:stored-file (get-xhrio-json xhrio))))
                                        (dispatch (conj error-handler status)))))
          params                  (merge json-params
                                         {:response-format  {:read        identity
                                                             :description "raw"}
                                          :headers          {"Caller-Id" (aget js/config "hakija-caller-id")
                                                             "CSRF"      (util/csrf-token)}
                                          :handler          response-handler
                                          :error-handler    response-handler
                                          :progress-handler #(dispatch (conj progress-handler % file-part-number))
                                          :body             (doto (js/FormData.)
                                                              (.append "file-part" file-part (normalizer/normalize-filename (.-name file)))
                                                              (.append "file-id" file-id)
                                                              (.append "file-size" (.-size file))
                                                              (.append "file-part-number" file-part-number))})
          req                     (POST (str url
                                             "?file-id=" file-id
                                             "&file-size=" (.-size file)
                                             "&file-part=" file-part (normalizer/normalize-filename (.-name file))
                                             "&file-part-number=" file-part-number) params)]
      {:dispatch (conj started-handler req)})))

(reg-event-db
  :application-file-upload/handle-attachment-progress-resumable
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
            speed         (if (and last-progress uploaded-size prev-uploaded)
                            (* 1000
                               (/ (- uploaded-size prev-uploaded)
                                  (if last-progress
                                    (c/in-millis (c/interval last-progress now)) nil)))
                            0)]
        (-> db
            (assoc-in (conj path :uploaded-size) uploaded-size)
            (assoc-in (conj path :last-progress) now)
            (assoc-in (conj path :speed) speed)))
      db)))
