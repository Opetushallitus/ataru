(ns ataru.user-rights
  (:require [schema.core :as s]))

(def ^:private
  oikeus-to-right
  {{:palvelu "ATARU_EDITORI" :oikeus "CRUD"}           :form-edit
   {:palvelu "ATARU_HAKEMUS" :oikeus "READ"}           :view-applications
   {:palvelu "ATARU_HAKEMUS" :oikeus "CRUD"}           :edit-applications
   {:palvelu "ATARU_HAKEMUS" :oikeus "VALINTA_READ"}   :view-valinta
   {:palvelu "ATARU_HAKEMUS" :oikeus "VALINTA_CRUD"}   :edit-valinta
   {:palvelu "ATARU_HAKEMUS" :oikeus "opinto-ohjaaja"} :opinto-ohjaaja})

(def right-names (vals oikeus-to-right))

(s/defschema Right (apply s/enum right-names))

(defn virkailija->right-organization-oids
  [virkailija rights]
  {:pre [(< 0 (count rights))]}
  (select-keys (->> (:organisaatiot virkailija)
                    (mapcat (fn [{:keys [organisaatioOid kayttooikeudet]}]
                              (map (fn [right] {right [organisaatioOid]})
                                   (keep oikeus-to-right kayttooikeudet))))
                    (reduce (partial merge-with concat) {}))
               rights))

(defn has-opinto-ohjaaja-right-for-any-organization?
  [session]
  (-> session
    :identity
    :user-right-organizations
    :opinto-ohjaaja
    seq
    boolean))

(defn all-organizations-have-opinto-ohjaaja-rights?
  [session]
  (let [opinto-ohjaaja-organizations (->> session
                                          :identity
                                          :user-right-organizations
                                          :opinto-ohjaaja
                                         (map :oid)
                                          set)
        all-organizations (->> session
                               :identity
                               :user-right-organizations
                               (vals)
                               (flatten)
                               (map :oid)
                               set)]
  (and (boolean (seq opinto-ohjaaja-organizations))
       (= opinto-ohjaaja-organizations all-organizations))))