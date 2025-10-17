(ns ataru.user-rights
  (:require [schema.core :as s]
            [clojure.string :as str]))

(def ^:private
  oikeus-to-right
  {{:palvelu "ATARU_EDITORI" :oikeus "CRUD"}           :form-edit
   {:palvelu "ATARU_HAKEMUS" :oikeus "READ"}           :view-applications
   {:palvelu "ATARU_HAKEMUS" :oikeus "CRUD"}           :edit-applications
   {:palvelu "ATARU_HAKEMUS" :oikeus "VALINTA_READ"}   :view-valinta
   {:palvelu "ATARU_HAKEMUS" :oikeus "VALINTA_CRUD"}   :edit-valinta
   {:palvelu "ATARU_HAKEMUS" :oikeus "opinto-ohjaaja"} :opinto-ohjaaja
   {:palvelu "ATARU_HAKEMUS" :oikeus "valinnat-valilehti"} :valinnat-valilehti})

(def right-names (vals oikeus-to-right))

(s/defschema Right (apply s/enum right-names))

(defn with-oid [roles]
  (filter (fn [role]
            (let [last-part (last (str/split role #"_"))]
              (str/starts-with? last-part "1.2.")))
          roles))

(defn only-ataru [roles]
  (filter #(str/starts-with? % "ROLE_APP_ATARU") roles))

(defn strip-role-app [roles]
  (map #(str/replace % #"^ROLE_APP_" "") roles))

(defn convert-to-organisaatiot [roles]
  (let [entries (map (fn [role]
                       (let [parts (str/split role #"_")
                             oid   (last parts)
                             service-parts (butlast parts)
                             palvelu       (str/join "_" (take 2 service-parts)) ;; e.g. ATARU_HAKEMUS
                             oikeus-parts  (drop 2 service-parts)
                             oikeus        (str/join "_" oikeus-parts)]
                         {:organisaatioOid oid
                          :kayttooikeudet [{:palvelu palvelu
                                            :oikeus  oikeus}]}))
                     roles)]
    (->> entries
         (group-by :organisaatioOid)
         (map (fn [[oid items]]
                {:organisaatioOid oid
                 :kayttooikeudet (->> items
                                      (mapcat :kayttooikeudet)
                                      distinct
                                      vec)}))
         (into []))))

(defn is-super-user?
  [session]
  (boolean (-> session :identity :superuser)))

(defn virkailija->right-organization-oids
  [roles rights]
  {:pre [(< 0 (count rights))]}
  (select-keys (->> roles
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

(defn all-organizations-have-only-opinto-ohjaaja-rights?
  [session]
  (let [get-organization-oids-for-right (fn [right]
                                          (->> session
                                               :identity
                                               :user-right-organizations
                                               right
                                               (map :oid)
                                               set))
        opinto-ohjaaja-organizations (get-organization-oids-for-right :opinto-ohjaaja)
        view-organizations (get-organization-oids-for-right :view-applications)
        edit-organizations (get-organization-oids-for-right :edit-applications)
        all-organizations (->> session
                               :identity
                               :user-right-organizations
                               (vals)
                               (flatten)
                               (map :oid)
                               set)]
  (and (boolean (seq opinto-ohjaaja-organizations))
       (= opinto-ohjaaja-organizations all-organizations)
       (empty? view-organizations)
       (empty? edit-organizations))))
