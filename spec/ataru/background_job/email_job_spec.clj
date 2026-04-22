(ns ataru.background-job.email-job-spec
  (:require [speclj.core :refer [before describe it should-be-nil should-not-be-nil should-throw should=]]
            [ataru.background-job.email-job :as job]
            [ataru.config.url-helper])
  (:import (java.util UUID Optional List Map)
           (fi.oph.viestinvalitys.vastaanotto.model VastaanottajaImpl MaskiImpl KayttooikeusImpl LahettajaImpl)
           (fi.oph.viestinvalitys ViestinvalitysClientImpl ViestinvalitysClientException)
           (fi.oph.viestinvalitys.vastaanotto.resource LuoLahetysSuccessResponseImpl LuoViestiSuccessResponseImpl)))

(def uuid1 (UUID/fromString "de49aa43-0a9e-4e2f-a7bd-29dbdacb99e5"))
(def uuid2 (UUID/fromString "04079761-2294-4279-a9e7-9bb767530bcc"))

(def message (atom nil))
(def batch (atom nil))

(def client-mock (proxy [ViestinvalitysClientImpl] [nil nil nil]
                        (luoViesti [viesti]
                                   (swap! message (fn [_] viesti))
                                   (new LuoViestiSuccessResponseImpl uuid1 uuid2))
                        (luoLahetys [lahetys]
                                    (swap! batch (fn [_] lahetys))
                                    (new LuoLahetysSuccessResponseImpl uuid2))))

(def throwing-client-mock (proxy [ViestinvalitysClientImpl] [nil nil nil]
                                 (luoViesti [_] (throw (new ViestinvalitysClientException (java.util.Set/of) 403)))
                                 (luoLahetys [_] (new LuoLahetysSuccessResponseImpl uuid2))))

(describe "send-email"
          (before
            (reset! message nil)
            (reset! batch nil))

          (it "should return nil when sending is successful"
              (should-be-nil (job/send-email client-mock "no@oph.fi" ["foo@bar.com" "foo@bar.com" "baz@bar.com"] "subj" "body"
                                             [{:secret "foo" :mask "****"}
                                              {:secret "bar" :mask "baz"}]
                                             {:foo ["bar" "baz"]
                                              :x '("y")}
                                             [{:privilege "APP_ATARU_HAKEMUS_CRUD"
                                               :organization "1.2.246.562.10.00000000003"}
                                              {:privilege "FOO"
                                               :organization "1.2.246.562.10.00000000002"}]))

              (should= (Optional/of "subj") (.otsikko @batch))
              (should= (Optional/of "hakemuspalvelu") (.lahettavaPalvelu @batch))
              (should= (Optional/of (new LahettajaImpl (Optional/of "Opetushallitus") (Optional/of "no@oph.fi")))
                       (.lahettaja @batch))
              (should= (Optional/of "normaali") (.prioriteetti @batch))
              (should= (Optional/of (int 1825)) (.sailytysaika @batch))

              (should= (Optional/of "subj") (.otsikko @message))
              (should= (Optional/of "body") (.sisalto @message))
              (should= (Optional/of
                         (List/of
                           (new MaskiImpl (Optional/of "foo") (Optional/of "****"))
                           (new MaskiImpl (Optional/of "bar") (Optional/of "baz"))))
                       (.maskit @message))
              (should= (Optional/of
                         (List/of
                           (new VastaanottajaImpl (Optional/empty) (Optional/of "foo@bar.com"))
                           (new VastaanottajaImpl (Optional/empty) (Optional/of "baz@bar.com"))))
                       (.vastaanottajat @message))
              (should= (Optional/of
                         (Map/of
                           "foo" (List/of "bar" "baz")
                           "x" (List/of "y")))
                       (.metadata @message))
              (should= (Optional/of
                         (List/of
                           (new KayttooikeusImpl (Optional/of "APP_ATARU_HAKEMUS_CRUD") (Optional/of "1.2.246.562.10.00000000003"))
                           (new KayttooikeusImpl (Optional/of "FOO") (Optional/of "1.2.246.562.10.00000000002"))
                           (new KayttooikeusImpl (Optional/of "APP_VIESTINVALITYS_OPH_PAAKAYTTAJA") (Optional/of "1.2.246.562.10.00000000001"))))
                       (.kayttooikeusRajoitukset @message)))

          (it "should throw client exception"
              (should-throw ViestinvalitysClientException (job/send-email throwing-client-mock "from" ["to1" "to2"] "subj" "body"
                                                                          [] {} []))))

(def handler-components {:viestinvalityspalvelu-client client-mock})

(def valid-job-params {:from       "no@oph.fi"
                       :recipients ["foo@bar.com"]
                       :subject    "Test subject"
                       :body       "<p>Test body</p>"
                       :masks      []
                       :metadata   {:foo ["bar"]}
                       :privileges [{:privilege    "APP_ATARU_HAKEMUS_CRUD"
                                     :organization "1.2.246.562.10.00000000003"}]})

(describe "send-email-handler"
          (before
            (reset! message nil)
            (reset! batch nil))

          (it "sends email to valid recipients"
              (with-redefs [ataru.config.url-helper/resolve-url (constantly "http://test")]
                (job/send-email-handler valid-job-params handler-components))
              (should-not-be-nil @message)
              (should= (Optional/of
                         (List/of
                           (new VastaanottajaImpl (Optional/empty) (Optional/of "foo@bar.com"))))
                       (.vastaanottajat @message)))

          (it "skips send when all recipients are blank"
              (with-redefs [ataru.config.url-helper/resolve-url (constantly "http://test")]
                (job/send-email-handler (assoc valid-job-params :recipients ["" "  " nil]) handler-components))
              (should-be-nil @message))

          (it "sends only to valid recipients when list contains blank entries"
              (with-redefs [ataru.config.url-helper/resolve-url (constantly "http://test")]
                (job/send-email-handler (assoc valid-job-params :recipients ["valid@bar.com" "" "also-valid@bar.com"]) handler-components))
              (should-not-be-nil @message)
              (should= (Optional/of
                         (List/of
                           (new VastaanottajaImpl (Optional/empty) (Optional/of "valid@bar.com"))
                           (new VastaanottajaImpl (Optional/empty) (Optional/of "also-valid@bar.com"))))
                       (.vastaanottajat @message)))

          (it "throws when required fields are missing"
              (should-throw Exception (job/send-email-handler (dissoc valid-job-params :subject) handler-components))))
