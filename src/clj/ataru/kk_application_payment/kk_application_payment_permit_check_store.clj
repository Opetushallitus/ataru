(ns ataru.kk-application-payment.kk-application-payment-permit-check-store
  "Virkailijan hakemusmaksun oleskelulupaliitteisiin tekemien merkintöjen tallennus. Erillään merkintöjen
   pääsynhallinnallisesta käsittelystä, koska maksun määrittely riippuu merkinnöistä ja pääsynhallinta maksun
   määrittelystä."
  (:require [ataru.component-data.kk-application-payment-module :as payment-module]
            [ataru.db.db :as db]
            [yesql.core :as yesql]))

(declare yesql-get-kk-application-payment-permit-checks)
(declare yesql-upsert-kk-application-payment-permit-check)
(declare yesql-insert-inherited-kk-application-payment-permit-check)
(declare yesql-delete-kk-application-payment-permit-checks)
(declare yesql-delete-other-kk-application-payment-permit-checks)
(declare yesql-get-kk-application-payment-document-option)
(declare yesql-lock-kk-application-payment-permit-checks)
(yesql/defqueries "sql/kk-application-payment-permit-check-queries.sql")

(defn get-permit-checks
  ([application-key]
   (db/exec :db yesql-get-kk-application-payment-permit-checks {:application_key application-key}))
  ([connection application-key]
   (yesql-get-kk-application-payment-permit-checks {:application_key application-key} {:connection connection})))

(defn- check-of-attachment [checks attachment-key]
  (some #(when (= attachment-key (:attachment-key %)) %) checks))

(defn get-permit-check
  ([application-key attachment-key]
   (check-of-attachment (get-permit-checks application-key) attachment-key))
  ([connection application-key attachment-key]
   (check-of-attachment (get-permit-checks connection application-key) attachment-key)))

(defn upsert-permit-check!
  "Palauttaa nil, kun hakemuksella on jo toisen liitteen tarkistus, koska hakemuksella on enintään yksi tarkistus."
  [connection check]
  (first (yesql-upsert-kk-application-payment-permit-check check {:connection connection})))

(defn insert-inherited-permit-check!
  "Palauttaa nil, kun hakemuksella on jo tarkistus."
  [connection check]
  (first (yesql-insert-inherited-kk-application-payment-permit-check check {:connection connection})))

(defn delete-permit-checks!
  [connection application-key modified-until]
  (yesql-delete-kk-application-payment-permit-checks
    {:application_key application-key
     :modified_until  modified-until}
    {:connection connection}))

(defn delete-other-permit-checks!
  "Poistaa hakemuksen tarkistukset muilta liitteiltä kuin attachment-key, tai kaikki tarkistukset, kun se on nil.
   Palauttaa poistetut tarkistukset."
  [connection application-key attachment-key]
  (yesql-delete-other-kk-application-payment-permit-checks
    {:application_key application-key
     :attachment_key  attachment-key}
    {:connection connection}))

(defn lock-permit-checks!
  "Lukitsee hakemuksen tarkistukset transaktion loppuun asti. Tarkistusta tallennettaessa lukko otetaan ennen kuin
   hakijan valitsema dokumenttivaihtoehto luetaan, ja vaihtoehdon vaihtuessa ennen kuin edellisen vaihtoehdon
   tarkistus poistetaan. Muuten tarkistus voisi tallentua edellisen vaihtoehdon liitteelle samanaikaisen vaihdon
   jälkeen ja estää valitun vaihtoehdon liitteen tarkistuksen, koska hakemuksella on enintään yksi tarkistus."
  [connection application-key]
  (yesql-lock-kk-application-payment-permit-checks {:application_key application-key} {:connection connection}))

(defn selected-residence-permit-attachment-key
  "Hakijan hakemuksen uusimmassa versiossa valitseman dokumenttivaihtoehdon oleskelulupaliite tai nil, kun
   vaihtoehtoon ei kuulu oleskelulupaliitettä."
  [connection application-key]
  (->> (yesql-get-kk-application-payment-document-option
         {:application_key application-key
          :answer_key      payment-module/kk-application-payment-choice-key}
         {:connection connection})
       first
       :value
       (get payment-module/kk-application-payment-residence-permit-attachment-key-by-option)))
