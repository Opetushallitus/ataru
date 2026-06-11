(ns ataru.siirtotiedosto.toinenaste-enrichment
  (:require [ataru.util :refer [answers-by-key]]
            [ataru.applications.toinenaste-util :as toinenaste-util]))

(defn- permissive-hakukohde
  "Synthetic hakukohde used when tarjonta metadata is unavailable. Lets the
   answer-based harkinnanvaraisuus reasoning proceed as if the hakukohde
   accepts harkinnanvaraisesti hakeneita."
  [oid]
  {:oid oid :voiko-hakukohteessa-olla-harkinnanvaraisesti-hakeneita? true})

(defn enrich-with-toinenaste
  "Computes toinenaste-specific data from a siirtotiedosto application and form-derived questions.
   Returns nil if questions are nil. Without tarjonta-service data, urheilija-amm interest is
   reported for every hakukohde and harkinnanvaraisuus uses a permissive default hakukohde."
  [application questions]
  (when questions
    (let [answers (answers-by-key (-> application :content :answers))
          shared (toinenaste-util/build-toinenaste-payload
                   {:answers                  answers
                    :hakukohde-oids           (:hakukohde application)
                    :lang                     (:lang application)
                    :person-oid               (:person_oid application)
                    :questions                questions
                    :get-hakukohde-fn         permissive-hakukohde
                    :urheilija-amm-hakukohde? (constantly true)})]
      (assoc shared :email (-> answers :email :value)))))
