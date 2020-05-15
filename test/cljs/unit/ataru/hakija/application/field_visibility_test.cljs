(ns ataru.hakija.application.field-visibility-test
  (:require [clojure.test :refer [deftest is testing]]
            [ataru.hakija.application.field-visibility :as field-visibility]))

(deftest set-field-visibility-for-plain-field-test
  (is (= {:application {:ui {:kysymys {:visible? true}}}}
         (field-visibility/set-field-visibility {} {:id "kysymys"}))))

(deftest set-field-visibility-for-options-test
  (testing "single option:"
    (is (= {:application {:answers {:kysymys {:value "0"}}
                          :ui      {:kysymys {0         {:visible? true}
                                              :visible? true}}}}
           (field-visibility/set-field-visibility {:application {:answers {:kysymys {:value "0"}}}}
                                                  {:id        "kysymys"
                                                   :fieldType "singleChoice"
                                                   :options   [{:value "0"}]}))))
  (testing "multiple options:"
    (is (= {:application {:answers {:kysymys {:value "1"}}
                          :ui      {:kysymys {0         {:visible? true}
                                              1         {:visible? true}
                                              :visible? true}}}}
           (field-visibility/set-field-visibility {:application {:answers {:kysymys {:value "1"}}}}
                                                  {:id        "kysymys"
                                                   :fieldType "singleChoice"
                                                   :options   [{:value "0"}
                                                               {:value "1"}]})))))

(deftest set-field-visibility-for-option-followups-test
  (testing "single option, option selected, single followup:"
    (is (= {:application {:answers {:kysymys {:value "0"}}
                          :ui      {:kysymys      {0         {:visible? true}
                                                   :visible? true}
                                    :jatkokysymys {:visible? true}}}}
           (field-visibility/set-field-visibility {:application {:answers {:kysymys {:value "0"}}}}
                                                  {:id        "kysymys"
                                                   :fieldType "singleChoice"
                                                   :options   [{:value     "0"
                                                                :followups [{:id "jatkokysymys"}]}]}))))
  (testing "multiple options, option not selected, single followup:"
    (is (= {:application {:answers {:kysymys {:value "0"}}
                          :ui      {:kysymys      {0         {:visible? true}
                                                   1         {:visible? true}
                                                   :visible? true}
                                    :jatkokysymys {:visible? false}}}}
           (field-visibility/set-field-visibility {:application {:answers {:kysymys {:value "0"}}}}
                                                  {:id        "kysymys"
                                                   :fieldType "singleChoice"
                                                   :options   [{:value "0"}
                                                               {:value     "1"
                                                                :followups [{:id "jatkokysymys"}]}]}))))
  (testing "single option, option selected, multiple followups:"
    (is (= {:application {:answers {:kysymys {:value "0"}}
                          :ui      {:kysymys   {0         {:visible? true}
                                                :visible? true}
                                    :jatkokysymys-1 {:visible? true}
                                    :jatkokysymys-2 {:visible? true}}}}
           (field-visibility/set-field-visibility {:application {:answers {:kysymys {:value "0"}}}}
                                                  {:id        "kysymys"
                                                   :fieldType "singleChoice"
                                                   :options   [{:value     "0"
                                                                :followups [{:id "jatkokysymys-1"}
                                                                            {:id "jatkokysymys-2"}]}]})))))
