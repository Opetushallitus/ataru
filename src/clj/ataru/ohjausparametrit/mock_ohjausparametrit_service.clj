(ns ataru.ohjausparametrit.mock-ohjausparametrit-service
  (:require [ataru.ohjausparametrit.ohjausparametrit-protocol :refer [OhjausparametritService]]))

(defrecord MockOhjausparametritService []
  OhjausparametritService

  (get-parametri [_ haku-oid]
    {:PH_SS          {:dateStart 1506920400000, :dateEnd nil},
     :PH_OPVP        {:date nil},
     :PH_HKMT        {:date nil},
     :PH_HKLPT       {:date nil},
     :PH_VSSAV       {:date nil},
     :PH_HKP         {:date nil},
     :PH_AHP         {:date nil},
     :PH_TJT         {:date 1506920400000},
     :PH_VTSSV       {:date nil},
     :PH_EVR         {:date nil},
     :PH_VSTP        {:date nil},
     :PH_YNH         {:dateStart nil, :dateEnd 1508400869203},
     :target         "1.2.246.562.29.75477542726",
     :PH_VTJH        {:dateStart nil, :dateEnd nil},
     :PH_IP          {:date nil},
     :PH_LMT         (cond
                       (= haku-oid "payment-info-test-kk-haku-custom-grace") {:value 10000}
                       (= haku-oid "hakukohtainen-raja-käytössä") {:value 20})
     :synteettisetHakemukset (not (= haku-oid "1.2.246.562.29.12345678910"))
     :synteettisetLomakeavain (if (= haku-oid "1.2.246.562.29.12345678910") "" "synthetic-application-test-form")
     :liitteidenMuokkauksenHakemuskohtainenTakarajaKaytossa (if (= haku-oid "hakemuskohtainen-raja-käytössä") true nil)
     :liitteidenMuokkauksenHakemuskohtainenTakarajaPaivaa (if (= haku-oid "hakemuskohtainen-raja-käytössä") 15 nil)
     :liitteidenMuokkauksenHakemuskohtainenTakarajaKellonaika (if (= haku-oid "hakemuskohtainen-raja-käytössä") "16:15" nil)
     :liitteidenMuokkauksenHakukohtainenTakarajaKellonaika (if (= haku-oid "hakukohtainen-raja-käytössä") "15:14" nil)
     :__modifiedBy__ "1.2.246.562.24.64667668834",
     :__modified__   1508400869203}))
