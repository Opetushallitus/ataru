(ns ataru.hakija.handlers-util-test
  (:require [cljs.test :refer-macros [deftest is]]
            [ataru.hakija.handlers-util :as util]))

(def hakukohde-in-ryhma
  {:oid "hk1" :hakukohderyhmat ["a1"]})

(def hakukohde-in-another-ryhma
  {:oid "hk2" :hakukohderyhmat ["a2"]})

(def per-hakukohde-question
  {:id 1 :per-hakukohde true :belongs-to-hakukohderyhma ["a1"]})

(def question
  {:id 2 :per-hakukohde false :belongs-to-hakukohderyhma ["a1"]})

(def per-hakukohde-question-with-followups
  {:id 3
   :per-hakukohde true
   :belongs-to-hakukohderyhma ["a1"]
   :options [{:followups [{:id "followup-1-id"}
                          {:id "followup-2-id"}]}]})

(defn first-duplicated-question
  [questions]
  (first (filter #(:original-question %) questions)))

(deftest duplicates-questions-for-hakukohde
  (let [duplicated-questions (util/duplicate-questions-for-hakukohde [hakukohde-in-ryhma] "hk1" [] per-hakukohde-question)
        duplicated-question (first-duplicated-question duplicated-questions)]
    (is (= 2 (count duplicated-questions)))
    (is (= 1 (:original-question duplicated-question)))
    (is (nil? (:per-hakukohde duplicated-question)))
    (is (= "hk1" (:duplikoitu-kysymys-hakukohde-oid duplicated-question)))
    (is (= "1_hk1" (:id duplicated-question)))))

(deftest does-not-duplicates-questions-which-do-not-have-hakukohde-in-ryhma
  (let [duplicated-questions (util/duplicate-questions-for-hakukohde [hakukohde-in-another-ryhma] "hk2" [] question)]
    (is (= 1 (count duplicated-questions)))))

(deftest does-not-duplicates-questions-which-do-not-have-per-hakukohde-attribute
  (let [duplicated-questions (util/duplicate-questions-for-hakukohde [hakukohde-in-ryhma] "hk1" [] question)]
    (is (= 1 (count duplicated-questions)))))

(deftest duplicates-child-questions
  (let [duplicated-questions (util/duplicate-questions-for-hakukohde
                               [hakukohde-in-ryhma] "hk1" [] {:children [per-hakukohde-question]})
        children (:children (first duplicated-questions))
        duplicated-question (first-duplicated-question children)]
    (is (= 1 (count duplicated-questions)))
    (is (= 2 (count children)))
    (is (= 1 (:original-question duplicated-question)))
    (is (nil? (:per-hakukohde duplicated-question)))
    (is (= "hk1" (:duplikoitu-kysymys-hakukohde-oid duplicated-question)))
    (is (= "1_hk1" (:id duplicated-question)))))

(deftest does-not-duplicates-child-without-per-hakukohde
  (let [duplicated-questions (util/duplicate-questions-for-hakukohde
                               [hakukohde-in-ryhma] "hk1" [] {:children [question]})
        children (:children (first duplicated-questions))]
    (is (= 1 (count duplicated-questions)))
    (is (= 1 (count children)))
    (is (nil? (:original-question (first children))))
    (is (nil? (:duplikoitu-kysymys-hakukohde-oid (first children))))))

(deftest does-not-duplicates-child-which-do-not-have-hakukohde-in-ryhma
  (let [duplicated-questions (util/duplicate-questions-for-hakukohde
                               [hakukohde-in-another-ryhma] "hk2" [] {:children [per-hakukohde-question]})
        children (:children (first duplicated-questions))]
    (is (= 1 (count duplicated-questions)))
    (is (= 1 (count children)))
    (is (nil? (:original-question (first children))))
    (is (nil? (:duplikoitu-kysymys-hakukohde-oid (first children))))))

(deftest duplicates-followups
  (let [duplicated-questions (util/duplicate-questions-for-hakukohde
                               [hakukohde-in-ryhma] "hk1" [] per-hakukohde-question-with-followups)
        [_original {[{[followup-1 followup-2] :followups}] :options}] duplicated-questions]
    (is (= "followup-1-id_hk1" (:id followup-1)))
    (is (= "followup-2-id_hk1" (:id followup-2)))
    (is (= "hk1" (:duplikoitu-followup-hakukohde-oid followup-1)))
    (is (= "hk1" (:duplikoitu-followup-hakukohde-oid followup-2)))
    (is (= "followup-1-id" (:original-followup followup-1)))
    (is (= "followup-2-id" (:original-followup followup-2)))))

(deftest correctly-duplicates-questions-with-combined-cases
  (let [duplicated-questions (util/duplicate-questions-for-hakukohteet-during-form-load
                               [hakukohde-in-ryhma hakukohde-in-another-ryhma] ["hk1" "hk2"]
                               [question per-hakukohde-question {:children [{:id 3} {:id 4 :per-hakukohde true :belongs-to-hakukohderyhma ["a2"]}]} per-hakukohde-question-with-followups])
        [_ _ duplicated-question {[_ original-child duplicated-child :as children] :children} _ {[{followups :followups}] :options}] duplicated-questions]
    (is (= 6 (count duplicated-questions)))
    (is (= 3 (count children)))
    (is (= 1 (:original-question duplicated-question)))
    (is (nil? (:per-hakukohde duplicated-question)))
    (is (= "hk1" (:duplikoitu-kysymys-hakukohde-oid duplicated-question)))
    (is (= "1_hk1" (:id duplicated-question)))
    (is (true? (:created-during-form-load duplicated-question)))
    (is (= 4 (:original-question duplicated-child)))
    (is (nil? (:per-hakukohde duplicated-child)))
    (is (= "hk2" (:duplikoitu-kysymys-hakukohde-oid duplicated-child)))
    (is (= "4_hk2" (:id duplicated-child)))
    (is (:per-hakukohde original-child))
    (is (nil? (:original-question original-child)))
    (is (nil? (::duplikoitu-kysymys-hakukohde-oid original-child)))
    (is (= 2 (count followups)))
    (is (= "followup-1-id_hk1" (:id (first followups))))
    (is (= "followup-2-id_hk1" (:id (second followups))))
    (is (= "hk1" (:duplikoitu-followup-hakukohde-oid (first followups))))
    (is (= "hk1" (:duplikoitu-followup-hakukohde-oid (second followups))))))

(deftest fill-missing-answer-for-hakukohde
  (let [answers {:q1 {:value ""}}
        questions [{:id "q1"} {:id "q2" :original-question "q1"}]
        result (util/fill-missing-answer-for-hakukohde answers questions)]
    (is (= {:q1 {:value ""} :q2 {:value "" :valid true}} result))))

(deftest copied-answer-should-have-valid-false-when-question-is-required
  (let [answers {:q1 {:value ""}}
        questions [{:id "q1"} {:id "q2" :original-question "q1" :validators ["required"]}]
        result (util/fill-missing-answer-for-hakukohde answers questions)]
    (is (= false (:valid (:q2 result))))))

(def test-flat-form
  [{:params {:size "M"},
    :rules {},
    :validators ["required"],
    :fieldClass "formField",
    :cannot-edit true,
    :label {:fi "Sukunimi", :sv "Efternamn", :en "Surname/Family name"},
    :id "last-name",
    :cannot-view false,
    :metadata {:created-by {:name "system", :oid "system", :date "1970-01-01T00:00:00Z"}}}
   {:label {:fi "Kysymysryhmä", :sv ""},
    :fieldClass "questionGroup",
    :id "04bf89e0-2fec-4f7a-941c-40c91f8f593a",
    :params {}, :metadata {:created-by {:name "Virkailija", :oid "1.2.246.562.24.76008520040", :date "2024-03-27T11:19:15Z"},
                           :modified-by {:name "Virkailija", :oid "1.2.246.562.24.76008520040", :date "2024-03-27T11:19:37Z"}},
    :fieldType "fieldset"}
   {:children-of "04bf89e0-2fec-4f7a-941c-40c91f8f593a",
    :params {:repeatable true, :question-group-id :04bf89e0-2fec-4f7a-941c-40c91f8f593a},
    :fieldClass "formField",
    :cannot-edit false,
    :label {:fi "Vapaamuotoinen vastaus", :sv ""},
    :id "2c97597f-2e52-43b0-a0a2-b8b022e572af",
    :cannot-view false,
    :metadata {:created-by {:name "Virkailija", :oid "1.2.246.562.24.76008520040", :date "2024-03-27T11:19:42Z"},
               :modified-by {:name "Virkailija", :oid "1.2.246.562.24.76008520040", :date "2024-03-27T11:19:47Z"}},
    :fieldType "textField"}
   {:label {:fi "Infoteksti"},
    :text {:fi "Tässä infotekstiä"},
    :fieldClass "infoElement",
    :id "efeb883a-8dfd-4933-b1c1-5751b7147eda",
    :params {:question-group-id :04bf89e0-2fec-4f7a-941c-40c91f8f593a},
    :metadata {:created-by {:name "Virkailija", :oid "1.2.246.562.24.76008520040", :date "2024-03-27T11:21:45Z"},
               :modified-by {:name "Virkailija", :oid "1.2.246.562.24.76008520040", :date "2024-03-27T11:21:49Z"}},
    :fieldType "p", :children-of "04bf89e0-2fec-4f7a-941c-40c91f8f593a"}
   {:children-of "04bf89e0-2fec-4f7a-941c-40c91f8f593a",
    :params {:max-value "2024", :numeric true, :min-value "1900", :question-group-id :04bf89e0-2fec-4f7a-941c-40c91f8f593a},
    :validators ["numeric"],
    :fieldClass "formField",
    :cannot-edit false,
    :label {:fi "Vapaamuotoinen vastaus 2", :sv ""}, :id "51207053-6674-47d1-b88e-c0f8ab5cee92",
    :cannot-view false,
    :options [{:label {:fi "", :sv ""}, :value "0", :condition {:comparison-operator "<", :answer-compared-to 2020}}],
    :metadata {:created-by {:name "Virkailija", :oid "1.2.246.562.24.76008520040", :date "2024-03-27T11:25:56Z"},
               :modified-by {:name "Virkailija", :oid "1.2.246.562.24.76008520040", :date "2024-03-27T11:26:27Z"}},
    :fieldType "textField"}
   {:params {:question-group-id :04bf89e0-2fec-4f7a-941c-40c91f8f593a},
    :option-value "0", :fieldClass "formField", :cannot-edit false,
    :label {:fi "Lisäkysymys kun yli 2020", :sv ""},
    :id "ce039866-a75c-4641-b444-0218e7421ad0",
    :cannot-view false,
    :followup-of "51207053-6674-47d1-b88e-c0f8ab5cee92",
    :metadata {:created-by {:name "Virkailija", :oid "1.2.246.562.24.76008520040", :date "2024-03-27T11:26:36Z"},
               :modified-by {:name "Virkailija", :oid "1.2.246.562.24.76008520040", :date "2024-03-27T11:26:40Z"}},
    :fieldType "textField"}])

(defn test-answers
  [add-remaining-answer]
  (let [answers [{:duplikoitu-followup-hakukohde-oid nil,
                  :key "last-name", :value "Henkilö",
                  :duplikoitu-kysymys-hakukohde-oid nil,
                  :original-question nil,
                  :fieldType "textField",
                  :original-followup nil}
                 {:duplikoitu-followup-hakukohde-oid nil,
                  :key "2c97597f-2e52-43b0-a0a2-b8b022e572af",
                  :value [["Vastaan tähän jotain" "Tähän vielä toinen kohta"] ["Tähänkin vastaus"]],
                  :duplikoitu-kysymys-hakukohde-oid nil,
                  :original-question nil,
                  :fieldType "textField",
                  :original-followup nil}
                 {:duplikoitu-followup-hakukohde-oid nil,
                  :key "51207053-6674-47d1-b88e-c0f8ab5cee92",
                  :value [["2000"] ["2023"]],
                  :duplikoitu-kysymys-hakukohde-oid nil,
                  :original-question nil,
                  :fieldType "textField",
                  :original-followup nil}]]
    (if add-remaining-answer
      (conj answers {:duplikoitu-followup-hakukohde-oid nil,
                     :key "ce039866-a75c-4641-b444-0218e7421ad0",
                     :value [["foo"] ["bar"]],
                     :duplikoitu-kysymys-hakukohde-oid nil,
                     :original-question nil,
                     :fieldType "textField",
                     :original-followup nil})
      answers)))

(defn test-app-db-answers
  [add-remaining-answer]
  (let [answers {:last-name
                 {:valid true, :label {:fi "Sukunimi", :sv "Efternamn", :en "Surname/Family name"},
                  :value "Henkilö", :values {:value "Henkilö", :valid true}, :original-value "Henkilö"}
                 :51207053-6674-47d1-b88e-c0f8ab5cee92
                 {:valid true, :label {:fi "Vapaamuotoinen vastaus 2", :sv ""}, :value [["2000"] ["2023"]],
                  :values [[{:valid true, :value "2000"}] [{:valid true, :value "2023"}]], :original-value [["2000"] ["2023"]]},
                 :2c97597f-2e52-43b0-a0a2-b8b022e572af
                 {:valid true, :label {:fi "Vapaamuotoinen vastaus", :sv ""}, :value [["Vastaan tähän jotain" "Tähän vielä toinen kohta"] ["Tähänkin vastaus"]],
                  :values [[{:valid true, :value "Vastaan tähän jotain"} {:valid true, :value "Tähän vielä toinen kohta"}] [{:valid true, :value "Tähänkin vastaus"}]],
                  :original-value [["Vastaan tähän jotain" "Tähän vielä toinen kohta"] ["Tähänkin vastaus"]]}
                 :ce039866-a75c-4641-b444-0218e7421ad0
                 {:valid true, :label {:fi "Lisäkysymys kun yli 2020", :sv ""},
                  :value [[""]],
                  :values [[{:valid false, :value ""}]],
                  :original-value [["Vastaus"] nil]}}]
    (if add-remaining-answer
      (merge answers {:ce039866-a75c-4641-b444-0218e7421ad0
                      {:valid true, :label {:fi "Lisäkysymys kun yli 2020", :sv ""},
                       :value [["foo"] ["bar"]],
                       :values [[{:valid true, :value "foo"}] [{:valid true, :value "bar"}]],
                       :original-value [["Vastaus"] nil]}})
      answers)))

(deftest reinitializes-values-without-changes-when-all-answers-present
  (let [answers (test-answers true)
        db-answers (test-app-db-answers true)
        pre-db {:application {:answers db-answers}}
        post-db (util/reinitialize-question-group-empty-answers pre-db answers test-flat-form)]
    (is (= pre-db post-db))))

(deftest reinitializes-values-with-question-group-nil-padding-on-missing-answers
  (let [answers (test-answers false)
        db-answers (test-app-db-answers false)
        pre-db {:application {:answers db-answers}}
        post-db (util/reinitialize-question-group-empty-answers pre-db answers test-flat-form)
        post-db-answers (get-in post-db [:application :answers])]
    (is (not (= pre-db post-db)))
    (is (= (get-in post-db-answers [:ce039866-a75c-4641-b444-0218e7421ad0 :value]) [[""] nil]))
    (is (= (get-in post-db-answers [:ce039866-a75c-4641-b444-0218e7421ad0 :values]) [[{:valid false, :value ""}] nil]))))
