(ns ataru.ajax.http
  (:require [re-frame.core :refer [register-handler dispatch]]
            [cljs.core.match :refer-macros [match]]
            [ataru.virkailija.temporal :as temporal]
            [ajax.core :refer [GET POST PUT DELETE]]))

(defn http [method path handler-or-dispatch & {:keys [override-args handler-args]}]
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
               :handler         (comp (fn [response]
                                        (dispatch [:flasher {:loading? false
                                                             :message
                                                                       (match [method]
                                                                              [:post] "Kaikki muutokset tallennettu"
                                                                              [:delete] "Tiedot poistettu"
                                                                              :else nil)}])
                                        (match [handler-or-dispatch]
                                               [(dispatch-keyword :guard keyword?)] (dispatch [dispatch-keyword response handler-args])
                                               [nil] nil
                                               :else (dispatch [:state-update (fn [db] (handler-or-dispatch db response handler-args))])))
                                      temporal/parse-times)}
              override-args))))

(defn post [path params & [handler-or-dispatch]]
  (http :post path handler-or-dispatch :override-args {:params params}))

