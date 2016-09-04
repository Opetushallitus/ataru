(ns ataru.virkailija.user.fake-organization-service
  (:require
   [ataru.virkailija.user.organization-service :refer [OrganizationService
                                                       get-direct-organization-oids
                                                       get-all-organizations]]))

(defrecord FakeOrganizationService []
  OrganizationService

  (get-direct-organization-oids [this user-name] ["1.2.246.562.10.0439845"])

  (get-all-organizations [this user-name]
    [{:name {:fi "Telajärven seudun koulutuskuntayhtymä telia"}, :oid "1.2.246.562.10.0439845"}]))
