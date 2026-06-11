(ns ataru.applications.application-store-toinenaste-spec
  (:require [speclj.core :refer [it describe tags should should= should-not= should-contain
                                  should-be-nil should-not-be-nil]]
            [ataru.applications.application-store :as store]
            [ataru.applications.question-util :as question-util]
            [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-types :refer [harkinnanvaraisuus-reasons]])
  (:import [org.joda.time DateTime DateTimeZone]))

;; The function under test is private; access it via the var.
(def ^:private unwrap-toinenaste
  @#'store/unwrap-hakurekisteri-application-toinenaste)

(def ^:private hakukohde-oid-1 "1.2.246.562.20.00000000001")
(def ^:private hakukohde-oid-2 "1.2.246.562.20.00000000002")

(def ^:private form-with-toinenaste-questions
  {:content
   [{:id "pohjakoulutus-2nd-wrapper"
     :children [{:id "base-education-2nd"
                 :options [{:followups [{:id "suoritusvuosi-perusopetus"}
                                        {:id "tutkintokieli-perusopetus"}]}]}]}
    {:id "sorawrapper"
     :children [{:id "sora-terveys"}
                {:id "sora-aiempi"}]}]})

(def ^:private base-questions
  (question-util/get-hakurekisteri-toinenaste-specific-questions form-with-toinenaste-questions))

;; "Muu, mikä?" label triggers the muu-laji follow-up dropdown lookup.
(def ^:private form-with-urheilija
  {:content
   [;; LUKIO urheilija wrapper - children are direct sport fields + a dropdown
    {:id "8466feca-1993-4af3-b82c-59003ca2fd63" ;urheilijan-lisakysymykset-lukiokohteisiin-wrapper-key
     :children [{:id "urheilija-2nd-keskiarvo"}
                {:id "urheilija-2nd-peruskoulu"}
                {:id "urheilija-2nd-tamakausi"}
                {:id "urheilija-2nd-viimekausi"}
                {:id "urheilija-2nd-toissakausi"}
                {:id "urheilija-2nd-sivulaji"}
                {:id "urheilija-2nd-valmentaja-nimi"}
                {:id "urheilija-2nd-valmentaja-email"}
                {:id "urheilija-2nd-valmentaja-puh"}
                {:id "urheilija-2nd-valmennus-seurajoukkue"}
                {:id "urheilija-2nd-valmennus-piirijoukkue"}
                {:id "urheilija-2nd-valmennus-maajoukkue"}
                {:id "urheilija-2nd-seura"}
                {:id "urheilija-2nd-liitto"}
                {:id "urheilija-2nd-lajivalinta-dropdown"
                 :options [{:value "1" :label {:fi "Jalkapallo" :sv "Fotboll"}}
                           {:value "2" :label {:fi "Jääkiekko" :sv "Ishockey"}}
                           {:value "21"
                            :label {:fi "Muu, mikä?" :sv "Annan, vad?"}
                            :followups [{:id "urheilija-2nd-muu-laji"
                                         :label {:fi "Päälaji" :sv "Huvudgren"}}]}]}]}
    ;; AMM urheilija wrapper - nested under first option's followups
    {:id "d26bac09-1fb2-4be3-8bd1-5071a81decb7" ;urheilijan-lisakysymykset-ammatillisiinkohteisiin-wrapper-key
     :children [{:id "urheilija-amm-key"
                 :belongs-to-hakukohderyhma ["g1"]
                 :options [{:followups [{:id "urheilija-2nd-amm-peruskoulu"}
                                        {:id "urheilija-2nd-amm-tamakausi"}
                                        {:id "urheilija-2nd-amm-viimekausi"}
                                        {:id "urheilija-2nd-amm-toissakausi"}
                                        {:id "urheilija-2nd-amm-sivulaji"}
                                        {:id "urheilija-2nd-amm-valmentaja-nimi"}
                                        {:id "urheilija-2nd-amm-valmentaja-email"}
                                        {:id "urheilija-2nd-amm-valmentaja-puh"}
                                        {:id "urheilija-2nd-amm-valmennus-seurajoukkue"}
                                        {:id "urheilija-2nd-amm-valmennus-piirijoukkue"}
                                        {:id "urheilija-2nd-amm-valmennus-maajoukkue"}
                                        {:id "urheilija-2nd-amm-seura"}
                                        {:id "urheilija-2nd-amm-liitto"}
                                        {:id "urheilija-2nd-amm-lajivalinta-dropdown"
                                         :options [{:value "1" :label {:fi "Salibandy" :sv "Innebandy"}}
                                                   {:value "21"
                                                    :label {:fi "Muu, mikä?" :sv "Annan, vad?"}
                                                    :followups [{:id "urheilija-2nd-amm-muu-laji"
                                                                 :label {:fi "Päälaji" :sv "Huvudgren"}}]}]}]}
                            {}]}]}
    ;; pohjakoulutus wrapper (needed so tutkintoVuosi can be answered).
    {:id "pohjakoulutus-2nd-wrapper"
     :children [{:id "base-education-2nd"
                 :options [{:followups [{:id "suoritusvuosi-perusopetus"}]}]}]}]})

(def ^:private urheilija-questions
  (question-util/get-hakurekisteri-toinenaste-specific-questions form-with-urheilija))

(defn- make-answer [k v]
  {:key (name k) :value v :fieldType "textField"})

;; Tutkinto-vuosi must be answered: the existing code calls (edn/read-string tutkinto-vuosi)
;; without nil/empty guards, so a missing value would throw and the whole result would be nil.
(def ^:private base-answers
  [(make-answer :suoritusvuosi-perusopetus "2024")])

(defn- create-hakemus [overrides]
  (merge
    {:key                "1.2.246.562.11.99999999991"
     :hakukohde          [hakukohde-oid-1 hakukohde-oid-2]
     :created_time       (DateTime. 2024 1 15 10 30 0 (DateTimeZone/forID "Europe/Helsinki"))
     :submitted          (DateTime. 2024 1 15 10 35 0 (DateTimeZone/forID "Europe/Helsinki"))
     :person_oid         "1.2.246.562.24.99999999991"
     :lang               "fi"
     :email              "row-email@example.fi"
     :content            {:answers base-answers}
     :attachment_reviews {}}
    overrides))

(defn- haun-hakukohteet
  ([] (haun-hakukohteet true))
  ([allow-harkinnanvaraisesti?]
   [{:oid hakukohde-oid-1 :voiko-hakukohteessa-olla-harkinnanvaraisesti-hakeneita? allow-harkinnanvaraisesti?}
    {:oid hakukohde-oid-2 :voiko-hakukohteessa-olla-harkinnanvaraisesti-hakeneita? allow-harkinnanvaraisesti?}]))

(describe "unwrap-hakurekisteri-application-toinenaste"
  (tags :unit)

  (it "returns a non-nil map for a minimal valid input"
      (should-not-be-nil (unwrap-toinenaste base-questions [] (haun-hakukohteet) (create-hakemus {}))))

  (it "includes the expected top-level keys"
      (let [result (unwrap-toinenaste base-questions [] (haun-hakukohteet) (create-hakemus {}))]
        (doseq [k [:oid :personOid :createdTime :hakemusFirstSubmittedTime
                   :kieli :hakukohteet :email :matkapuhelin :lahiosoite :postinumero
                   :postitoimipaikka :asuinmaa :kotikunta
                   :sahkoisenAsioinninLupa :valintatuloksenJulkaisulupa :koulutusmarkkinointilupa
                   :attachments :huoltajat
                   :pohjakoulutus :tutkintoKieli :tutkintoVuosi
                   :kiinnostunutOppisopimusKoulutuksesta
                   :urheilijanLisakysymykset :urheilijanLisakysymyksetAmmatillinen]]
          (should-contain k result))))

  (it "maps :key from the DB row to :oid"
      (let [result (unwrap-toinenaste base-questions [] (haun-hakukohteet)
                                      (create-hakemus {:key "1.2.246.562.11.ABC"}))]
        (should= "1.2.246.562.11.ABC" (:oid result))))

  (it "maps :person_oid from the DB row to :personOid"
      (let [result (unwrap-toinenaste base-questions [] (haun-hakukohteet)
                                      (create-hakemus {:person_oid "1.2.246.562.24.XYZ"}))]
        (should= "1.2.246.562.24.XYZ" (:personOid result))))

  (it "uses :email from the DB row (denormalized), not from answers"
      (let [content-with-answer-email {:answers (conj base-answers
                                                       (make-answer :email "answer-email@example.fi"))}
            result (unwrap-toinenaste base-questions [] (haun-hakukohteet)
                                       (create-hakemus {:email "row-email@example.fi"
                                                     :content  content-with-answer-email}))]
        (should= "row-email@example.fi" (:email result))))

  (it "formats :createdTime via JodaFormatter as Helsinki-local ISO without offset"
      (let [t (DateTime. 2024 6 15 12 30 0 (DateTimeZone/forID "Europe/Helsinki"))
            result (unwrap-toinenaste base-questions [] (haun-hakukohteet)
                                      (create-hakemus {:created_time t}))]
        (should= "2024-06-15T12:30:00" (:createdTime result))))

  (it "formats :hakemusFirstSubmittedTime via JodaFormatter"
      (let [t (DateTime. 2024 6 15 12 30 0 (DateTimeZone/forID "Europe/Helsinki"))
            result (unwrap-toinenaste base-questions [] (haun-hakukohteet)
                                      (create-hakemus {:submitted t}))]
        (should= "2024-06-15T12:30:00" (:hakemusFirstSubmittedTime result))))

  (it "shapes :attachments by stringifying attachment_reviews keys"
      (let [reviews {:liite-1 "ATTACHMENT_MISSING"
                     :liite-2 "ATTACHMENT_OK"}
            result (unwrap-toinenaste base-questions [] (haun-hakukohteet)
                                      (create-hakemus {:attachment_reviews reviews}))]
        (should= {"liite-1" "ATTACHMENT_MISSING"
                  "liite-2" "ATTACHMENT_OK"}
                 (:attachments result))))

  (describe "harkinnanvaraisuus per hakukohde uses real haun-hakukohteet"

    (it "marks a hakukohde that does not allow harkinnanvarainen as :ei-harkinnanvarainen-hakukohde"
        (let [mixed [{:oid hakukohde-oid-1 :voiko-hakukohteessa-olla-harkinnanvaraisesti-hakeneita? true}
                     {:oid hakukohde-oid-2 :voiko-hakukohteessa-olla-harkinnanvaraisesti-hakeneita? false}]
              result (unwrap-toinenaste base-questions [] mixed (create-hakemus {}))
              hk2 (first (filter #(= hakukohde-oid-2 (:oid %)) (:hakukohteet result)))]
          (should= (:ei-harkinnanvarainen-hakukohde harkinnanvaraisuus-reasons)
                   (:harkinnanvaraisuus hk2))))

    (it "does not mark a hakukohde that allows harkinnanvarainen as :ei-harkinnanvarainen-hakukohde"
        (let [allow [{:oid hakukohde-oid-1 :voiko-hakukohteessa-olla-harkinnanvaraisesti-hakeneita? true}
                     {:oid hakukohde-oid-2 :voiko-hakukohteessa-olla-harkinnanvaraisesti-hakeneita? true}]
              result (unwrap-toinenaste base-questions [] allow (create-hakemus {}))
              hk1 (first (filter #(= hakukohde-oid-1 (:oid %)) (:hakukohteet result)))]
          (should-not= (:ei-harkinnanvarainen-hakukohde harkinnanvaraisuus-reasons)
                       (:harkinnanvaraisuus hk1)))))

  (describe "urheilija-amm group filtering"

    (it "reports kiinnostunut only for hakukohdes listed in urheilija-amm-hakukohdes"
        (let [answers (conj base-answers (make-answer :urheilija-amm-key "0"))
              ;; Only hakukohde-oid-1 offers urheilija-amm training.
              result (unwrap-toinenaste urheilija-questions
                                        [hakukohde-oid-1]
                                        (haun-hakukohteet)
                                        (create-hakemus {:content {:answers answers}}))
              hk1 (first (filter #(= hakukohde-oid-1 (:oid %)) (:hakukohteet result)))
              hk2 (first (filter #(= hakukohde-oid-2 (:oid %)) (:hakukohteet result)))]
          (should= true (:kiinnostunutUrheilijanAmmatillisestaKoulutuksesta hk1))
          (should-be-nil (:kiinnostunutUrheilijanAmmatillisestaKoulutuksesta hk2))))

    (it "is nil for all hakukohdes when applicant has not expressed interest"
        (let [result (unwrap-toinenaste urheilija-questions
                                        [hakukohde-oid-1 hakukohde-oid-2]
                                        (haun-hakukohteet)
                                        (create-hakemus {}))]
          (should (every? nil? (map :kiinnostunutUrheilijanAmmatillisestaKoulutuksesta
                                     (:hakukohteet result)))))))

  (describe "shared field extraction (sanity checks)"

    (it "extracts pohjakoulutus from answers"
        (let [answers (conj base-answers (make-answer :base-education-2nd "1"))
              result (unwrap-toinenaste base-questions [] (haun-hakukohteet)
                                        (create-hakemus {:content {:answers answers}}))]
          (should= "1" (:pohjakoulutus result))))

    (it "parses tutkintoVuosi as integer"
        (let [result (unwrap-toinenaste base-questions [] (haun-hakukohteet) (create-hakemus {}))]
          (should= 2024 (:tutkintoVuosi result))))

    (it "builds first huoltaja from guardian answers (value is array)"
        (let [answers (concat base-answers
                              [{:key "guardian-firstname" :value ["Matti"]}
                               {:key "guardian-lastname"  :value ["Meikalainen"]}
                               {:key "guardian-phone"     :value ["0401111111"]}
                               {:key "guardian-email"     :value ["huoltaja@example.fi"]}])
              result (unwrap-toinenaste base-questions [] (haun-hakukohteet)
                                        (create-hakemus {:content {:answers answers}}))
              huoltaja (first (:huoltajat result))]
          (should= "Matti" (:etunimi huoltaja))
          (should= "Meikalainen" (:sukunimi huoltaja))
          (should= "0401111111" (:matkapuhelin huoltaja))
          (should= "huoltaja@example.fi" (:email huoltaja)))))

  (describe "urheilijanLisakysymykset (lukio)"

    (it "extracts all 14 single-key sport fields from answers"
        (let [answers (concat base-answers
                              [(make-answer :urheilija-2nd-keskiarvo "8.5")
                               (make-answer :urheilija-2nd-peruskoulu "Yhteiskoulu")
                               (make-answer :urheilija-2nd-tamakausi "Aktiivinen")
                               (make-answer :urheilija-2nd-viimekausi "Aktiivinen")
                               (make-answer :urheilija-2nd-toissakausi "Aktiivinen")
                               (make-answer :urheilija-2nd-sivulaji "Yleisurheilu")
                               (make-answer :urheilija-2nd-valmentaja-nimi "Pekka")
                               (make-answer :urheilija-2nd-valmentaja-email "pekka@example.fi")
                               (make-answer :urheilija-2nd-valmentaja-puh "0407654321")
                               (make-answer :urheilija-2nd-valmennus-seurajoukkue "Klubi")
                               (make-answer :urheilija-2nd-valmennus-piirijoukkue "Piiri")
                               (make-answer :urheilija-2nd-valmennus-maajoukkue "Maajoukkue")
                               (make-answer :urheilija-2nd-seura "HJK")
                               (make-answer :urheilija-2nd-liitto "SPL")])
              result (unwrap-toinenaste urheilija-questions [] (haun-hakukohteet)
                                        (create-hakemus {:content {:answers answers}}))
              sports (:urheilijanLisakysymykset result)]
          (should= {:keskiarvo                   "8.5"
                    :peruskoulu                  "Yhteiskoulu"
                    :tamakausi                   "Aktiivinen"
                    :viimekausi                  "Aktiivinen"
                    :toissakausi                 "Aktiivinen"
                    :sivulaji                    "Yleisurheilu"
                    :valmentaja_nimi             "Pekka"
                    :valmentaja_email            "pekka@example.fi"
                    :valmentaja_puh              "0407654321"
                    :valmennusryhma_seurajoukkue "Klubi"
                    :valmennusryhma_piirijoukkue "Piiri"
                    :valmennusryhma_maajoukkue   "Maajoukkue"
                    :seura                       "HJK"
                    :liitto                      "SPL"
                    :laji                        nil}
                   sports)))

    (it "produces nil values for all fields when no sport answers are provided"
        (let [result (unwrap-toinenaste urheilija-questions [] (haun-hakukohteet) (create-hakemus {}))
              sports (:urheilijanLisakysymykset result)]
          (doseq [k [:keskiarvo :peruskoulu :tamakausi :viimekausi :toissakausi :sivulaji
                     :valmentaja_nimi :valmentaja_email :valmentaja_puh
                     :valmennusryhma_seurajoukkue :valmennusryhma_piirijoukkue :valmennusryhma_maajoukkue
                     :seura :liitto :laji]]
            (should-be-nil (k sports)))))

    (it "takes the first element when an answer's :value is a vector (to-single-value)"
        ;; Valmentaja contact fields can be stored as single-element arrays in some forms.
        (let [answers (conj base-answers
                            {:key "urheilija-2nd-valmentaja-nimi" :value ["Pekka"]})
              result (unwrap-toinenaste urheilija-questions [] (haun-hakukohteet)
                                        (create-hakemus {:content {:answers answers}}))]
          (should= "Pekka" (-> result :urheilijanLisakysymykset :valmentaja_nimi))))

    (describe ":laji extraction from the dropdown"

      (it "resolves a regular dropdown value via value-to-label in the application's language (fi)"
          (let [answers (conj base-answers (make-answer :urheilija-2nd-lajivalinta-dropdown "1"))
                result (unwrap-toinenaste urheilija-questions [] (haun-hakukohteet)
                                          (create-hakemus {:lang "fi"
                                                           :content {:answers answers}}))]
            (should= "Jalkapallo" (-> result :urheilijanLisakysymykset :laji))))

      (it "resolves a regular dropdown value in Swedish when lang is \"sv\""
          (let [answers (conj base-answers (make-answer :urheilija-2nd-lajivalinta-dropdown "1"))
                result (unwrap-toinenaste urheilija-questions [] (haun-hakukohteet)
                                          (create-hakemus {:lang "sv"
                                                           :content {:answers answers}}))]
            (should= "Fotboll" (-> result :urheilijanLisakysymykset :laji))))

      (it "uses the muu-laji answer when the dropdown value is \"21\" (Muu, mikä?)"
          (let [answers (concat base-answers
                                [(make-answer :urheilija-2nd-lajivalinta-dropdown "21")
                                 (make-answer :urheilija-2nd-muu-laji "Curling")])
                result (unwrap-toinenaste urheilija-questions [] (haun-hakukohteet)
                                          (create-hakemus {:content {:answers answers}}))]
            (should= "Curling" (-> result :urheilijanLisakysymykset :laji))))))

  (describe "urheilijanLisakysymyksetAmmatillinen"

    (it "extracts amm sport fields from amm-specific answer keys (independent of lukio)"
        (let [answers (concat base-answers
                              [(make-answer :urheilija-2nd-amm-peruskoulu "AmmKoulu")
                               (make-answer :urheilija-2nd-amm-tamakausi "Aktiivinen")
                               (make-answer :urheilija-2nd-amm-valmentaja-nimi "AmmValmentaja")
                               (make-answer :urheilija-2nd-amm-seura "AmmSeura")
                               (make-answer :urheilija-2nd-amm-liitto "AmmLiitto")
                               ;; A lukio-keyed answer must NOT leak into the amm output.
                               (make-answer :urheilija-2nd-peruskoulu "LukioKoulu")])
              result (unwrap-toinenaste urheilija-questions [] (haun-hakukohteet)
                                        (create-hakemus {:content {:answers answers}}))
              amm (:urheilijanLisakysymyksetAmmatillinen result)]
          (should= "AmmKoulu"      (:peruskoulu amm))
          (should= "Aktiivinen"    (:tamakausi amm))
          (should= "AmmValmentaja" (:valmentaja_nimi amm))
          (should= "AmmSeura"      (:seura amm))
          (should= "AmmLiitto"     (:liitto amm))))

    (it "has nil :keskiarvo because amm question keys don't define a keskiarvo field"
        (let [result (unwrap-toinenaste urheilija-questions [] (haun-hakukohteet) (create-hakemus {}))]
          (should-be-nil (-> result :urheilijanLisakysymyksetAmmatillinen :keskiarvo))))

    (it "resolves :laji from the amm-specific dropdown"
        (let [answers (conj base-answers
                            (make-answer :urheilija-2nd-amm-lajivalinta-dropdown "1"))
              result (unwrap-toinenaste urheilija-questions [] (haun-hakukohteet)
                                        (create-hakemus {:content {:answers answers}}))]
          (should= "Salibandy" (-> result :urheilijanLisakysymyksetAmmatillinen :laji))))

    (it "uses the amm muu-laji answer when the amm dropdown value is \"21\""
        (let [answers (concat base-answers
                              [(make-answer :urheilija-2nd-amm-lajivalinta-dropdown "21")
                               (make-answer :urheilija-2nd-amm-muu-laji "Petanque")])
              result (unwrap-toinenaste urheilija-questions [] (haun-hakukohteet)
                                        (create-hakemus {:content {:answers answers}}))]
          (should= "Petanque" (-> result :urheilijanLisakysymyksetAmmatillinen :laji)))))

  (describe "error handling"

    (it "tolerates a missing tutkinto-vuosi answer"
        ;; All tutkintovuosi-keys are absent → tutkinto-vuosi is nil →
        ;; (edn/read-string nil) returns nil rather than throwing, so the whole result is still produced.
        (let [result (unwrap-toinenaste base-questions [] (haun-hakukohteet)
                                        (create-hakemus {:content {:answers []}}))]
          (should-not-be-nil result)
          (should-be-nil (:tutkintoVuosi result))))

    (it "returns nil when an unexpected exception occurs (try/catch wrapper)"
        ;; A non-map :attachment_reviews makes reduce-kv throw; the try/catch wrapper returns nil.
        (let [result (unwrap-toinenaste base-questions [] (haun-hakukohteet)
                                        (create-hakemus {:attachment_reviews [:not-a-map]}))]
          (should-be-nil result)))))
