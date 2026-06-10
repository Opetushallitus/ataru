(ns ataru.siirtotiedosto.toinenaste-enrichment-spec
  (:require [speclj.core :refer [it describe tags should= should-not-be-nil should-be-nil]]
            [ataru.siirtotiedosto.toinenaste-enrichment :as enrichment]
            [ataru.applications.question-util :as question-util]
            [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-types :refer [harkinnanvaraisuus-reasons]]))

(def ^:private hakukohde-oid-1 "1.2.246.562.20.00000000001")
(def ^:private hakukohde-oid-2 "1.2.246.562.20.00000000002")

(def ^:private form-with-toinenaste-questions
  {:content
   [{:id "pohjakoulutus-2nd-wrapper"
     :children [{:id "base-education-2nd"
                 :options [{:followups [{:id "suoritusvuosi-perusopetus"}
                                        {:id "tutkintokieli-perusopetus"}]}
                            {:followups [{:id "suoritusvuosi-yks"}
                                         {:id "tutkintokieli-yks"}]}]}]}
    {:id "sorawrapper"
     :children [{:id "sora-terveys"}
                {:id "sora-aiempi"}]}
    {:id "huoltajawrapper"
     :children [{:id "guardian-firstname"}
                {:id "guardian-lastname"}
                {:id "guardian-phone"}
                {:id "guardian-email"}]}]})

(def ^:private base-questions
  (question-util/get-hakurekisteri-toinenaste-specific-questions form-with-toinenaste-questions))

(defn- make-application
  "Builds a minimal siirtotiedosto application map."
  [overrides]
  (merge {:lang       "fi"
          :hakukohde  [hakukohde-oid-1 hakukohde-oid-2]
          :person_oid "1.2.246.562.24.00000000001"
          :form       42
          :hakemusOid "1.2.246.562.11.00000000001"
          :content    {:answers []}}
         overrides))

(defn- make-answers [& kvs]
  (map (fn [[k v]] {:key (name k) :value v :fieldType "textField"})
       (partition 2 kvs)))

(describe "toinenaste-enrichment"
  (tags :unit)

  (describe "enrich-with-toinenaste"

    (it "returns nil when questions are nil"
        (should-be-nil (enrichment/enrich-with-toinenaste (make-application {}) nil)))

    (it "returns a map when questions are provided"
        (let [result (enrichment/enrich-with-toinenaste (make-application {}) base-questions)]
          (should-not-be-nil result)))

    (it "includes personOid from application"
        (let [result (enrichment/enrich-with-toinenaste
                       (make-application {:person_oid "1.2.246.562.24.99"})
                       base-questions)]
          (should= "1.2.246.562.24.99" (:personOid result))))

    (it "includes kieli from application"
        (let [result (enrichment/enrich-with-toinenaste
                       (make-application {:lang "sv"})
                       base-questions)]
          (should= "sv" (:kieli result))))

    (it "includes one hakukohde entry per hakukohde OID"
        (let [result (enrichment/enrich-with-toinenaste
                       (make-application {:hakukohde [hakukohde-oid-1 hakukohde-oid-2]})
                       base-questions)]
          (should= 2 (count (:hakukohteet result)))
          (should= hakukohde-oid-1 (-> result :hakukohteet first :oid))
          (should= hakukohde-oid-2 (-> result :hakukohteet second :oid))))

    (it "returns an empty hakukohteet list when application has no hakukohteet"
        (let [result (enrichment/enrich-with-toinenaste
                       (make-application {:hakukohde []})
                       base-questions)]
          (should= [] (:hakukohteet result))))

    (it "extracts contact fields from answers"
        (let [answers (make-answers :email "testi@esimerkki.fi"
                                    :phone "0401234567"
                                    :address "Testikatu 1"
                                    :postal-code "00100"
                                    :postal-office "Helsinki"
                                    :country-of-residence "246"
                                    :home-town "Helsinki")
              result (enrichment/enrich-with-toinenaste
                       (make-application {:content {:answers answers}})
                       base-questions)]
          (should= "testi@esimerkki.fi" (:email result))
          (should= "0401234567" (:matkapuhelin result))
          (should= "Testikatu 1" (:lahiosoite result))
          (should= "00100" (:postinumero result))
          (should= "Helsinki" (:postitoimipaikka result))
          (should= "246" (:asuinmaa result))
          (should= "Helsinki" (:kotikunta result))))

    (it "uses city instead of postal-office for foreign applicants"
        (let [answers (make-answers :postal-office "Helsinki"
                                    :city "Stockholm"
                                    :country-of-residence "752")
              result (enrichment/enrich-with-toinenaste
                       (make-application {:content {:answers answers}})
                       base-questions)]
          (should= "Stockholm" (:postitoimipaikka result))))

    (it "extracts pohjakoulutus from answers"
        (let [answers (make-answers :base-education-2nd "1")
              result (enrichment/enrich-with-toinenaste
                       (make-application {:content {:answers answers}})
                       base-questions)]
          (should= "1" (:pohjakoulutus result))))

    (it "defaults pohjakoulutus to empty string when not answered"
        (let [result (enrichment/enrich-with-toinenaste (make-application {}) base-questions)]
          (should= "" (:pohjakoulutus result))))

    (it "extracts first huoltaja from answers"
        (let [answers [{:key "guardian-firstname" :value ["Matti"] :fieldType "textField"}
                       {:key "guardian-lastname"  :value ["Meikalainen"] :fieldType "textField"}
                       {:key "guardian-phone"     :value ["0401111111"] :fieldType "textField"}
                       {:key "guardian-email"     :value ["huoltaja@esimerkki.fi"] :fieldType "textField"}]
              result (enrichment/enrich-with-toinenaste
                       (make-application {:content {:answers answers}})
                       base-questions)]
          (should= 1 (count (:huoltajat result)))
          (let [huoltaja (first (:huoltajat result))]
            (should= "Matti" (:etunimi huoltaja))
            (should= "Meikalainen" (:sukunimi huoltaja))
            (should= "0401111111" (:matkapuhelin huoltaja))
            (should= "huoltaja@esimerkki.fi" (:email huoltaja)))))

    (it "returns empty huoltajat when no guardian answers present"
        (let [result (enrichment/enrich-with-toinenaste (make-application {}) base-questions)]
          (should= 0 (count (:huoltajat result)))))

    (it "detects terveys SORA per hakukohde"
        (let [terveys-key (str "sora-terveys_" hakukohde-oid-1)
              answers (make-answers (keyword terveys-key) "1")
              result (enrichment/enrich-with-toinenaste
                       (make-application {:content {:answers answers}})
                       base-questions)
              hk1 (first (filter #(= hakukohde-oid-1 (:oid %)) (:hakukohteet result)))
              hk2 (first (filter #(= hakukohde-oid-2 (:oid %)) (:hakukohteet result)))]
          (should= true (:terveys hk1))
          (should= false (:terveys hk2))))

    (it "detects aiempiPeruminen SORA per hakukohde"
        (let [aiempi-key (str "sora-aiempi_" hakukohde-oid-2)
              answers (make-answers (keyword aiempi-key) "1")
              result (enrichment/enrich-with-toinenaste
                       (make-application {:content {:answers answers}})
                       base-questions)
              hk2 (first (filter #(= hakukohde-oid-2 (:oid %)) (:hakukohteet result)))]
          (should= true (:aiempiPeruminen hk2))))

    (it "computes harkinnanvaraisuus from common reason in answers"
        (let [answers (make-answers :base-education-2nd "0")
              result (enrichment/enrich-with-toinenaste
                       (make-application {:content {:answers answers}})
                       base-questions)
              hk1 (first (:hakukohteet result))]
          (should= (:ataru-ulkomailla-opiskelu harkinnanvaraisuus-reasons)
                   (:harkinnanvaraisuus hk1))))

    (it "returns nil for tutkintoVuosi when not answered"
        (let [result (enrichment/enrich-with-toinenaste (make-application {}) base-questions)]
          (should-be-nil (:tutkintoVuosi result))))

    (it "parses tutkintoVuosi as integer"
        (let [answers (make-answers :suoritusvuosi-perusopetus "2019")
              result (enrichment/enrich-with-toinenaste
                       (make-application {:content {:answers answers}})
                       base-questions)]
          (should= 2019 (:tutkintoVuosi result))))

    (it "returns a result with nil fields when application has no content"
        (let [empty-application (make-application {:content nil})]
          (should-not-be-nil (enrichment/enrich-with-toinenaste empty-application base-questions))
          (should-be-nil (:email (enrichment/enrich-with-toinenaste empty-application base-questions)))))))
