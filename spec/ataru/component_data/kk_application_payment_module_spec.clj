(ns ataru.component-data.kk-application-payment-module-spec
  (:require [ataru.component-data.kk-application-payment-module :as payment-module]
            [ataru.util :as util]
            [speclj.core :refer [describe tags it should-contain]]))

(describe "kk-application-payment-module"
          (tags :unit :attachments :payment)

          (it "should contain all the specified ids"
              (let [keys-generated (->> (payment-module/kk-application-payment-module)
                                        :children
                                        util/flatten-form-fields
                                        (map (comp name :id))
                                        set)
                    keys-to-check ["kk-application-payment-option"
                                   "asiakasnumero-migri"
                                   "passport-attachment"
                                   "eu-blue-card-attachment"
                                   "eu-passport-attachment"
                                   "continuous-residence-permit-front"
                                   "continuous-residence-permit-back"
                                   "continuous-residence-passport-attachment"
                                   "permanent-residence-permit"
                                   "permanent-residence-passport-attachment"
                                   "brexit-permit-attachment"
                                   "brexit-passport-attachment"
                                   "permanent-residence-permit"
                                   "permanent-residence-passport-attachment"
                                   "eu-family-member-permit"
                                   "eu-family-passport-attachment"
                                   "temporary-protection-permit"]]
                (doseq [id keys-to-check]
                  (should-contain id keys-generated)))))
