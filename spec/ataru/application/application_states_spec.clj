(ns ataru.application.application-states-spec
  (:require [ataru.application.application-states :as application-states]
            [speclj.core :refer [describe it should= tags]]))

(def ^:private hakukohde "1.2.246.562.20.00000000001")

(defn- review [attachment-key state]
  {:attachment-key attachment-key :state state :hakukohde hakukohde})

(defn- states-for-filtering [application]
  (->> (application-states/attachment-reviews-for-filtering application)
       (map (juxt :attachment-key :state))
       set))

(describe "attachment reviews for filtering"
  (tags :unit)

  (it "ignores the missing attachments of the document option when the residence permit check decides the payment obligation"
    (should= #{["other-attachment" "attachment-missing"]
               ["continuous-residence-permit-back" "checked"]}
             (states-for-filtering
               {:hakukohde                                        [hakukohde]
                :kk-payment-permit-found-in-migri-attachment-keys ["continuous-residence-permit-front"]
                :application-attachment-reviews                   [(review "continuous-residence-permit-front" "attachment-missing")
                                                                   (review "continuous-residence-permit-back" "checked")
                                                                   (review "continuous-residence-passport-attachment" "attachment-missing")
                                                                   (review "other-attachment" "attachment-missing")]})))

  (it "keeps the other states of the attachments of the document option"
    (should= #{["continuous-residence-permit-front" "incomplete-attachment"]}
             (states-for-filtering
               {:hakukohde                                        [hakukohde]
                :kk-payment-permit-found-in-migri-attachment-keys ["continuous-residence-permit-front"]
                :application-attachment-reviews                   [(review "continuous-residence-permit-front" "incomplete-attachment")]})))

  (it "keeps the missing attachments of another document option"
    (should= #{["eu-passport-attachment" "attachment-missing"]}
             (states-for-filtering
               {:hakukohde                                        [hakukohde]
                :kk-payment-permit-found-in-migri-attachment-keys ["continuous-residence-permit-front"]
                :application-attachment-reviews                   [(review "eu-passport-attachment" "attachment-missing")]})))

  (it "keeps the missing attachments when the residence permit check does not decide the payment obligation"
    (should= #{["continuous-residence-permit-front" "attachment-missing"]}
             (states-for-filtering
               {:hakukohde                      [hakukohde]
                :application-attachment-reviews [(review "continuous-residence-permit-front" "attachment-missing")]})))

  (it "has no attachment requirements in a hakukohde whose only attachment reviews are ignored"
    (should= [{:hakukohde hakukohde :state "no-requirements"}]
             (application-states/attachment-reviews-for-filtering
               {:hakukohde                                        [hakukohde]
                :kk-payment-permit-found-in-migri-attachment-keys ["permanent-residence-permit"]
                :application-attachment-reviews                   [(review "permanent-residence-permit" "attachment-missing")
                                                                   (review "permanent-residence-passport-attachment" "attachment-missing")]}))))
