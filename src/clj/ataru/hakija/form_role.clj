(ns ataru.hakija.form-role
  (:require [schema.core :as s]))

(s/defschema FormRole (s/enum :hakija :virkailija))

(s/defn ^:always-validate virkailija? :- s/Bool
  [roles :- [FormRole]]
  (boolean (some #(= :virkailija %) roles)))
