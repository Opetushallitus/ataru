(ns ataru.middleware.user-feedback
  (:require
   [ring.util.http-response :refer [bad-request]])
  (:import
   (clojure.lang ExceptionInfo)))

(defn user-feedback-exception
  "Exception which should deliver an error message (in finnish) all the way to the browser"
  ; We can make this localized later if needed (label map with :fi etc. as keys)
  [message]
  (ex-info message {:type :user-feedback-exception}))

(defn wrap-user-feedback
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch ExceptionInfo e
        (if (not= :user-feedback-exception (-> e ex-data :type))
          ;; We only want to catch our specific :user-feedback-exception and throw anything else up the stack
          (throw e)
          (bad-request {:error (.getMessage e)}))))))
