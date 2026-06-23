(ns ataru.siirtotiedosto.toinenaste-enrichment
  (:require [ataru.util :refer [answers-by-key]]
            [ataru.applications.toinenaste-util :as toinenaste-util]))

(defn- strip-tarjonta-dependent-hakukohde-fields
  "Drops per-hakukohde fields that cannot be computed correctly without tarjonta-service.
   - :harkinnanvaraisuus depends on a hakukohde flag (:voiko-...harkinnanvaraisesti-hakeneita?)
     and on hakukohde-specific reason metadata.
   - :kiinnostunutUrheilijanAmmatillisestaKoulutuksesta depends on hakukohde group membership
     (:ryhmaliitokset) to know which hakukohdes actually offer urheilija-amm training.
   The applicant's urheilija-amm interest itself is surfaced at the top level instead."
  [hakukohteet]
  (mapv #(dissoc % :harkinnanvaraisuus :kiinnostunutUrheilijanAmmatillisestaKoulutuksesta)
        hakukohteet))

(defn- top-level-urheilija-amm-interest
  "Per-applicant interest in urheilija-amm training, derived from the applicant's answer to
   the urheilija-amm wrapper question. Nil if the form doesn't ask the question."
  [answers questions]
  (when-let [sports-key (:urheilijan-amm-lisakysymys-key questions)]
    (when-let [answer (-> answers sports-key :value)]
      (= "0" answer))))

(defn enrich-with-toinenaste
  "Computes toinenaste-specific data from a siirtotiedosto application and form-derived questions.
   Returns nil if questions are nil.

   Per-hakukohde entries (`:hakukohteet`) include only fields derived purely from form + answers
   (`:oid`, `:terveys`, `:aiempiPeruminen`, `:kiinnostunutKaksoistutkinnosta`). Fields that depend
   on tarjonta data (`:harkinnanvaraisuus`, `:kiinnostunutUrheilijanAmmatillisestaKoulutuksesta`)
   are omitted — consumers needing them should use the suoritusrekisteri API.

   `:kiinnostunutUrheilijanAmmatillisestaKoulutuksesta` is reported at the top level as a
   per-applicant fact (independent of which hakukohde offers urheilija-amm)."
  [application questions]
  (when questions
    (let [answers (answers-by-key (-> application :content :answers))
          shared (toinenaste-util/build-toinenaste-payload
                   {:answers                  answers
                    :hakukohde-oids           (:hakukohde application)
                    :lang                     (:lang application)
                    :person-oid               (:person_oid application)
                    :questions                questions
                    ;; Strategy outputs are dissoc'd below; values don't matter.
                    :get-hakukohde-fn         (constantly nil)
                    :urheilija-amm-hakukohde? (constantly false)})]
      (-> shared
          (update :hakukohteet strip-tarjonta-dependent-hakukohde-fields)
          (assoc :email (-> answers :email :value))
          (assoc :kiinnostunutUrheilijanAmmatillisestaKoulutuksesta ;;Huom. Tämä kenttä ei sisällä tarjonta-tietoa, eli Ovaran päässä yhdistettävä tarjonta-tietoon.
                 (top-level-urheilija-amm-interest answers questions))))))
