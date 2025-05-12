(ns ataru.background-job.email-job-spec
  (:require [speclj.core :refer [describe it should-be-nil should-throw should=]]
            [ataru.background-job.email-job :as job])
  (:import (java.util UUID Optional List)
           (fi.oph.viestinvalitys.vastaanotto.model VastaanottajaImpl MaskiImpl)
           (fi.oph.viestinvalitys ViestinvalitysClientImpl ViestinvalitysClientException)
           (fi.oph.viestinvalitys.vastaanotto.resource LuoLahetysSuccessResponseImpl LuoViestiSuccessResponseImpl)))

(def uuid1 (UUID/fromString "de49aa43-0a9e-4e2f-a7bd-29dbdacb99e5"))
(def uuid2 (UUID/fromString "04079761-2294-4279-a9e7-9bb767530bcc"))

(def client-mock (proxy [ViestinvalitysClientImpl] [nil nil nil]
                        (luoViesti [_] (new LuoViestiSuccessResponseImpl uuid1 uuid2))
                        (luoLahetys [_] (new LuoLahetysSuccessResponseImpl uuid2))))

(def throwing-client-mock (proxy [ViestinvalitysClientImpl] [nil nil nil]
                                 (luoViesti [_] (throw (new ViestinvalitysClientException (java.util.Set/of) 403)))
                                 (luoLahetys [_] (new LuoLahetysSuccessResponseImpl uuid2))))

(describe "send-email"
          (it "should return nil when sending is successful"
              (with-redefs [job/viestinvalitys-client (fn [] client-mock)]
                (should-be-nil (job/send-email "from" ["to1" "to2"] "subj" "body" [{:secret "foo" :mask "****"}]))))
          (it "should throw client exception"
              (with-redefs [job/viestinvalitys-client (fn [] throwing-client-mock)]
                (should-throw ViestinvalitysClientException (job/send-email "from" ["to1" "to2"] "subj" "body" [])))))

(describe "vastaanottaja"
          (it "should return a list of recipients"
              (should= (List/of
                         (new VastaanottajaImpl (Optional/empty) (Optional/of "foo@bar.com"))
                         (new VastaanottajaImpl (Optional/empty) (Optional/of "baz@bar.com")))
                       (job/vastaanottajat ["foo@bar.com" "baz@bar.com"]))))

(describe "maskit"
          (it "should return a list of masks"
              (should= (List/of
                         (new MaskiImpl (Optional/of "foo") (Optional/of "***"))
                         (new MaskiImpl (Optional/of "bar") (Optional/of "baz")))
                       (job/maskit [{:secret "foo" :mask "***"}
                                    {:secret "bar" :mask "baz"}]))))
