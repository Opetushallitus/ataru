  (ns ataru.attachment-deadline.attachment-deadline-disabled-spec
    (:require [ataru.attachment-deadline.attachment-deadline-protocol :as deadline]
              [ataru.attachment-deadline.attachment-deadline-service :as service]
              [ataru.config.core :as config]
              [ataru.hakija.hakija-form-service :as form-service]
              [ataru.kk-application-payment.kk-application-payment :as payment]
              [ataru.ohjausparametrit.ohjausparametrit-protocol :refer [OhjausparametritService]]
              [ataru.tarjonta-service.hakukohde :as hakukohde]
              [ataru.time :as t]
              [ataru.time.coerce :as c]
              [speclj.core :refer [describe it tags should= should-not=]]))

(def start (t/date-time 2026 1 1))
(def end (t/date-time 2026 1 2))
(def now (t/date-time 2026 2 1))
(def hakuaika {:start (c/to-long start)
               :end (c/to-long end)
               :on false
               :jatkuva-or-joustava-haku? true})
(def hakuajat {:uniques #{hakuaika} :by-oid {}})
(def attachment {:id "liite" :fieldClass "formField" :fieldType "attachment"})

(defn- deadline-service [parametrit]
  (service/->AttachmentDeadlineService
   (reify OhjausparametritService
     (get-parametri [_ _] parametrit))))

(defn- attachment-deadline [service haku]
  (with-redefs [config/config {:public-config {:attachment-modify-grace-period-days 14}}]
    (deadline/attachment-deadline-for-hakuaika service start haku hakuaika)))

(defn- cannot-edit? [service haku field & [processing? field-deadlines]]
  (:cannot-edit
   (form-service/flag-uneditable-and-unviewable-field
    service now hakuajat [:hakija] (boolean processing?) field-deadlines
    false false start haku field)))

(describe "OY-5822: attachment editing without a deadline"
          (tags :unit)

          (for [hakutapa ["hakutapa_03" "hakutapa_03#1" "hakutapa_04" "hakutapa_04#1"]]
            (it (str "allows attachment editing after the default deadline: " hakutapa)
                (let [service (deadline-service {:liitteidenMuokkausIlmanTakarajaa true})
                      haku {:oid "test-haku" :hakutapa-uri hakutapa}]
                  (should= true (deadline/attachment-deadline-disabled? service haku))
                  (should= nil (attachment-deadline service haku))
                  (should= false (cannot-edit? service haku attachment))
                  (should= true (cannot-edit? service haku
                                              {:id "other" :fieldClass "formField" :fieldType "textField"}))
                  (should= true (cannot-edit? service haku attachment true))
                  (should= true (cannot-edit? service haku attachment false
                                              {"liite" {:deadline end}}))
                  (should= true (cannot-edit? service haku
                                              (assoc attachment :params {:deadline "2.1.2026 15:00"})))
                  (should= nil
                           (get-in (hakukohde/populate-attachment-deadlines
                                    {:content [attachment]} now hakuajat {} service start haku)
                                   [:content 0 :params :deadline-label])))))

          (for [hakutapa ["hakutapa_01#1" "hakutapa_02#1" "hakutapa_05#1" "hakutapa_06#1"]]
            (it (str "preserves the default deadline for other application types: " hakutapa)
                (let [service (deadline-service {:liitteidenMuokkausIlmanTakarajaa true})
                      haku {:oid "test-haku" :hakutapa-uri hakutapa}]
                  (should= false (deadline/attachment-deadline-disabled? service haku))
                  (should= (c/to-long (t/plus end (t/days 14)))
                           (c/to-long (attachment-deadline service haku))))))

          (it "does not treat unavailable parameters as an explicit unlimited deadline"
              (let [service (deadline-service nil)
                    haku {:oid "test-haku" :hakutapa-uri "hakutapa_03"}]
                (should= false (deadline/attachment-deadline-disabled? service haku))
                (should= (c/to-long (t/plus end (t/days 14)))
                         (c/to-long (attachment-deadline service haku)))))

          (for [parametrit [{:liitteidenMuokkauksenHakemuskohtainenTakarajaKaytossa false}
                              {:PH_LMT {:value 0}}
                              {:PH_LMT {:value 7}}
                              {:liitteidenMuokkauksenHakemuskohtainenTakarajaKaytossa true
                               :liitteidenMuokkauksenHakemuskohtainenTakarajaPaivaa 7}]]
            (it (str "preserves explicitly configured deadlines: " parametrit)
                (let [service (deadline-service parametrit)
                      haku {:oid "test-haku" :hakutapa-uri "hakutapa_04"}]
                  (should= false (deadline/attachment-deadline-disabled? service haku))
                  (should-not= nil (attachment-deadline service haku))
                  (should= true (cannot-edit? service haku attachment)))))

          (for [hakutapa ["hakutapa_03#1" "hakutapa_04#1"]
                value [true "true"]]
            (it (str "uses the explicit flag even with a legacy deadline: " hakutapa " " value)
                (let [service (deadline-service {:liitteidenMuokkausIlmanTakarajaa value
                                                :PH_LMT {:value 7}})
                      haku {:oid "test-haku" :hakutapa-uri hakutapa}]
                  (should= true (deadline/attachment-deadline-disabled? service haku))
                  (should= nil (attachment-deadline service haku)))))

          (for [hakutapa ["hakutapa_03" "hakutapa_04"]
                parametrit [nil
                           {}
                           {:liitteidenMuokkausIlmanTakarajaa false}
                           {:liitteidenMuokkausIlmanTakarajaa "false"}]]
            (it (str "preserves the default without an enabled flag: " hakutapa " " parametrit)
                (let [service (deadline-service parametrit)
                      haku {:oid "test-haku" :hakutapa-uri hakutapa}]
                  (should= false (deadline/attachment-deadline-disabled? service haku))
                  (should= (c/to-long (t/plus end (t/days 14)))
                           (c/to-long (attachment-deadline service haku))))))

          (it "does not consider a disabled deadline passed in application payment processing"
              (let [service (deadline-service {:liitteidenMuokkausIlmanTakarajaa true})
                    haku {:oid "test-haku"
                          :hakutapa-uri "hakutapa_04"
                          :hakuajat [{:end (c/to-long end)}]}]
                (should= true
                         (#'payment/time-is-before-some-attachment-deadlines?
                          service start haku now)))))
