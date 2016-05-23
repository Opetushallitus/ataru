(ns ataru.hakija.hakija-routes
  (:require [compojure.core :refer [routes defroutes wrap-routes context GET]]
            [compojure.api.sweet :as api]
            [ring.util.http-response :refer [ok]]
            [compojure.route :as route]
            [selmer.parser :as selmer]))

(def ^:private cache-fingerprint (System/currentTimeMillis))

(def placeholder-content
  {:content
   [{:fieldClass "wrapperElement"
     :id         "applicant-fieldset"
     :children
                 [{:fieldClass "formField"
                   :helpText
                               {:fi "Yhteyshenkilöllä tarkoitetaan hankkeen vastuuhenkilöä."
                                :sv "Med kontaktperson avses den projektansvariga i sökandeorganisationen."}
                   :label      {:fi "Sukunimi", :sv "Efternamn"}
                   :id         "applicant-firstname"
                   :required   true
                   :fieldType  "textField"}
                  {:fieldClass "formField"
                   :label      {:fi "Etunimi", :sv "Förnamn"}
                   :id         "applicant-surname"
                   :required   true
                   :fieldType  "textField"}]}]})

(def api-routes
  (api/api
    {:swagger {:spec "/hakemus/swagger.json"
               :ui "/hakemus/api-docs"
               :data {:info {:version "1.0.0"
                             :title "Ataru Hakija API"
                             :description "Specifies the Hakija API for Ataru"}}
               :tags [{:name "application-api" :description "Application handling"}]}}
    (api/context "/api" []
                 :tags ["application-api"]
                 (api/GET "/form/:id" [id]
                          (ok placeholder-content)))))

(def hakija-routes
  (-> (routes
        (context "/hakemus" []
          api-routes
          (route/resources "/")
          (GET "/:id" []
            (selmer/render-file "templates/hakija.html" {:cache-fingerprint cache-fingerprint})))
        (route/not-found "<h1>Page not found</h1>"))))
