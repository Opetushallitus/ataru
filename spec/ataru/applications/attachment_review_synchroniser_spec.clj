(ns ataru.applications.attachment-review-synchroniser-spec
  (:require [ataru.applications.attachment-review-synchroniser :as attachment-review-synchroniser]
            [ataru.applications.application-store :as application-store]
            [speclj.core :refer [describe tags it should-not should== should=]]))

(describe "save-attachment-hakukohde-reviews"
  (tags :unit)

  (it "syncs a changed payment module attachment review to other hakukohde the attachment applies to, even though the request also carries other hakukohde's unchanged values"
    (let [application-key "app-1"
          saved (atom [])]
      (with-redefs [application-store/get-application-attachment-reviews
                    (constantly [{:hakukohde "hk-1" :attachment-key "passport-attachment" :state "not-checked"}
                                 {:hakukohde "hk-2" :attachment-key "passport-attachment" :state "not-checked"}])
                    application-store/save-attachment-hakukohde-review
                    (fn [_ hakukohde attachment-key state _ _]
                      (swap! saved conj [hakukohde attachment-key state]))]
        (should=
          true
          (attachment-review-synchroniser/save-attachment-hakukohde-reviews
           application-key
           {:hk-1 {:passport-attachment "checked"}
            :hk-2 {:passport-attachment "not-checked"}}
           nil nil))
        (should== #{["hk-1" "passport-attachment" "checked"]
                    ["hk-2" "passport-attachment" "checked"]}
                  (set @saved)))))

  (it "syncs a changed passport-attachment variant key (e.g. eu-passport-attachment) the same way as the base passport-attachment key"
    (let [application-key "app-1"
          saved (atom [])]
      (with-redefs [application-store/get-application-attachment-reviews
                    (constantly [{:hakukohde "hk-1" :attachment-key "eu-passport-attachment" :state "not-checked"}
                                 {:hakukohde "hk-2" :attachment-key "eu-passport-attachment" :state "not-checked"}])
                    application-store/save-attachment-hakukohde-review
                    (fn [_ hakukohde attachment-key state _ _]
                      (swap! saved conj [hakukohde attachment-key state]))]
        (should=
          true
          (attachment-review-synchroniser/save-attachment-hakukohde-reviews
           application-key
           {:hk-1 {:eu-passport-attachment "checked"}
            :hk-2 {:eu-passport-attachment "not-checked"}}
           nil nil))
        (should== #{["hk-1" "eu-passport-attachment" "checked"]
                    ["hk-2" "eu-passport-attachment" "checked"]}
                  (set @saved)))))

  (it "does not sync a non-payment-module attachment review to other hakukohde"
    (let [application-key "app-1"
          saved (atom [])]
      (with-redefs [application-store/get-application-attachment-reviews
                    (constantly [{:hakukohde "hk-1" :attachment-key "other-attachment" :state "not-checked"}
                                 {:hakukohde "hk-2" :attachment-key "other-attachment" :state "not-checked"}])
                    application-store/save-attachment-hakukohde-review
                    (fn [_ hakukohde attachment-key state _ _]
                      (swap! saved conj [hakukohde attachment-key state]))]
        (should=
          false
          (attachment-review-synchroniser/save-attachment-hakukohde-reviews
           application-key
           {:hk-1 {:other-attachment "checked"}
            :hk-2 {:other-attachment "not-checked"}}
           nil nil))
        ; hk-2:n arvo on muuttumaton verrattuna jo tallennettuun, joten sitä ei pidä
        ; tallentaa (uudelleen) - application-store/save-attachment-hakukohde-review avaa
        ; oman tietokantatransaktionsa jokaista kutsua kohden, joten muuttumattomien tilojen
        ; ohittaminen välttää turhat edestakaiset tietokantakutsut
        (should== [["hk-1" "other-attachment" "checked"]] @saved))))

  (it "only syncs to hakukohde the payment module attachment actually applies to, not every hakukohde in the application"
    (let [application-key "app-1"
          saved (atom [])]
      (with-redefs [application-store/get-application-attachment-reviews
                    (constantly [{:hakukohde "hk-1" :attachment-key "passport-attachment" :state "not-checked"}
                                 {:hakukohde "hk-2" :attachment-key "passport-attachment" :state "not-checked"}
                                 {:hakukohde "hk-3" :attachment-key "other-attachment" :state "not-checked"}])
                    application-store/save-attachment-hakukohde-review
                    (fn [_ hakukohde attachment-key state _ _]
                      (swap! saved conj [hakukohde attachment-key state]))]
        ; hk-3 ei ole niiden hakukohteiden joukossa joita liitepyyntö koskee (sillä ei ole
        ; olemassa olevaa tarkastusmerkintäriviä passport-attachmentille), joten sen ei pidä
        ; saada periytynyttä arvoa vaikka se onkin osa samaa hakemusta
        (should=
          true
          (attachment-review-synchroniser/save-attachment-hakukohde-reviews
           application-key
           {:hk-1 {:passport-attachment "checked"}
            :hk-2 {:passport-attachment "not-checked"}}
           nil nil))
        (should== #{["hk-1" "passport-attachment" "checked"]
                    ["hk-2" "passport-attachment" "checked"]}
                  (set @saved))
        (should-not (some #(= "hk-3" (first %)) @saved)))))

  (it "does not sync when the payment module attachment review value is unchanged"
    (let [application-key "app-1"
          saved (atom [])]
      (with-redefs [application-store/get-application-attachment-reviews
                    (constantly [{:hakukohde "hk-1" :attachment-key "passport-attachment" :state "checked"}
                                 {:hakukohde "hk-2" :attachment-key "passport-attachment" :state "checked"}])
                    application-store/save-attachment-hakukohde-review
                    (fn [_ hakukohde attachment-key state _ _]
                      (swap! saved conj [hakukohde attachment-key state]))]
        (should=
          false
          (attachment-review-synchroniser/save-attachment-hakukohde-reviews
           application-key
           {:hk-1 {:passport-attachment "checked"}
            :hk-2 {:passport-attachment "checked"}}
           nil nil))
        (should== [] @saved))))

  (it "does not call save-attachment-hakukohde-review at all when nothing in the request differs from the stored state"
    (let [application-key "app-1"
          save-call-count (atom 0)]
      (with-redefs [application-store/get-application-attachment-reviews
                    (constantly [{:hakukohde "hk-1" :attachment-key "passport-attachment" :state "checked"}
                                 {:hakukohde "hk-2" :attachment-key "passport-attachment" :state "not-checked"}
                                 {:hakukohde "hk-3" :attachment-key "other-attachment" :state "not-checked"}])
                    application-store/save-attachment-hakukohde-review
                    (fn [_ _ _ _ _ _] (swap! save-call-count inc))]
        (should=
          false
          (attachment-review-synchroniser/save-attachment-hakukohde-reviews
           application-key
           {:hk-1 {:passport-attachment "checked"}
            :hk-2 {:passport-attachment "not-checked"}
            :hk-3 {:other-attachment "not-checked"}}
           nil nil))
        (should= 0 @save-call-count)))))
