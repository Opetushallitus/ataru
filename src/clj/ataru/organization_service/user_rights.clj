(ns ataru.organization-service.user-rights
  (:require [ataru.config.core :refer [config]]
            [schema.core :as s]))

(def ^:private
  oikeus-to-right
  {{:palvelu "ATARU_EDITORI" :oikeus "CRUD"}         :form-edit
   {:palvelu "ATARU_HAKEMUS" :oikeus "READ"}         :view-applications
   {:palvelu "ATARU_HAKEMUS" :oikeus "CRUD"}         :edit-applications
   {:palvelu "ATARU_HAKEMUS" :oikeus "VALINTA_READ"} :view-valinta
   {:palvelu "ATARU_HAKEMUS" :oikeus "VALINTA_CRUD"} :edit-valinta})

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
