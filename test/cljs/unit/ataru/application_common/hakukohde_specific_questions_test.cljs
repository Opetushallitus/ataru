(ns ataru.application-common.hakukohde-specific-questions-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [ataru.application-common.hakukohde-specific-questions :as hsq]))

(def question
  {:options
   [{:followups
     [{:id "followup-1-id"}
      {:id "followup-2-id"}]}]})

(deftest change-followups-for-question-test
  (testing "change-followups-for-question"
    (let [{[{[followup-1 followup-2] :followups}] :options} (hsq/change-followups-for-question question "hakukohde-oid")]
      (is (= "followup-1-id_hakukohde-oid" (:id followup-1)))
      (is (= "followup-2-id_hakukohde-oid" (:id followup-2))))))
