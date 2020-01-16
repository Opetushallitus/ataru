(ns ataru.virkailija.application.application-attachment-test
  (:require [cljs.test :refer-macros [deftest is]]
            [clojure.string :refer [includes?]]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.ratom :as r :refer-macros [reaction]]
            [ataru.virkailija.application.attachments.virkailija-attachment-subs :refer [liitepyynnot-for-selected-hakukohteet]]
            [ataru.util :as util]))

(deftest returns-attachments-in-correct-order
  (let [selected-hakukohde-oids ["hakukohde_oid"]
        form-fields {:attachment3 {:label "label3"}
                     :attachment2 {:label "label2"}
                     :attachment1 {:label "label1"}}
        application {:answers
                     {:attachment1 {:key "attachment1" :values "value1"}
                      :attachment3 {:key "attachment3" :values "value3"}
                      :attachment2 {:key "attachment2" :values "value2"}}}
        liitepyynnot-for-hakukohteet {:hakukohde_oid {"attachment3" "checked"
                                                      "attachment2" "checked"
                                                      "attachment1" "checked"}}
        expected-value  [{:key :attachment1,
                          :state "checked",
                          :values "value1",
                          :label "label1",
                          :hakukohde-oid "hakukohde_oid"}
                         {:key :attachment2,
                          :state "checked",
                          :values "value2",
                          :label "label2",
                          :hakukohde-oid "hakukohde_oid"}
                         {:key :attachment3,
                          :state "checked",
                          :values "value3",
                          :label "label3",
                          :hakukohde-oid "hakukohde_oid"}]

        result (liitepyynnot-for-selected-hakukohteet
            [selected-hakukohde-oids
             form-fields
             application
             liitepyynnot-for-hakukohteet] 0)]
    (is (= result expected-value))))

