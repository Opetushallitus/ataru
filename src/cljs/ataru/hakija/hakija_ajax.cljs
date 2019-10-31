(ns ataru.hakija.hakija-ajax
  (:require [re-frame.core :as re-frame]
            [ataru.cljs-util :as util]
            [ajax.core :as ajax]))

(defn http [{:keys [method
                    url
                    post-data
                    headers
                    handler
                    progress-handler
                    error-handler
                    started-handler]}]
  (cond-> ((case method
             :get    ajax/GET
             :post   ajax/POST
             :put    ajax/PUT
             :delete ajax/DELETE)
           url
           (merge {:response-format (ajax/ring-response-format
                                     {:format (ajax/json-response-format
                                               {:keywords? true})})
                   :headers         (merge headers
                                           {"Caller-Id" (aget js/config "hakija-caller-id")}
                                           (when (util/include-csrf-header? method)
                                             {"CSRF" (util/csrf-token)}))
                   :handler         (fn [response] (re-frame/dispatch (conj handler response)))
                   :error-handler   (fn [response] (re-frame/dispatch (conj (if (some? error-handler)
                                                                              error-handler
                                                                              [:application/default-handle-error])
                                                                            (:response response))))}
                  (when (some? progress-handler)
                    {:progress-handler (fn [event] (re-frame/dispatch (conj progress-handler event)))})
                  (when (some? post-data))
                  {:format :json
                   :params post-data}))
          (some? started-handler)
          started-handler))

(re-frame/reg-fx
  :http http)

(re-frame/reg-fx :http-abort ajax/abort)


