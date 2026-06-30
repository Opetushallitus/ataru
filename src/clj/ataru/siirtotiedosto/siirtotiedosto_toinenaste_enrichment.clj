(ns ataru.siirtotiedosto.siirtotiedosto-toinenaste-enrichment
  (:require [ataru.util :refer [answers-by-key]]
            [ataru.applications.toinenaste-util :as toinenaste-util]))

(defn- strip-tarjonta-hakukohderyhma-dependent-hakukohde-fields
  "Poistaa hakukohdekohtaiset kentät, joita ei voi laskea oikein ilman tarjonta-palvelua.
   - :harkinnanvaraisuus riippuu tarjonta-datasta (voiko hakukohteessa ylipäätään olla harkinnanvaraisesti hakeneita)
     sekä hakukohdekohtaisesta syymetadatasta.
   - :kiinnostunutUrheilijanAmmatillisestaKoulutuksesta riippuu hakukohteen hakukohderyhmäjäsenyyksistä
     (:ryhmaliitokset), joista nähdään mitkä hakukohteet oikeasti tarjoavat urheilijan
     ammatillista koulutusta.
   Hakijan oma urheilija-amm-kiinnostus välitetään sen sijaan ylemmällä tasolla."
  [hakukohteet]
  (mapv #(dissoc % :harkinnanvaraisuus :kiinnostunutUrheilijanAmmatillisestaKoulutuksesta)
        hakukohteet))

(defn- onko-hakija-kiinnostunut-ammatillisesta-koulutuksesta
  "Hakemuksella ilmaistu kiinnostus urheilijan ammatilliseen koulutukseen, päätelty hakijan
   vastauksesta urheilija-amm-kysymyksen wrapperiin. Nil jos lomakkeella ei ole kysymystä."
  [answers questions]
  (when-let [sports-key (:urheilijan-amm-lisakysymys-key questions)]
    (when-let [answer (-> answers sports-key :value)]
      (= "0" answer))))

(defn enrich-with-toinenaste
  "Muodostaa toisen asteen siirtotiedostokohtaiset tiedot hakemukselta ja lomakkeesta johdetuista
   kysymyksistä. Palauttaa nil jos questions on nil (ei pitäisi tapahtua toisen asteen haulle).

   Hakukohdekohtaiset rivit (`:hakukohteet`) sisältävät vain kentät, jotka voidaan johtaa pelkästään
   lomakkeesta ja vastauksista (`:oid`, `:terveys`, `:aiempiPeruminen`,
   `:kiinnostunutKaksoistutkinnosta`). Tarjonta- tai hakukohderyhmätietoa vaativat kentät (`:harkinnanvaraisuus`,
   `:kiinnostunutUrheilijanAmmatillisestaKoulutuksesta`) jätetään pois siirtotiedostoista — tarvittaessa tehdään
   vastaavat päättelyt Ovaran päässä.

   `:kiinnostunutUrheilijanAmmatillisestaKoulutuksesta` raportoidaan ylimmällä tasolla
   hakemuskohtaisena tietona (ei sisällä tietoa siitä mikä hakukohde tarjoaa tai ei tarjoa urheilijan amm. koulutusta)."
  [application questions]
  (when questions
    (let [answers (answers-by-key (-> application :content :answers))
          shared (toinenaste-util/build-toinenaste-payload
                   {:answers                  answers
                    :hakukohde-oids           (:hakukohde application)
                    :lang                     (:lang application)
                    :person-oid               (:person_oid application)
                    :questions                questions
                    ;; Alla olevien funktioiden muokkaamat tulokset poistetaan siirtotiedostoista (strip-tarjonta-dependent-hakukohde-fields); arvoilla ei ole väliä.
                    :get-hakukohde-fn         (constantly nil)
                    :urheilija-amm-hakukohde? (constantly false)})]
      (-> shared
          (update :hakukohteet strip-tarjonta-hakukohderyhma-dependent-hakukohde-fields)
          (assoc :email (-> answers :email :value))
          (assoc :kiinnostunutUrheilijanAmmatillisestaKoulutuksesta ;;Huom. Tämä kenttä ei sisällä tarjonta-tietoa, eli Ovaran päässä yhdistettävä tarjonta-tietoon.
                 (onko-hakija-kiinnostunut-ammatillisesta-koulutuksesta answers questions))))))
