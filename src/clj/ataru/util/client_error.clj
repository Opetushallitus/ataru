(ns ataru.util.client-error
  (:require [taoensso.timbre :refer [error]]
            [schema.core :as s]))

(s/defschema ClientError {:error-message s/Str
                          :url s/Str
                          :line s/Int
                          :col s/Int
                          :user-agent s/Str
                          :stack s/Str})


(defn log-client-error [error-details]
  (error "Error from client browser:")
  (error (:error-message error-details))
  (error (:url error-details))
  (error "line:" (:line error-details) "column:" (:col error-details))
  (error "user-agent:" (:user-agent error-details))
  (error "stack trace:" (:stack error-details)))
