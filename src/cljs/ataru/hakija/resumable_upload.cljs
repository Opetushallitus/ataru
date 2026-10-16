(ns ataru.hakija.resumable-upload
  (:require [ajax.core :refer [GET PUT]]
            [re-frame.core :refer [dispatch reg-event-fx reg-event-db ->interceptor]]
            [goog.crypt :as crypt]
            [goog.crypt.Md5]
            [cljs-time.core :as c]
            [ataru.filename-normalizer :as normalizer]
            [ataru.hakija.attachment-path :as attachment-path]
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

(defn- dispatch-failure
  "Aborting an in-flight request is a user cancellation, anything else is a transfer failure."
  [{:keys [error-handler cancel-handler]} {:keys [status failure]}]
  (if (and (= :aborted failure) (some? cancel-handler))
    (dispatch cancel-handler)
    (dispatch (conj error-handler status))))

(def ^:private stop-when-abandoned
  "Runs before each step of an upload, whose event carries the handlers map as its
   fourth element. Once the attachment row is gone, because the user cancelled the
   upload or removed the question group row it was in, the step is skipped and the
   cancellation reported instead. A cancel can land while no request is in flight to
   abort (hashing, between requests), so it is this check that stops the upload then."
  (->interceptor
   :id :stop-when-abandoned
   :before (fn [context]
             (let [db                                          (get-in context [:coeffects :db])
                   {:keys [field-id upload-id cancel-handler]} (get-in context [:coeffects :event 3])]
               (if (attachment-path/path-by-upload-id db (keyword field-id) upload-id)
                 context
                 (assoc context
                        :queue   []
                        :effects (cond-> {} (some? cancel-handler) (assoc :dispatch cancel-handler))))))))

(defn upload-file
  [url finished-url file field-id upload-id application-attachments-id handlers]
  {:pre [(every? (complement clojure.string/blank?) [url field-id application-attachments-id])
         (every? some? [file upload-id handlers])]}
  (hex-md5-hash
   file
   (fn [md5-hash]
     (dispatch [:application-file-upload/fetch-signed-url-for-upload
                url
                finished-url
                (assoc handlers :field-id field-id :upload-id upload-id)
                (str
                 application-attachments-id "-"
                 field-id "-"
                 upload-id "-"
                 md5-hash)
                file
                0]))))

(reg-event-fx
  :application-file-upload/mark-upload-delivered
  [stop-when-abandoned]
  (fn [_ [_ _ finished-url {:keys [handler error-handler started-handler] :as handlers} file-id file]]
    (let [filename                (normalizer/normalize-filename (.-name file))
          size                    (.-size file)
          response-handler        (fn [xhrio]
                                    (let [status (or (:status xhrio) (.getStatus xhrio))]
                                      (case status
                                        201 (dispatch (conj handler {:filename filename
                                                                     :key      file-id
                                                                     :size     size}))
                                        (dispatch (conj error-handler status)))))
          req                     (PUT (str finished-url
                                            "?file-id=" file-id
                                            "&file-size=" size
                                            "&file-name=" filename)
                                    {:response-format {:read        identity
                                                       :description "raw"}
                                     :params          {:file-id          file-id
                                                       :file-size        (.-size file)
                                                       :file-name        (normalizer/normalize-filename (.-name file))}
                                     :headers         {"Caller-Id" (aget js/config "hakija-caller-id")
                                                       "CSRF"      (util/csrf-token)}
                                     :handler         response-handler
                                     :error-handler   (partial dispatch-failure handlers)})]
      {:dispatch (conj started-handler req)})))

(reg-event-fx
  :application-file-upload/upload-using-signed-url
  [stop-when-abandoned]
  (fn [_ [_ url finished-url {:keys [progress-handler error-handler started-handler] :as handlers} file-id file file-part-number]]
    (let [mark-upload-delivered   [:application-file-upload/mark-upload-delivered url finished-url handlers file-id file]
          response-handler        (fn [xhrio]
                                    (let [status (or (:status xhrio) (.getStatus xhrio))]
                                      (case status
                                        200 (dispatch mark-upload-delivered)
                                        (dispatch (conj error-handler status)))))
          req                     (PUT url
                                    {:response-format {:read        identity
                                                       :description "raw"}
                                     :handler          response-handler
                                     :error-handler    (partial dispatch-failure handlers)
                                     :progress-handler #(dispatch (conj progress-handler % file-part-number))
                                     :body             file})]
      {:dispatch (conj started-handler req)})))

(reg-event-fx
  :application-file-upload/fetch-signed-url-for-upload
  [stop-when-abandoned]
  (fn [_ [_ url finished-url {:keys [started-handler] :as handlers} file-id file file-part-number]]
    (let [params                 (merge json-params
                                        {:params        {:file-id          file-id
                                                         :file-size        (.-size file)
                                                         :file-name        (normalizer/normalize-filename (.-name file))
                                                         :file-part-number file-part-number}
                                         :handler       (fn [{:keys [signed-url key]}]
                                                          (dispatch [:application-file-upload/upload-using-signed-url signed-url finished-url handlers key file file-part-number]))
                                         :headers       {"Caller-Id" (aget js/config "hakija-caller-id")
                                                         "CSRF"      (util/csrf-token)}
                                         :error-handler (partial dispatch-failure handlers)})
          req                    (GET url params)]
      {:dispatch (conj started-handler req)})))

(reg-event-db
  :application-file-upload/handle-attachment-progress-resumable
  (fn [db [_ field-descriptor upload-id evt file-part-number]]
    (if-let [path (and (.-lengthComputable evt)
                       (attachment-path/path-by-upload-id db
                                                          (keyword (:id field-descriptor))
                                                          upload-id))]
      (let [now           (c/now)
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
