(ns ataru.application.application-states-spec
  (:require [ataru.application.application-states :as application-states]
            [ataru.component-data.kk-application-payment-module :as payment-module]
            [speclj.core :refer [describe it should should-not should= tags]]))

(def ^:private hakukohde "1.2.246.562.20.00000000001")

(defn- review [attachment-key state]
  {:attachment-key attachment-key :state state :hakukohde hakukohde})

(def ^:private reviewed-obligation
  [{:requirement "kk-application-payment-obligation" :state "reviewed" :hakukohde hakukohde}])

(defn- deciding-checks [attachment-key]
  [{:attachment-key attachment-key :found-in-migri true :payment-obligation-basis "not-obligated"}])

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
               {:hakukohde                              [hakukohde]
                :kk-payment-permit-checks-for-filtering (deciding-checks "continuous-residence-permit-front")
                :application-hakukohde-reviews          reviewed-obligation
                :application-attachment-reviews         [(review "continuous-residence-permit-front" "attachment-missing")
                                                         (review "continuous-residence-permit-back" "checked")
                                                         (review "continuous-residence-passport-attachment" "attachment-missing")
                                                         (review "other-attachment" "attachment-missing")]})))

  (it "keeps the other states of the attachments of the document option"
    (should= #{["continuous-residence-permit-front" "incomplete-attachment"]}
             (states-for-filtering
               {:hakukohde                              [hakukohde]
                :kk-payment-permit-checks-for-filtering (deciding-checks "continuous-residence-permit-front")
                :application-hakukohde-reviews          reviewed-obligation
                :application-attachment-reviews         [(review "continuous-residence-permit-front" "incomplete-attachment")]})))

  (it "keeps the missing attachments of another document option"
    (should= #{["eu-passport-attachment" "attachment-missing"]}
             (states-for-filtering
               {:hakukohde                              [hakukohde]
                :kk-payment-permit-checks-for-filtering (deciding-checks "continuous-residence-permit-front")
                :application-hakukohde-reviews          reviewed-obligation
                :application-attachment-reviews         [(review "eu-passport-attachment" "attachment-missing")]})))

  (it "keeps the missing attachments when the residence permit check does not decide the payment obligation"
    (should= #{["continuous-residence-permit-front" "attachment-missing"]}
             (states-for-filtering
               {:hakukohde                              [hakukohde]
                :application-attachment-reviews         [(review "continuous-residence-permit-front" "attachment-missing")]})))

  (it "has no attachment requirements in a hakukohde whose only attachment reviews are ignored"
    (should= [{:hakukohde hakukohde :state "no-requirements"}]
             (application-states/attachment-reviews-for-filtering
               {:hakukohde                              [hakukohde]
                :kk-payment-permit-checks-for-filtering (deciding-checks "permanent-residence-permit")
                :application-hakukohde-reviews          reviewed-obligation
                :application-attachment-reviews         [(review "permanent-residence-permit" "attachment-missing")
                                                         (review "permanent-residence-passport-attachment" "attachment-missing")]}))))

(describe "permit check deciding the payment obligation"
  (tags :unit)

  (it "decides the payment obligation when found in Migri, the basis is set and the obligation is reviewed"
    (doseq [basis ["not-obligated" "obligated-own-studies"]]
      (should (payment-module/permit-check-decides-payment-obligation?
                {:found-in-migri true :payment-obligation-basis basis}
                reviewed-obligation))))

  (it "does not decide the payment obligation otherwise"
    (should-not (payment-module/permit-check-decides-payment-obligation?
                  {:found-in-migri false :payment-obligation-basis "not-obligated"} reviewed-obligation))
    (should-not (payment-module/permit-check-decides-payment-obligation?
                  {:found-in-migri nil :payment-obligation-basis "not-obligated"} reviewed-obligation))
    (should-not (payment-module/permit-check-decides-payment-obligation?
                  {:found-in-migri true :payment-obligation-basis nil} reviewed-obligation))
    (should-not (payment-module/permit-check-decides-payment-obligation?
                  {:found-in-migri true :payment-obligation-basis "not-obligated"}
                  [{:requirement "kk-application-payment-obligation" :state "unreviewed" :hakukohde hakukohde}
                   {:requirement "processing-state" :state "reviewed" :hakukohde hakukohde}]))
    (should-not (payment-module/permit-check-decides-payment-obligation? nil reviewed-obligation))))

(def ^:private filter-application
  {:hakukohde                              [hakukohde "1.2.246.562.20.00000000002"]
   :kk-payment-permit-checks-for-filtering (deciding-checks "continuous-residence-permit-front")
   :application-hakukohde-reviews          reviewed-obligation
   :application-attachment-reviews         [(review "continuous-residence-permit-front" "attachment-missing")
                                            (review "other-attachment" "attachment-missing")
                                            {:attachment-key "other-attachment"
                                             :state          "checked"
                                             :hakukohde      "1.2.246.562.20.00000000002"}]})

(describe "attachment review states filter"
  (tags :unit)

  (it "does not match an attachment whose only matching reviews are ignored"
    (should-not (application-states/attachment-review-states-filter-matches?
                  filter-application "continuous-residence-permit-front" ["attachment-missing"] [])))

  (it "matches the other attachments by state and hakukohde"
    (should (application-states/attachment-review-states-filter-matches?
              filter-application "other-attachment" ["attachment-missing"] []))
    (should (application-states/attachment-review-states-filter-matches?
              filter-application "other-attachment" [] [hakukohde]))
    (should-not (application-states/attachment-review-states-filter-matches?
                  filter-application "other-attachment" ["attachment-missing"] ["1.2.246.562.20.00000000002"])))

  (it "matches the missing attachment when the permit check does not decide the payment obligation"
    (should (application-states/attachment-review-states-filter-matches?
              (dissoc filter-application :application-hakukohde-reviews)
              "continuous-residence-permit-front"
              ["attachment-missing"]
              []))))
