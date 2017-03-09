(ns ataru.haku.haku-access-control
  (:require
    [ataru.virkailija.user.session-organizations :as session-orgs]
    [ataru.applications.application-store :as application-store]
    [taoensso.timbre :refer [warn]]
    [clojure.string :as string]))

(defn- add-haku-names
  [tarjonta-service haku-results]
  (remove nil? (map (fn [haku-result]
                      (let [haku (.get-haku tarjonta-service (:haku haku-result))
                            haku-name (-> haku :nimi :kieli_fi)]
                        (if (string/blank? haku-name)
                          (warn "No haku title found with OID" (:haku haku-result))
                          (merge haku-result {:haku-name haku-name}))))
                    haku-results)))

(defn get-haut [session organization-service tarjonta-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   vector
   #(add-haku-names tarjonta-service (application-store/get-haut %))
   #(add-haku-names tarjonta-service (application-store/get-all-haut))))
