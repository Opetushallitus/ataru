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
  (error error-details))
