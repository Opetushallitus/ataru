(ns ataru.selection-limit.selection-limit-spec
  (:require
   [speclj.core :refer [describe it tags should should= should-throw should-not-throw]]
   [ataru.forms.form-store :as forms]
   [ataru.selection-limit.selection-limit-service :as selection-limit])
  (:import [java.util UUID]))

(def question-id "question-id")

(defn form-with-fields
  [& limits]
  (let [id (str (UUID/randomUUID))]
  {:id      id
   :content [{:id      question-id
              :params  {:selection-group-id id}
              :options (map-indexed (fn [i limit]
                                        {:value (str i) :selection-limit limit}) limits)}]}))

(describe
 "selection limit"
 (tags :unit :selection-limit)
  (it "works with empty data (nothing stored)"
      (let [form (form-with-fields 5 0)]
        (with-redefs [forms/fetch-by-key (constantly form)]
          (let [available (selection-limit/query-available-selections (:id form))]
            (should= {:limit-reached [{:question-id question-id :answer-id "1"}]} available)))))
  (it "doesn't allow booking when limit set to zero"
      (let [form (form-with-fields 0)
            id   (:id form)]
        (with-redefs [forms/fetch-by-key (constantly form)]
          (should-throw clojure.lang.ExceptionInfo
            (selection-limit/new-selection id question-id "0" id)))))
  (it "allows permabooking"
      (let [form (form-with-fields 1)
            id   (:id form)]
        (with-redefs [forms/fetch-by-key (constantly form)]
          (let [application-key (str (UUID/randomUUID))
                {selection-id  :selection-id
                 limit-reached :limit-reached} (selection-limit/new-selection id question-id "0" id)
                application     {:answers [{:key   question-id
                                            :id    question-id
                                            :value "0"}]}]
            (should-not-throw
              (selection-limit/permanent-select-on-store-application application-key application selection-id form))))))
  (it "allows permabooking swab between selections"
      (let [form (form-with-fields 1 1)
            id   (:id form)]
        (with-redefs [forms/fetch-by-key (constantly form)]
          (let [application-key (str (UUID/randomUUID))
                {selection-id  :selection-id
                 limit-reached :limit-reached} (selection-limit/new-selection id question-id "0" id)
                application     {:answers [{:key   question-id
                                            :id    question-id
                                            :value "0"}]}]
            (should-not-throw
              (selection-limit/permanent-select-on-store-application application-key application selection-id form))
            (let [{selection-id  :selection-id
                   limit-reached :limit-reached} (selection-limit/new-selection id question-id "1" id)
                  application     {:answers [{:key   question-id
                                              :id    question-id
                                              :value "1"}]}]
              (should-not-throw
                (selection-limit/permanent-select-on-store-application application-key application selection-id form)))))))
  (it "disallows permabooking swab to fully booked selection"
      (let [form (form-with-fields 1 0)
            id   (:id form)]
        (with-redefs [forms/fetch-by-key (constantly form)]
          (let [application-key (str (UUID/randomUUID))
                {selection-id  :selection-id
                 limit-reached :limit-reached} (selection-limit/new-selection id question-id "0" id)
                application     {:answers [{:key   question-id
                                            :id    question-id
                                            :value "0"}]}]
            (should-not-throw
              (selection-limit/permanent-select-on-store-application application-key application selection-id form))
            (let [application     {:answers [{:key   question-id
                                              :id    question-id
                                              :value "1"}]}]
              (should-throw
                (selection-limit/permanent-select-on-store-application application-key application selection-id form))
              (let [{limit-reached :limit-reached} (selection-limit/query-available-selections (:id form))]
                ; asserts that previously booked is still booked
                (should= [{:question-id question-id :answer-id "0"}
                          {:question-id question-id :answer-id "1"}] limit-reached)))))))
  (it "allows booking and permabooking same option multiple times (saving muokkaus fails without this)"
      (let [form (form-with-fields 1)
            id   (:id form)]
        (with-redefs [forms/fetch-by-key (constantly form)]
          (let [application-key (str (UUID/randomUUID))
                {selection-id  :selection-id
                 limit-reached :limit-reached} (selection-limit/new-selection id question-id "0" id)
                {limit-reached-outsider :limit-reached} (selection-limit/query-available-selections id)
                {limit-after-swab :limit-reached} (selection-limit/swab-selection id selection-id question-id "0" id)
                {limit-reached-outsider-after-swab :limit-reached} (selection-limit/query-available-selections id)
                application     {:answers [{:key   question-id
                                            :id    question-id
                                            :value "0"}]}]
            (should= [] limit-reached)
            (should= [{:question-id question-id :answer-id "0"}] limit-reached-outsider)
            (should= [{:question-id question-id :answer-id "0"}] limit-reached-outsider-after-swab)
            (should= [] limit-after-swab)
            (should-not-throw
              (selection-limit/permanent-select-on-store-application application-key application selection-id form))
            (should-not-throw
              (selection-limit/permanent-select-on-store-application application-key application selection-id form))
            (let [{limit-reached :limit-reached} (selection-limit/query-available-selections (:id form))]
              ; asserts that booked is still booked
              (should= [{:question-id question-id :answer-id "0"}] limit-reached))))))
  (it "doesn't allow overbooking"
      (let [form (form-with-fields 1)
            id   (:id form)]
        (with-redefs [forms/fetch-by-key (constantly form)]
          (let [{limit-reached :limit-reached}  (selection-limit/new-selection id question-id "0" id)
                {limit-reached-outsider :limit-reached} (selection-limit/query-available-selections id)]
            (should= [] limit-reached)
            (should= [{:question-id question-id :answer-id "0"}] limit-reached-outsider)
            (should-throw clojure.lang.ExceptionInfo
              (selection-limit/new-selection id question-id "0" id))))))
  (it "unlimited selection when limit set to nil"
      (let [form (form-with-fields nil)
            id   (:id form)]
        (with-redefs [forms/fetch-by-key (constantly form)]
          (let [limits (mapcat :limit-reached [(selection-limit/query-available-selections id)
                                               (selection-limit/new-selection id question-id "0" id)
                                               (selection-limit/new-selection id question-id "0" id)])]
            (should= [] limits))))))

