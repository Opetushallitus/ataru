(ns ataru.kk-application-payment.fixtures)

(def koodisto-valtioryhmat-response
  [{:uri "valtioryhmat_1"
    :version 1
    :value "EU"
    :label {}
    :valid { :start "2015-09-03T00:00:00+03:00" }
    :within [{:uri "maatjavaltiot2_246"
              :version 1
              :value "246"}
             {:uri "maatjavaltiot2_250"
              :version 1
              :value "250"}
             {:uri "maatjavaltiot2_233"
              :version 1
              :value "233"}
             {:uri "maatjavaltiot2_056"
              :version 1
              :value "056"}]}
   {:uri "valtioryhmat_2"
    :version 1
    :value "ETA"
    :label {}
    :valid { :start "2015-09-03T00:00:00+03:00" }
    :within {}}])

(defn haku-with-hakuajat
  [hakuaika-start hakuaika-end]
  {:prioritize-hakukohteet false
   :hakukausi-vuosi 2025
   :yhteishaku false
   :name {:fi "testing2"}
   :alkamiskausi "kausi_s#1"
   :haun-tiedot-url "https://toimimaton.virkailija-host-arvo.test.edn-tiedostosta/kouta/haku/payment-info-test-kk-haku"
   :hakukohteet ["payment-info-test-kk-hakukohde"]
   :oid "payment-info-test-kk-haku"
   :hakuajat [{:hakuaika-id "kouta-hakuaika-id",
               :start hakuaika-start,
               :end hakuaika-end}],
   :kohdejoukko-uri "haunkohdejoukko_12#1",
   :kohdejoukon-tarkenne-uri "haunkohdejoukontarkenne_1#1",
   :can-submit-multiple-applications false,
   :hakutapa-uri "hakutapa_02#1",
   :ylioppilastutkinto-antaa-hakukelpoisuuden? false,
   :alkamisvuosi 2025,
   :ataru-form-key "41101b4f-1762-49af-9db0-e3603adae3ad",
   :sijoittelu false})