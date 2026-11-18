(ns ataru.component-data.kk-application-payment-module-spec
  (:require [ataru.component-data.kk-application-payment-module :as payment-module]
            [ataru.util :as util]
            [ataru.translations.texts :refer [kk-application-payment-module-texts]]
            [speclj.core :refer [describe tags it should-contain should-not-contain should=]]))

(defn- document-options []
  (-> (payment-module/kk-application-payment-module) :children first :options))

(defn- option-followups [value]
  (->> (document-options) (filter #(= value (:value %))) first :followups))

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
                                   "continuous-residence-passport-attachment"
                                   "permanent-residence-permit"
                                   "permanent-residence-passport-attachment"
                                   "longterm-permit-attachment"
                                   "longterm-passport-attachment"
                                   "brexit-permit-attachment"
                                   "brexit-passport-attachment"
                                   "eu-family-member-permit"
                                   "eu-family-passport-attachment"
                                   "temporary-protection-permit"
                                   "temporary-protection-passport-attachment"
                                   "none-passport-attachment"]]
                (doseq [id keys-to-check]
                  (should-contain id keys-generated))))

          (it "lists the document options in the specified order"
              (should= ["8" "0" "6" "3" "7" "1" "2" "4" "5"]
                       (map :value (document-options))))

          (it "asks only the customer number, the residence permit and the passport for the continuous residence permit"
              (let [followups (option-followups "3")]
                (should= ["p" "textField" "attachment" "attachment"]
                         (map :fieldType followups))
                (should= ["asiakasnumero-migri" "continuous-residence-permit-front" "continuous-residence-passport-attachment"]
                         (map :id (rest followups)))))

          (it "shows the residence permit instructions on permit attachments and the passport instructions on passport attachments"
              (let [attachments (->> (document-options)
                                     (mapcat :followups)
                                     (filter #(= "attachment" (:fieldType %))))
                    info-text   (fn [id] (->> attachments (filter #(= id (:id %))) first :params :info-text :value))]
                (should= 16 (count attachments))
                (doseq [id ["eu-blue-card-attachment" "eu-family-member-permit" "continuous-residence-permit-front"
                            "longterm-permit-attachment" "brexit-permit-attachment" "permanent-residence-permit"
                            "temporary-protection-permit"]]
                  (should= (:residence-permit-attachment-info kk-application-payment-module-texts) (info-text id)))
                (doseq [id ["passport-attachment" "eu-passport-attachment" "eu-family-passport-attachment"
                            "continuous-residence-passport-attachment" "longterm-passport-attachment"
                            "brexit-passport-attachment" "permanent-residence-passport-attachment"
                            "temporary-protection-passport-attachment" "none-passport-attachment"]]
                  (should= (:passport-attachment-info kk-application-payment-module-texts) (info-text id)))
                (should-not-contain "continuous-residence-permit-back" (map :id attachments))))

          (it "handles all the attachments of the module in the payment"
              (let [attachments (->> (document-options)
                                     (mapcat :followups)
                                     (filter #(= "attachment" (:fieldType %))))]
                (doseq [id (map :id attachments)]
                  (should-contain id payment-module/sync-triggering-kk-application-payment-attachment-keys))
                (doseq [id (->> attachments
                                (filter #(= (:residence-permit-attachment-info kk-application-payment-module-texts)
                                            (get-in % [:params :info-text :value])))
                                (map :id))]
                  (should-contain id payment-module/kk-application-payment-exempt-attachment-keys)))))
