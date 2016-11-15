(ns ataru.hakija.hakija-ajax
  (:require [re-frame.core :refer [dispatch reg-fx]]
            [cljs.core.match :refer-macros [match]]
            [ajax.core :refer [GET POST]])
  (:refer-clojure :exclude [get]))

(def ^:private json-params {:format :json :response-format :json :keywords? true})

(defn- handler-fn [handler-kw & {:keys [default]}]
  (fn [response]
    (let [dispatch-vec (match handler-kw
                         (_ :guard nil?)
                         [default response]

                         (handler-vec :guard vector?)
                         (conj handler-vec response)

                         :else
                         [handler-kw response])]
      (dispatch dispatch-vec))))

(defn- params [handler-kw error-handler-kw]
  (merge
    {:handler       (handler-fn handler-kw :default :application/default-http-ok-handler)
     :error-handler (handler-fn error-handler-kw :default :application/default-handle-error)}
    json-params))

(defn get [path & [handler-kw error-handler-kw]]
  (GET path (params handler-kw error-handler-kw)))

(defn post [path post-data & [handler-kw error-handler-kw]]
  (POST path (merge {:params post-data} (params handler-kw error-handler-kw))))

(reg-fx
  :http
  (fn [{:keys [method post-data url handler error-handler]}]
    (let [f (case method
              :post (partial post url post-data)
              :get  (partial get url))]
      (f handler error-handler))))

