(ns ataru.virkailija.application.application-attachment-test
  (:require [cljs.test :refer-macros [deftest is]]
            [clojure.string :refer [includes?]]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.ratom :as r :refer-macros [reaction]]
            [ataru.virkailija.application.attachments.virkailija-attachment-subs :refer [liitepyynnot-for-selected-hakukohteet]]
            [ataru.util :as util]))

(deftest moj-glupi-test
  (let [selected-hakukohde-oids [1 2 3]
        form-fields [2 3 4]
        application ["bla"]
        liitepyynnot-for-hakukohteet [6 7]

        result (liitepyynnot-for-selected-hakukohteet
            [[selected-hakukohde-oids
              form-fields
              application
              liitepyynnot-for-hakukohteet]])]
    (is (= result [5]))))

