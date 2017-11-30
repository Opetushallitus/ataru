(ns ataru.ohjausparametrit.mock-ohjausparametrit-service
  (:require [ataru.ohjausparametrit.ohjausparametrit-protocol :refer [OhjausparametritService]]))

(defrecord MockOhjausparametritService []
  OhjausparametritService

  (get-parametri [_ _]
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
     :__modifiedBy__ "1.2.246.562.24.64667668834",
     :__modified__   1508400869203}))
