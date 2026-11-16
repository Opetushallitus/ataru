(ns ataru.koski.koski-client
  (:require [schema.core :as s]
            [ataru.util.http-util :as http]
            [ataru.config.core :refer [config]]
            [ataru.config.url-helper :as url-helper]
            [ataru.schema.koski-tutkinnot-schema :as koski-schema]
            [cheshire.core :as json]))
(s/defn ^:always-validate get-tutkinnot-for-oppija-oid :- (s/maybe koski-schema/KoskiResponse)
  [oppija-oid :- s/Str]
  (let [url (url-helper/resolve-url :koski.hakemuspalvelu)
        username (get-in config [:koski :username])
        password (get-in config [:koski :password])
        default-username-status (if (= username "not-in-use")
                                  "Default username IS in use"
                                  "Default username NOT in use")
        {:keys [status body]} (http/do-post-json-with-basic-auth url username password {:oid oppija-oid} {})]
    (case status
      200 (json/parse-string body true)
      404 nil
      (throw (new RuntimeException (str "Could not post " url ", "
                                        default-username-status
                                        "status: " status ", "
                                        "body: " body))))))

