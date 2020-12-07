(ns ataru.util.client-error
  (:require [taoensso.timbre :as log]
            [schema.core :as s]))

(s/defschema ClientError {:error-message s/Str
                          :url s/Str
                          :line s/Int
                          :col s/Int
                          :description s/Str
                          :user-agent s/Str
                          :stack s/Str})


(defn log-client-error [error-details]
  (prn "vsdfvf")
  (log/error (str "Error from client browser:\n"
                  (:description error-details) "\n"
                  (:error-message error-details) "\n"
                  (:url error-details) "\n"
                  "line: " (:line error-details) " column: " (:col error-details) "\n"
                  "user-agent: " (:user-agent error-details) "\n"
                  "stack trace: " (:stack error-details))))
