(ns ataru.hakija.resumable-upload
  (:require [ajax.core :refer [GET POST]]
            [re-frame.core :refer [dispatch reg-event-fx reg-event-db]]
            [goog.crypt :as crypt]
            [goog.crypt.Md5 :as Md5]
            [cljs-time.core :as c]))

(def ^:private json-params {:format :json :response-format :json :keywords? true})
(def max-part-size
  (get (js->clj js/config) "attachment-file-part-max-size-bytes" (* 1024 1024 5)))

(defn- hex-md5-hash
  [array-buffer]
  (let [md5 (goog.crypt.Md5.)]
    (.update md5 (js/Uint8Array. array-buffer))
    (crypt/byteArrayToHex (.digest md5))))

(defn upload-file
  [url file field-id attachment-idx application-attachments-id handlers]
  {:pre [(every? (complement clojure.string/blank?) [url field-id application-attachments-id])
         (every? some? [file attachment-idx handlers])]}
  (let [fr (js/FileReader.)]
    (.addEventListener
      fr
      "loadend"
      (fn []
        (let [file-buffer (.-result fr)
              id          (str
                            application-attachments-id "-"
                            field-id "-"
                            attachment-idx "-"
                            (hex-md5-hash file-buffer))
              dispatch-kw (if (<= (.-size file) max-part-size)
                            :application-file-upload/upload-file-part
                            :application-file-upload/check-file-part-status-and-upload)]
          (dispatch [dispatch-kw url handlers id file 0]))))
    (.readAsArrayBuffer fr file)))

(reg-event-fx
  :application-file-upload/check-file-part-status-and-upload
  (fn [_ [_ url {:keys [error-handler started-handler] :as handlers} file-id file file-part-number]]
    (let [have-file-dispatch     [:application-file-upload/check-file-part-status-and-upload url handlers file-id file (inc file-part-number)]
          not-have-file-dispatch [:application-file-upload/upload-file-part url handlers file-id file file-part-number]
          last-file-dispatch     [:application-file-upload/upload-file-part url handlers file-id file (inc file-part-number)]
          params                 (merge json-params
                                        {:params        {:file-id          file-id
                                                         :file-size        (.-size file)
                                                         :file-name        (.-name file)
                                                         :file-part-number file-part-number}
                                         :handler       (fn [{:keys [next-is-last]}]
                                                          (if next-is-last
                                                            (dispatch last-file-dispatch)
                                                            (dispatch have-file-dispatch)))
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
                                          :handler          response-handler
                                          :error-handler    response-handler
                                          :progress-handler #(dispatch (conj progress-handler % file-part-number))
                                          :body             (doto (js/FormData.)
                                                              (.append "file-part" file-part (.-name file))
                                                              (.append "file-id" file-id)
                                                              (.append "file-size" (.-size file))
                                                              (.append "file-part-number" file-part-number))})
          req                     (POST url params)]
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
            speed         (* 1000
                             (/ (- uploaded-size prev-uploaded)
                                (c/in-millis (c/interval last-progress now))))]
        (-> db
            (assoc-in (conj path :uploaded-size) uploaded-size)
            (assoc-in (conj path :last-progress) now)
            (assoc-in (conj path :speed) speed)))
      db)))
