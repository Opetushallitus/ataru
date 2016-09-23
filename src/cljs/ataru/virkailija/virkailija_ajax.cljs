(ns ataru.virkailija.virkailija-ajax
  (:require [re-frame.core :refer [register-handler dispatch]]
            [cljs.core.match :refer-macros [match]]
            [ataru.virkailija.temporal :as temporal]
            [ajax.core :refer [GET POST PUT DELETE]]
            [taoensso.timbre :refer-macros [spy debug]]))

(def ^:private
  error-messages
  {"form_updated_in_background"      "Lomakkeen sisältö on muuttunut. Lataa sivu uudelleen."
   "no_organization_for_user"        "Käyttäjätunnukseen ei ole liitetty organisaatota"
   "multiple_organizations_for_user" "Käyttäjätunnukselle löytyi monta organisaatiota"})

(defn dispatch-flasher-error-msg
  [method response]
  (let [response-error-code (-> response :response :error)
        explicit-error-message (get error-messages response-error-code)
        error-type (if (and (= 400 (:status response))
                            (not-empty response-error-code)
                            (not-empty explicit-error-message))
                     :explicit-error
                     :server-error)
        message (case error-type
                  :explicit-error explicit-error-message
                  :server-error (str "Virhe "
                                     (case method
                                       :get "haettaessa."
                                       :post "tallennettaessa."
                                       :put "tallennettaessa."
                                       :delete "poistettaessa.")))]
    (dispatch [:flasher {:loading? false
                         :message  message
                         :error-type error-type
                         :detail   response}])
    response))

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
               :error-handler   (comp
                                  (or (:error-handler override-args) identity)
                                  (partial dispatch-flasher-error-msg method))
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

(defn post [path params handler-or-dispatch & {:keys [override-args handler-args]}]
  (http :post path handler-or-dispatch
        :override-args (merge override-args {:params params})
        :handler-args handler-args))
