(ns ataru.kk-application-payment.utils-spec
  (:require [ataru.component-data.kk-application-payment-module :as payment-module :refer [kk-application-payment-wrapper-key]]
            [ataru.component-data.person-info-module :refer [person-info-module]]
            [ataru.kk-application-payment.utils :as utils]
            [ataru.component-data.component :as c]
            [speclj.core :refer [should should= should-not describe tags it]]))


(describe "kk-application-payment-module"
          (tags :unit :payment)

          (it "form does not have application payment module"
              (should-not (utils/has-payment-module? {:content [{:id "not-payment"}]})))

          (it "form has application payment module"
              (should (utils/has-payment-module? {:content [(payment-module/kk-application-payment-module)]})))

          (it "injects payment module to form after person-info"
              (let [form            {:content [
                                               (c/form-section [])
                                               (c/form-section [])
                                               (c/form-section [])
                                               (c/form-section [])
                                               (person-info-module :onr)
                                               (c/form-section [])
                                               (c/form-section [])]}
                    injected-form  (utils/inject-payment-module-to-form form)]
                (should= kk-application-payment-wrapper-key (-> (:content injected-form)
                                                                (nth 5)
                                                                :id))))

          (it "injects payment module to form and updates person-info"
              (let [form          {:content [(person-info-module :onr)]}
                    injected-form (utils/inject-payment-module-to-form form)
                    ids           (map :id (:content injected-form))]
                (should= 2 (count ids))
                (should (some #(= % kk-application-payment-wrapper-key) ids))
                (should (some #(= % "onr-kk-application-payment") ids)))))

