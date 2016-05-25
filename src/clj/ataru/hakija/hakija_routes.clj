(ns ataru.hakija.hakija-routes
  (:require [ataru.forms.form-store :as form-store]
            [compojure.core :refer [routes defroutes wrap-routes context GET]]
            [schema.core :as s]
            [compojure.api.sweet :as api]
            [ring.util.http-response :refer [ok not-found]]
            [compojure.route :as route]
            [selmer.parser :as selmer]))

(def ^:private cache-fingerprint (System/currentTimeMillis))

(def placeholder-content
  {:content
   [{:fieldClass "formField"
     :label      {:fi "Ikä", :sv "ålder"}
     :id         "applicant-age"
     :required   true
     :fieldType  "textField"}
    {:fieldClass "wrapperElement"
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
                   :fieldType  "textField"}]}
    {:fieldClass "wrapperElement"
     :id         "applicant-fieldset"
     :children
     [{:fieldClass "wrapperElement"
       :id         "applicant-fieldset"
       :children
                   [{:fieldClass "formField"
                     :label      {:fi "X", :sv "Z"}
                     :id         "applicant-x"
                     :required   true
                     :fieldType  "textField"}
                    {:fieldClass "formField"
                     :label      {:fi "A", :sv "B"}
                     :id         "applicant-a"
                     :required   true
                     :fieldType  "textField"}]
       }]}]})

(defn- fetch-form [id]
  (let [form (form-store/fetch-form id)]
    (if form
      (ok form)
      (not-found form))))

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
                 (api/GET "/form/:id" []
                          :path-params [id :- Long]
                          :return s/Any
                          (ok (fetch-form id))))))

(def hakija-routes
  (-> (routes
        (context "/hakemus" []
          api-routes
          (route/resources "/")
          (GET "/:id" []
            (selmer/render-file "templates/hakija.html" {:cache-fingerprint cache-fingerprint})))
        (route/not-found "<h1>Page not found</h1>"))))
