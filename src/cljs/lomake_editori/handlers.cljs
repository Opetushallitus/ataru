(ns lomake-editori.handlers
    (:require [re-frame.core :as re-frame :refer [register-handler dispatch]]
              [ajax.core :refer [GET POST PUT DELETE]]
              [lomake-editori.db :as db]
              [lomake-editori.temporal :refer [coerce-timestamp]]
              [cljs-time.core :as c]
              [cljs-time.format :as f]
              [cljs.core.match :refer-macros [match]]
              [taoensso.timbre :refer-macros [spy debug]]))

(def formatter (f/formatter "EEEE dd.MM.yyyy HH:mm"))

(defn http [method path handler-or-dispatch & [override-args]]
  (let [f (case method
            :get    GET
            :post   POST
            :put    PUT
            :delete DELETE)]
    (dispatch [:flasher {:loading? true
                         :message  (match [method]
                                          [:post] "Tietoja tallennetaan"
                                          [:delete] "Tietoja poistetaan"
                                          :else nil)}])
    (f path
       (merge {:response-format :json
               :format          :json
               :keywords?       true
               :error-handler   #(dispatch [:flasher {:loading? false
                                                      :message (str "Virhe "
                                                                    (case method
                                                                      :get "haettaessa."
                                                                      :post "tallennettaessa."
                                                                      :put "tallennettaessa."
                                                                      :delete "poistettaessa."))
                                                      :detail %}])
               :handler         (fn [response]
                                  (dispatch [:flasher {:loading? false
                                                       :message
                                                       (match [method]
                                                              [:post] "Kaikki muutokset tallennettu"
                                                              [:delete] "Tiedot poistettu"
                                                              :else nil)}])
                                  (match [handler-or-dispatch]
                                         [(dispatch-keyword :guard keyword?)] (dispatch [dispatch-keyword response])
                                         :else (dispatch [:state-update (fn [db] (handler-or-dispatch db response))])))}
              override-args))))

(defn post [path params handler-or-dispatch]
  (http :post path handler-or-dispatch {:params params}))

(register-handler
 :initialize-db
 (fn  [_ _]
   db/default-db))

(register-handler
  :set-state
  (fn [db [_ path args]]
    (assert (or (vector? path)
                (seq? path)))
    (if (map? args)
      (update-in db path merge args)
      (assoc-in db path args))))

(register-handler
  :state-update
  (fn [db [_ f]]
    (or (f db)
        db)))

(register-handler
  :handle-error
  (fn [db [_ error]]
    ;; TODO make a generic error message panel in UI which is shown when
    ;; :error-message exists and has a control for emptying it
    (assoc db :error-message "Lomakelistan hakeminen epäonnistui")))

(register-handler
  :fetch-initial-data
  (fn [db _]
    (dispatch [:editor/refresh-forms])
    db))

(register-handler
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

(register-handler
  :flasher
  (fn [db [_ flash]]
    (assoc db :flasher flash)))
