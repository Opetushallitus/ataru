(ns ataru.hakija.form-role
  (:require [schema.core :as s]))

(s/defschema FormRole (s/enum :hakija :virkailija :with-henkilo))

(s/defn ^:always-validate virkailija? :- s/Bool
  [roles :- [FormRole]]
  (boolean (some #(= :virkailija %) roles)))

(s/defn ^:always-validate with-henkilo? :- s/Bool
  [roles :- [FormRole]]
  (boolean (some #(= :with-henkilo %) roles)))
