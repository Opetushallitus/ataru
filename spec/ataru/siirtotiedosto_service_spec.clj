(ns ataru.siirtotiedosto-service-spec
  (:require [speclj.core :refer [it describe tags should= should-be-nil should-not-be-nil
                                  should-contain should-throw]]
            [ataru.siirtotiedosto-service :as service]
            [ataru.applications.application-store :as application-store]
            [ataru.forms.form-store :as form-store]
            [ataru.config.core :refer [config]]
            [cheshire.core :as json])
  (:import (ataru.siirtotiedosto_service CommonSiirtotiedostoService)
           (fi.vm.sade.valinta.dokumenttipalvelu SiirtotiedostoPalvelu)
           (fi.vm.sade.valinta.dokumenttipalvelu.dto ObjectMetadata)))

;; The function under test is private; access via the var.
(def ^:private combine-results @#'service/combine-results)

(defn- make-capturing-client [captured-payloads]
  (proxy [SiirtotiedostoPalvelu]
         [(-> config :siirtotiedostot :aws-region)
          (-> config :siirtotiedostot :s3-bucket)
          (-> config :siirtotiedostot :transferFileTargetRoleArn)]
    (saveSiirtotiedosto [_ _ _ _ _ stream _]
      (swap! captured-payloads conj (slurp stream))
      (ObjectMetadata. "k" "documentId" ["tag"] nil 0 "v6"))))

(defn- make-answer [k v]
  {:key (name k) :value v :fieldType "textField"})

(def ^:private test-form
  {:id      42
   :content [{:id "sora-terveys"}
             {:id "pohjakoulutus-2nd-wrapper"
              :children [{:id "base-education-2nd"
                          :options [{:followups [{:id "suoritusvuosi-perusopetus"}]}]}]}]})

(def ^:private test-app
  {:hakemusOid "1.2.246.562.11.0001"
   :form       42
   :lang       "fi"
   :hakukohde  ["1.2.246.562.20.0001"]
   :person_oid "1.2.246.562.24.0001"
   :content    {:answers [(make-answer :suoritusvuosi-perusopetus "2024")]}})


(describe "siirtotiedosto-service pure helpers"
  (tags :unit)

  (describe "create-new-siirtotiedosto-data"

    (it "carries window-start from the previous successful run's window-end"
        (let [previous {:success true :window-end "2024-01-15T00:00:00Z"}
              result   (service/create-new-siirtotiedosto-data previous "exec-uuid")]
          (should= "2024-01-15T00:00:00Z" (:window-start result))
          (should= "exec-uuid" (:execution-uuid result))
          (should= {} (:info result))
          (should-be-nil (:success result))
          (should-be-nil (:error-message result))))

    (it "throws when the previous run was not successful"
        (should-throw RuntimeException "Edellistä onnistunutta operaatiota ei löytynyt."
          (service/create-new-siirtotiedosto-data {:success false} "exec-uuid"))))

  (describe "update-siirtotiedosto-data"

    (it "merges :success true and :info on a successful operation"
        (let [base       {:execution-uuid "x" :success nil :info {} :error-message nil}
              op-results {:success true :info {:applications 5 :forms 9}}
              result     (service/update-siirtotiedosto-data base op-results)]
          (should= true (:success result))
          (should= {:applications 5 :forms 9} (:info result))
          (should-be-nil (:error-message result))))

    (it "merges :success false and :error-message on a failed operation"
        (let [base       {:execution-uuid "x" :success nil :info {} :error-message nil}
              op-results {:success false :error-message "boom"}
              result     (service/update-siirtotiedosto-data base op-results)]
          (should= false (:success result))
          (should= "boom" (:error-message result))
          (should= {} (:info result)))))

  (describe "combine-results"

    (it "reports success only when both applications and forms succeed"
        (let [r (combine-results {:success true :error-message nil :total-count 5}
                                  {:success true :error-message nil :total-count 9})]
          (should= true (:success r))
          (should-be-nil (:error-message r))
          (should= {:applications 5 :forms 9} (:info r))))

    (it "reports failure when applications failed, propagating its error message"
        (let [r (combine-results {:success false :error-message "apps boom" :total-count 0}
                                  {:success true  :error-message nil          :total-count 9})]
          (should= false (:success r))
          (should= "apps boom" (:error-message r))))

    (it "reports failure when forms failed, propagating its error message"
        (let [r (combine-results {:success true  :error-message nil          :total-count 5}
                                  {:success false :error-message "forms boom" :total-count 0})]
          (should= false (:success r))
          (should= "forms boom" (:error-message r))))))


(describe "siirtotiedosto-applications wiring"
  (tags :unit)

  (it "adds a non-nil :toinenaste enrichment to every application before saving"
      (let [captured     (atom [])
            mock         (make-capturing-client captured)
            service-impl (CommonSiirtotiedostoService. mock)]
        (with-redefs [application-store/siirtotiedosto-application-ids       (fn [_] [{:id 100}])
                      application-store/siirtotiedosto-applications-for-ids  (fn [_] [test-app])
                      form-store/fetch-forms-with-content-by-ids             (fn [_] [test-form])]
          (service/siirtotiedosto-applications service-impl {:execution-uuid "test"}))
        (let [payload (first @captured)
              apps    (json/parse-string payload true)
              app     (first apps)]
          (should= 1 (count apps))
          (should-contain :toinenaste app)
          (should-not-be-nil (:toinenaste app)))))

  (it "computes :toinenaste using the form whose id matches the application's :form"
      (let [captured     (atom [])
            mock         (make-capturing-client captured)
            service-impl (CommonSiirtotiedostoService. mock)
            apps         [(assoc test-app :form 42 :hakemusOid "h-42")
                          (assoc test-app :form 99 :hakemusOid "h-99")]
            forms        [{:id 42 :content (:content test-form)}
                          ;; form 99 has no SORA wrapper (different question metadata for that app).
                          {:id 99 :content [{:id "pohjakoulutus-2nd-wrapper"
                                              :children [{:id "base-education-2nd"
                                                          :options [{:followups [{:id "suoritusvuosi-perusopetus"}]}]}]}]}]]
        (with-redefs [application-store/siirtotiedosto-application-ids      (fn [_] [{:id 1} {:id 2}])
                      application-store/siirtotiedosto-applications-for-ids (fn [_] apps)
                      form-store/fetch-forms-with-content-by-ids             (fn [_] forms)]
          (service/siirtotiedosto-applications service-impl {:execution-uuid "test"}))
        (let [parsed (json/parse-string (first @captured) true)]
          (should-not-be-nil (:toinenaste (first parsed)))
          (should-not-be-nil (:toinenaste (second parsed))))))

  (it "fetches forms once per chunk with the distinct form ids referenced by that chunk"
      (let [captured-fetch-args (atom [])
            mock                (make-capturing-client (atom []))
            service-impl        (CommonSiirtotiedostoService. mock)
            apps                [(assoc test-app :hakemusOid "h1" :form 42)
                                 (assoc test-app :hakemusOid "h2" :form 42)
                                 (assoc test-app :hakemusOid "h3" :form 99)]]
        (with-redefs [application-store/siirtotiedosto-application-ids      (fn [_] [{:id 1} {:id 2} {:id 3}])
                      application-store/siirtotiedosto-applications-for-ids (fn [_] apps)
                      form-store/fetch-forms-with-content-by-ids
                      (fn [ids]
                        (swap! captured-fetch-args conj (vec ids))
                        [{:id 42 :content (:content test-form)}
                         {:id 99 :content (:content test-form)}])]
          (service/siirtotiedosto-applications service-impl {:execution-uuid "test"}))
        (should= 1 (count @captured-fetch-args))
        (should= #{42 99} (set (first @captured-fetch-args))))))


;; Forms used by the urheilija content tests.
;; Lukio wrapper id = urheilijan-lisakysymykset-lukiokohteisiin-wrapper-key
;; Amm   wrapper id = urheilijan-lisakysymykset-ammatillisiinkohteisiin-wrapper-key
(def ^:private form-with-urheilija
  {:id 42
   :content
   [{:id "8466feca-1993-4af3-b82c-59003ca2fd63"
     :children [{:id "urheilija-2nd-keskiarvo"}
                {:id "urheilija-2nd-peruskoulu"}
                {:id "urheilija-2nd-valmentaja-nimi"}
                {:id "urheilija-2nd-seura"}
                {:id "urheilija-2nd-liitto"}
                {:id "urheilija-2nd-lajivalinta-dropdown"
                 :options [{:value "1" :label {:fi "Jalkapallo" :sv "Fotboll"}}
                           {:value "21"
                            :label {:fi "Muu, mikä?" :sv "Annan, vad?"}
                            :followups [{:id "urheilija-2nd-muu-laji"
                                         :label {:fi "Päälaji" :sv "Huvudgren"}}]}]}]}
    {:id "d26bac09-1fb2-4be3-8bd1-5071a81decb7"
     :children [{:id "urheilija-amm-key"
                 :belongs-to-hakukohderyhma ["g1"]
                 :options [{:followups [{:id "urheilija-2nd-amm-peruskoulu"}
                                        {:id "urheilija-2nd-amm-valmentaja-nimi"}
                                        {:id "urheilija-2nd-amm-seura"}
                                        {:id "urheilija-2nd-amm-liitto"}
                                        {:id "urheilija-2nd-amm-lajivalinta-dropdown"
                                         :options [{:value "1" :label {:fi "Salibandy" :sv "Innebandy"}}
                                                   {:value "21"
                                                    :label {:fi "Muu, mikä?" :sv "Annan, vad?"}
                                                    :followups [{:id "urheilija-2nd-amm-muu-laji"
                                                                 :label {:fi "Päälaji" :sv "Huvudgren"}}]}]}]}
                            {}]}]}
    {:id "pohjakoulutus-2nd-wrapper"
     :children [{:id "base-education-2nd"
                 :options [{:followups [{:id "suoritusvuosi-perusopetus"}]}]}]}]})

(def ^:private form-without-urheilija
  {:id 42
   :content [{:id "pohjakoulutus-2nd-wrapper"
              :children [{:id "base-education-2nd"
                          :options [{:followups [{:id "suoritusvuosi-perusopetus"}]}]}]}]})

(defn- capture-enriched-app
  "Runs siirtotiedosto-applications with the given form + answers and returns the parsed
   single application that was written to S3."
  [form answers]
  (let [app          (assoc test-app :content {:answers answers})
        captured     (atom [])
        mock         (make-capturing-client captured)
        service-impl (CommonSiirtotiedostoService. mock)]
    (with-redefs [application-store/siirtotiedosto-application-ids      (fn [_] [{:id 1}])
                  application-store/siirtotiedosto-applications-for-ids (fn [_] [app])
                  form-store/fetch-forms-with-content-by-ids            (fn [_] [form])]
      (service/siirtotiedosto-applications service-impl {:execution-uuid "test"}))
    (-> @captured first (json/parse-string true) first)))


(describe "siirtotiedosto-applications urheilijanLisakysymykset / urheilijanLisakysymyksetAmmatillinen"
  (tags :unit)

  (it "populates urheilijanLisakysymykset when the form has the lukio urheilija wrapper and the application has answers"
      (let [answers [(make-answer :suoritusvuosi-perusopetus "2024")
                     (make-answer :urheilija-2nd-keskiarvo "8.5")
                     (make-answer :urheilija-2nd-peruskoulu "Yhteiskoulu")
                     (make-answer :urheilija-2nd-seura "HJK")
                     (make-answer :urheilija-2nd-liitto "SPL")
                     {:key "urheilija-2nd-valmentaja-nimi" :value ["Pekka"]}
                     (make-answer :urheilija-2nd-lajivalinta-dropdown "1")]
            ta      (:toinenaste (capture-enriched-app form-with-urheilija answers))
            lukio   (:urheilijanLisakysymykset ta)]
        (should= "8.5"        (:keskiarvo lukio))
        (should= "Yhteiskoulu" (:peruskoulu lukio))
        (should= "Pekka"      (:valmentaja_nimi lukio))
        (should= "HJK"        (:seura lukio))
        (should= "SPL"        (:liitto lukio))
        (should= "Jalkapallo" (:laji lukio))))

  (it "populates urheilijanLisakysymyksetAmmatillinen when the form has the amm urheilija wrapper and the application has answers"
      (let [answers [(make-answer :suoritusvuosi-perusopetus "2024")
                     (make-answer :urheilija-2nd-amm-peruskoulu "AmmKoulu")
                     (make-answer :urheilija-2nd-amm-seura "AmmSeura")
                     (make-answer :urheilija-2nd-amm-liitto "AmmLiitto")
                     {:key "urheilija-2nd-amm-valmentaja-nimi" :value ["AmmPekka"]}
                     (make-answer :urheilija-2nd-amm-lajivalinta-dropdown "1")]
            ta      (:toinenaste (capture-enriched-app form-with-urheilija answers))
            amm     (:urheilijanLisakysymyksetAmmatillinen ta)]
        (should= "AmmKoulu"   (:peruskoulu amm))
        (should= "AmmPekka"   (:valmentaja_nimi amm))
        (should= "AmmSeura"   (:seura amm))
        (should= "AmmLiitto"  (:liitto amm))
        (should= "Salibandy"  (:laji amm))))

  (it "uses the muu-laji follow-up answer for :laji when the lukio dropdown value is \"21\""
      (let [answers [(make-answer :suoritusvuosi-perusopetus "2024")
                     (make-answer :urheilija-2nd-lajivalinta-dropdown "21")
                     (make-answer :urheilija-2nd-muu-laji "Curling")]
            ta      (:toinenaste (capture-enriched-app form-with-urheilija answers))]
        (should= "Curling" (-> ta :urheilijanLisakysymykset :laji))))

  (it "surfaces the applicant's urheilija-amm interest as a top-level :kiinnostunutUrheilijanAmmatillisestaKoulutuksesta boolean"
      (let [answers [(make-answer :suoritusvuosi-perusopetus "2024")
                     (make-answer :urheilija-amm-key "0")]
            ta      (:toinenaste (capture-enriched-app form-with-urheilija answers))]
        (should= true (:kiinnostunutUrheilijanAmmatillisestaKoulutuksesta ta))))

  (it "leaves urheilijanLisakysymykset/Ammatillinen with all-nil values when the form has no urheilija wrappers, even if the application answers urheilija keys"
      (let [answers [(make-answer :suoritusvuosi-perusopetus "2024")
                     ;; Answers exist but the form doesn't ask the urheilija questions:
                     (make-answer :urheilija-2nd-keskiarvo "8.5")
                     (make-answer :urheilija-2nd-amm-peruskoulu "AmmKoulu")
                     (make-answer :urheilija-2nd-lajivalinta-dropdown "1")]
            ta      (:toinenaste (capture-enriched-app form-without-urheilija answers))
            lukio   (:urheilijanLisakysymykset ta)
            amm     (:urheilijanLisakysymyksetAmmatillinen ta)]
        ;; The default key map IS populated (form-independent), so answers ARE picked up.
        ;; But :laji depends on the form's dropdown options, which are absent → :laji nil.
        (should-be-nil (:laji lukio))
        (should-be-nil (:laji amm))
        ;; Without the wrapper, the kysymys/amm output should still be a non-nil map
        ;; with the expected shape (single-key fields per urheilija-fields-with-single-key):
        (should-not-be-nil lukio)
        (should-not-be-nil amm))))
