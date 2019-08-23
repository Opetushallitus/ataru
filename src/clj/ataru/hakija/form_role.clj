(ns ataru.hakija.form-role
  (:require [schema.core :as s]))

(s/defschema FormRole (s/enum :hakija
                              :virkailija
                              :with-henkilo
                              :sensitive-questions-viewable
                              :sensitive-questions-editable))

(s/defn ^:always-validate virkailija? :- s/Bool
  [roles :- [FormRole]]
  (boolean (some #(= :virkailija %) roles)))

(s/defn ^:always-validate with-henkilo? :- s/Bool
  [roles :- [FormRole]]
  (boolean (some #(= :with-henkilo %) roles)))

(s/defn ^:always-validate sensitive-questions-viewable? :- s/Bool
  [roles :- [FormRole]]
  (boolean (some #(= :sensitive-questions-viewable %) roles)))

(s/defn ^:always-validate sensitive-questions-editable? :- s/Bool
  [roles :- [FormRole]]
  (boolean (some #(= :sensitive-questions-editable %) roles)))
