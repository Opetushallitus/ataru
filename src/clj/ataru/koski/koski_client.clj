(ns ataru.koski.koski-client
  (:require [schema.core :as s]
            [ataru.cas.client :as cas-client]
            [ataru.config.url-helper :as url-helper]
            [ataru.schema.koski-tutkinnot-schema :as koski-schema]
            [cheshire.core :as json]))
(s/defn ^:always-validate get-tutkinnot-for-oppija-oid :- (s/maybe koski-schema/KoskiSuoritusResponse)
  [oppija-oid :- s/Str
   cas-client]
  (let [url (url-helper/resolve-url :koski.hakemuspalvelu)
        {:keys [status body]} (cas-client/cas-authenticated-post cas-client url {:oid oppija-oid})]
    (case status
      200 (json/parse-string body true)
      404 nil
      (throw (new RuntimeException (str "Could not post " url ", "
                                        "status: " status ", "
                                        "body: " body))))))

