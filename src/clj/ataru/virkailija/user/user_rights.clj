(ns ataru.virkailija.user.user-rights
  (:require [schema.core :as s]
            [oph.soresu.common.config :refer [config]]))

(def ^:private
  rights
  {:form-edit         "APP_ATARU_EDITORI_CRUD"
   :view-applications "APP_ATARU_HAKEMUS_READ"
   :edit-applications "APP_ATARU_HAKEMUS_CRUD"})

(def right-names (keys rights))

(s/defschema Right (apply s/enum right-names))

(s/defn ^:always-validate ldap-right [right :- Right]
  (let [name-from-config (-> config :ldap :user-right-names right)]
    (if (not (clojure.string/blank? name-from-config))
      name-from-config
      (right rights))))
