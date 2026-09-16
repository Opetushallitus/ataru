(ns ataru.siirtotiedosto.toinenaste-enrichment-spec
  (:require [speclj.core :refer [it describe tags should-not-contain
                                  should= should-not-be-nil should-be-nil]]
            [ataru.siirtotiedosto.siirtotiedosto-toinenaste-enrichment :as enrichment]
            [ataru.applications.question-util :as question-util]))

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

;; Form that asks the urheilija-amm wrapper question (so the top-level interest field can be populated).
(def ^:private form-with-urheilija-amm
  {:content
   [{:id "d26bac09-1fb2-4be3-8bd1-5071a81decb7"
     :children [{:id "urheilija-amm-key"
                 :belongs-to-hakukohderyhma ["g1"]
                 :options [{:followups [{:id "urheilija-2nd-amm-peruskoulu"}]}
                            {}]}]}]})

(def ^:private urheilija-amm-questions
  (question-util/get-hakurekisteri-toinenaste-specific-questions form-with-urheilija-amm))

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

    (describe ":hakukohteet (tarjonta-independent per-hakukohde fields)"

      (it "includes one entry per applied hakukohde OID"
          (let [result (enrichment/enrich-with-toinenaste
                         (make-application {:hakukohde [hakukohde-oid-1 hakukohde-oid-2]})
                         base-questions)]
            (should= 2 (count (:hakukohteet result)))
            (should= hakukohde-oid-1 (-> result :hakukohteet first :oid))
            (should= hakukohde-oid-2 (-> result :hakukohteet second :oid))))

      (it "returns an empty :hakukohteet vector when the applicant applied to none"
          (let [result (enrichment/enrich-with-toinenaste
                         (make-application {:hakukohde []})
                         base-questions)]
            (should= [] (:hakukohteet result))))

      (it "omits :harkinnanvaraisuus per hakukohde (tarjonta data not available)"
          (let [result (enrichment/enrich-with-toinenaste (make-application {}) base-questions)]
            (doseq [hk (:hakukohteet result)]
              (should-not-contain :harkinnanvaraisuus hk))))

      (it "omits per-hakukohde :kiinnostunutUrheilijanAmmatillisestaKoulutuksesta (tarjonta groups not available)"
          (let [result (enrichment/enrich-with-toinenaste (make-application {}) base-questions)]
            (doseq [hk (:hakukohteet result)]
              (should-not-contain :kiinnostunutUrheilijanAmmatillisestaKoulutuksesta hk))))

      (it "detects terveys SORA per hakukohde from applicant answers"
          (let [terveys-key (str "sora-terveys_" hakukohde-oid-1)
                answers (make-answers (keyword terveys-key) "1")
                result (enrichment/enrich-with-toinenaste
                         (make-application {:content {:answers answers}})
                         base-questions)
                hk1 (first (filter #(= hakukohde-oid-1 (:oid %)) (:hakukohteet result)))
                hk2 (first (filter #(= hakukohde-oid-2 (:oid %)) (:hakukohteet result)))]
            (should= true  (:terveys hk1))
            (should= false (:terveys hk2))))

      (it "detects aiempiPeruminen SORA per hakukohde from applicant answers"
          (let [aiempi-key (str "sora-aiempi_" hakukohde-oid-2)
                answers (make-answers (keyword aiempi-key) "1")
                result (enrichment/enrich-with-toinenaste
                         (make-application {:content {:answers answers}})
                         base-questions)
                hk2 (first (filter #(= hakukohde-oid-2 (:oid %)) (:hakukohteet result)))]
            (should= true (:aiempiPeruminen hk2))))

      (it "detects kiinnostunutKaksoistutkinnosta per hakukohde from applicant answers"
          (let [kaksois-key (str "kaksoistutkinto-lukio_" hakukohde-oid-1)
                answers (make-answers (keyword kaksois-key) "0")
                result (enrichment/enrich-with-toinenaste
                         (make-application {:content {:answers answers}})
                         base-questions)
                hk1 (first (filter #(= hakukohde-oid-1 (:oid %)) (:hakukohteet result)))
                hk2 (first (filter #(= hakukohde-oid-2 (:oid %)) (:hakukohteet result)))]
            (should= true (:kiinnostunutKaksoistutkinnosta hk1))
            (should= nil  (:kiinnostunutKaksoistutkinnosta hk2)))))

    (describe "top-level :kiinnostunutUrheilijanAmmatillisestaKoulutuksesta (per-applicant)"

      (it "is true when the form has the urheilija-amm wrapper and the applicant answered \"0\""
          (let [answers (make-answers :urheilija-amm-key "0")
                result (enrichment/enrich-with-toinenaste
                         (make-application {:content {:answers answers}})
                         urheilija-amm-questions)]
            (should= true (:kiinnostunutUrheilijanAmmatillisestaKoulutuksesta result))))

      (it "is false when the form asks the question and the applicant answered something other than \"0\""
          (let [answers (make-answers :urheilija-amm-key "1")
                result (enrichment/enrich-with-toinenaste
                         (make-application {:content {:answers answers}})
                         urheilija-amm-questions)]
            (should= false (:kiinnostunutUrheilijanAmmatillisestaKoulutuksesta result))))

      (it "is nil when the form has no urheilija-amm wrapper"
          (let [result (enrichment/enrich-with-toinenaste (make-application {}) base-questions)]
            (should-be-nil (:kiinnostunutUrheilijanAmmatillisestaKoulutuksesta result))))

      (it "is nil when the form has the wrapper but the applicant didn't answer"
          (let [result (enrichment/enrich-with-toinenaste (make-application {}) urheilija-amm-questions)]
            (should-be-nil (:kiinnostunutUrheilijanAmmatillisestaKoulutuksesta result)))))

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
