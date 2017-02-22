(ns ataru.hakija.hakija-ajax
  (:require [re-frame.core :refer [dispatch reg-fx]]
            [ataru.cljs-util :as util]
            [cljs.core.match :refer-macros [match]]
            [ajax.core :refer [GET POST PUT raw-response-format]])
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
  {:handler       (handler-fn handler-kw :default :application/default-http-ok-handler)
   :error-handler (handler-fn error-handler-kw :default :application/default-handle-error)})

(defn get [path & [handler-kw error-handler-kw]]
  (GET path (merge (params handler-kw error-handler-kw) json-params)))

(defn post [path post-data & [handler-kw error-handler-kw body]]
  (let [params (cond-> (params handler-kw error-handler-kw)
                 (some? body) (merge {:body body :response-format :json :keywords? true})
                 (some? post-data) (merge {:params post-data} json-params)
                 (util/include-csrf-header? :post) (assoc-in [:headers "CSRF"] (util/csrf-token)))]
    (POST path params)))

(defn put [path post-data & [handler-kw error-handler-kw body]]
  (let [params (cond-> (params handler-kw error-handler-kw)
                 (some? body) (merge {:body body :response-format :json :keywords? true})
                 (some? post-data) (merge {:params post-data} json-params)
                 (util/include-csrf-header? :put) (assoc-in [:headers "CSRF"] (util/csrf-token)))]
    (PUT path params)))

(reg-fx
  :http
  (fn [{:keys [method post-data url handler error-handler body]}]
    (let [f (case method
              :post (partial post url post-data)
              :put  (partial put url post-data)
              :get  (partial get url))]
      (f handler error-handler body))))

