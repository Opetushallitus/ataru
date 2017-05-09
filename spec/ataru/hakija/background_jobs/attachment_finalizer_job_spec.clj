(ns ataru.hakija.background-jobs.attachment-finalizer-job-spec
  (:require [ataru.applications.application-store :as application-store]
            [ataru.hakija.background-jobs.attachment-finalizer-job :as job]
            [cheshire.core :as json]
            [org.httpkit.client :as http]
            [ring.util.http-response :as response]
            [speclj.core :refer :all]))

(describe "finalizing attachments"
  (tags :unit)

  (it "should finalize attachments belonging to an application"
    (with-redefs [http/post                         (fn [url params]
                                                      (should= "/api/files/finalize" url)
                                                      (let [body (json/parse-string (:body params) true)]
                                                        (should= {:keys ["attachment-key-1"
                                                                         "attachment-key-2"
                                                                         "attachment-key-3"]} body))
                                                      (future (response/ok)))
                  application-store/get-application (fn [application-id]
                                                      (should= application-id 3)
                                                      {:answers [{:fieldType "attachment"
                                                                  :value     ["attachment-key-1"]}
                                                                 {:fieldType "textField"
                                                                  :value     "lolbal"}
                                                                 {:fieldType "attachment"
                                                                  :value     ["attachment-key-2" "attachment-key-3"]}]})]
      (let [result (job/finalize-attachments {:application-id 3} nil)]
        (should= {:transition {:id :final}} result))))

  (it "should not call finalize API without any attachments"
    (with-redefs [http/post                         (fn [_ _]
                                                      (should-fail))
                  application-store/get-application (fn [application-id]
                                                      (should= application-id 3)
                                                      {:answers [{:fieldType "textField"
                                                                  :value     "lolbal"}]})]
      (let [result (job/finalize-attachments {:application-id 3} nil)]
        (should= {:transition {:id :final}} result)))))
