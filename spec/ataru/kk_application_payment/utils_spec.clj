(ns ataru.kk-application-payment.utils-spec
  (:require [ataru.component-data.kk-application-payment-module :as payment-module]
            [ataru.kk-application-payment.utils :as utils]
            [speclj.core :refer [should should-not describe tags it]]))


(describe "kk-application-payment-module"
          (tags :unit :payment)

          (it "form does not have application payment module"
              (should-not (utils/has-payment-module? {:content [{:id "not-payment"}]})))

          (it "form has application payment module"
              (should (utils/has-payment-module? {:content [(payment-module/kk-application-payment-module)]}))))

