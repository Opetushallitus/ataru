(ns ataru.unit-runner
  (:require [cljs.test :as test]
            [ataru.application-common.option-visibility-test]
            [ataru.hakija.oppija-session-test]
            [ataru.cljs-util-test]
            [ataru.date-test]
            [ataru.dob-test]
            [ataru.virkailija.editor.handlers-test]
            [ataru.virkailija.autosave-test]
            [ataru.virkailija.temporal-test]
            [ataru.virkailija.application.attachments.liitepyynto-information-request-handlers-test]
            [ataru.virkailija.application.attachments.virkailija-attachment-subs-test]
            [ataru.virkailija.application.virkailija-application-subs-test]
            [ataru.virkailija.kevyt-valinta.virkailija-kevyt-valinta-subs-test]
            [ataru.hakija.application.field-visibility-test]
            [ataru.hakija.application-test]
            [ataru.hakija.application-validators-test]
            [ataru.hakija.rules-test]
            [ataru.hakija.ssn-test]
            [ataru.hakija.banner-test]
            [ataru.component-data.value-transformers-test]
            [ataru.virkailija.kevyt-valinta.virkailija-kevyt-valinta-pseudo-random-valintatapajono-oids-test]
            [ataru.collections-test]
            [ataru.hakija.handlers-util-test]
            [ataru.application-common.hakukohde-specific-questions-test]
            [ataru.application-common.application-field-common-test]
            [ataru.hakija.form-tools-test]
            [ataru.liitteet-test]
            [ataru.virkailija.application.excel-download.excel-utils-test]
            [ataru.hakija.application-hakukohde-util-test])
  (:require-macros [ataru.unit-runner-macros :refer [run-all-cljs-unit-tests]]))

(enable-console-print!)

; bin/run-cljs-unit-tests.mjs odottaa tätä tulosta
(defmethod test/report [::test/default :end-run-tests] [m]
  (set! (.-cljsTestResult js/window)
        #js {:success (test/successful? m)}))

; Ajaa kaikki test/cljs/unit -hakemiston *_test.cljs -nimiavaruudet.
; Kaatuu käännösaikana, jos jokin niistä puuttuu yllä olevista requireista.
(run-all-cljs-unit-tests)
