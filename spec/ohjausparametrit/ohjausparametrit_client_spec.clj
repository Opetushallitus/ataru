(ns ohjausparametrit.ohjausparametrit-client-spec
  (:require [ataru.ohjausparametrit.ohjausparametrit-client :as oc]
            [ring.util.http-response :as resp]
            [org.httpkit.client :as http]
            [speclj.core :refer :all]
            [cheshire.core :as json]))

(def ohjausparametrit-resp {:PH_SS          {:dateStart 1506920400000, :dateEnd nil},
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
                            :__modifiedBy__ "1.2.246.562.24.64667668834",
                            :__modified__   1508400869203})

(describe "Getting ohjausparametri"
  (tags :unit)

  (around-all [specs]
    (with-redefs [http/get (fn [url]
                             (should= url "https://virkailija.untuva.aws.opintopolku.fi/ohjausparametrit-service/api/v1/rest/parametri/1.2.246.562.29.75477542726")
                             (-> ohjausparametrit-resp json/generate-string resp/ok future))]
      (specs)))

  (it "should get ohjausparametri from ohjausparametrit-service"
    (let [resp (oc/get-ohjausparametrit "1.2.246.562.29.75477542726")]
      (should= "2017-10-02T05:00:00.000Z"
               (-> resp :PH_SS :dateStart str))
      (should= "2017-10-02T05:00:00.000Z"
               (-> resp :PH_TJT :date str))
      (should= "2017-10-19T08:14:29.203Z"
               (-> resp :PH_YNH :dateEnd str))
      (should= "2017-10-19T08:14:29.203Z"
               (-> resp :__modified__ str)))))
