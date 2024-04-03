(ns ataru.unit-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [ataru.application-common.option-visibility-test]
            [ataru.hakija.oppija-session-test]
            [ataru.cljs-util-test]
            [ataru.dob-test]
            [ataru.virkailija.editor.handlers-test]
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
            [ataru.hakija.form-tools-test]
            [ataru.liitteet-test]
            [ataru.virkailija.application.excel-download.excel-utils-test]))

(doo-tests 'ataru.application-common.option-visibility-test
           'ataru.hakija.oppija-session-test
           'ataru.cljs-util-test
           'ataru.dob-test
           'ataru.virkailija.editor.handlers-test
           'ataru.virkailija.application.attachments.virkailija-attachment-subs-test
           'ataru.virkailija.application.virkailija-application-subs-test
           'ataru.virkailija.kevyt-valinta.virkailija-kevyt-valinta-subs-test
           'ataru.hakija.application.field-visibility-test
           'ataru.hakija.application-test
           'ataru.hakija.application-validators-test
           'ataru.hakija.rules-test
           'ataru.hakija.ssn-test
           'ataru.hakija.banner-test
           'ataru.component-data.value-transformers-test
           'ataru.virkailija.kevyt-valinta.virkailija-kevyt-valinta-pseudo-random-valintatapajono-oids-test
           'ataru.collections-test
           'ataru.hakija.handlers-util-test
           'ataru.application-common.hakukohde-specific-questions-test
           'ataru.hakija.form-tools-test
           'ataru.liitteet-test
           'ataru.virkailija.application.excel-download.excel-utils-test)
