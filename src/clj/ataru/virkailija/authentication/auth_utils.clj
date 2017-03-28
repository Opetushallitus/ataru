(ns ataru.virkailija.authentication.auth-utils
  (:require
    [ataru.config.url-helper :refer [resolve-url]]))

(defn cas-auth-url
  []
  (resolve-url :opintopolku.login))
