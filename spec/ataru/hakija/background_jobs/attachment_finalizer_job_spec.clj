(ns ataru.hakija.background-jobs.attachment-finalizer-job-spec
  (:require [ataru.applications.application-store :as application-store]
            [ataru.hakija.background-jobs.attachment-finalizer-job :as job]
            [ataru.cas.client :as cas]
            [ring.util.http-response :as response]
            [speclj.core :refer [describe tags it should= should-fail]]))

(describe "finalizing attachments"
  (tags :unit)

  (it "should finalize attachments belonging to an application"
    (with-redefs [cas/cas-authenticated-post        (fn [_ url body _]
                                                      (should= "/api/files/finalize" url)
                                                      (should= {:keys ["attachment-key-1"
                                                                       "attachment-key-2"
                                                                       "attachment-key-3"]} body)
                                                      (response/ok))
                  application-store/get-application (fn [application-id]
                                                      (should= application-id 3)
                                                      {:answers [{:fieldType "attachment"
                                                                  :value     ["attachment-key-1"]}
                                                                 {:fieldType "textField"
                                                                  :value     "lolbal"}
                                                                 {:fieldType "attachment"
                                                                  :value     ["attachment-key-2" "attachment-key-3"]}]})]
      (let [result (job/finalize-attachments {:application-id 3} nil)]
        (should= nil result))))

  (it "should not call finalize API without any attachments"
    (with-redefs [cas/cas-authenticated-post        (fn [_ _ _]
                                                      (should-fail))
                  application-store/get-application (fn [application-id]
                                                      (should= application-id 3)
                                                      {:answers [{:fieldType "textField"
                                                                  :value     "lolbal"}]})]
      (let [result (job/finalize-attachments {:application-id 3} nil)]
        (should= nil result)))))
