(ns ataru.applications.excel-export-spec
  (:require [ataru.fixtures.excel-fixtures :as fixtures]
            [ataru.forms.form-store :as form-store]
            [ataru.test-utils :refer [export-test-excel with-excel-workbook]]
            [speclj.core :refer [around describe it should should-be-nil
                                 should= tags]]))

(defn- verify-row
  [sheet row-num expected-values]
  (let [row (.getRow sheet row-num)]
    (if (nil? expected-values)
      (should-be-nil row)
      (should= expected-values
               (map (fn [col] (some-> (.getCell row col)
                                      .getStringCellValue))
                    (range (.getLastCellNum row)))))))

(defn- transpose [m] (apply mapv vector m))

(defn- verify-cols
  [sheet expected-col-values]
  (doall (map-indexed (fn [row-index expected-row]
                        (verify-row sheet row-index expected-row))
                      (transpose expected-col-values))))

(defn- verify-pane-information
  [sheet]
  (let [info (.getPaneInformation sheet)]
    (should (.isFreezePane info))
    (should= 1 (.getHorizontalSplitPosition info))
    (should= 0 (.getVerticalSplitPosition info))
    (should= 1 (.getHorizontalSplitTopRow info))
    (should= 0 (.getVerticalSplitLeftColumn info))))

(describe
 "excel export"
 (tags :unit :excel)
 (around [spec]
         (with-redefs [form-store/fetch-by-id (fn [id]
                                                (case id
                                                  123 fixtures/form
                                                  321 fixtures/form-for-hakukohde))
                       form-store/fetch-by-key (fn [key]
                                                 (case key
                                                   "form_123_key" fixtures/form
                                                   "form_321_key" fixtures/form-for-hakukohde))]
           (spec)))

 (it "should export applications for a form without hakukohde or haku"
     (with-excel-workbook
       (export-test-excel [fixtures/application-for-form] {:skip-answers? false
                                                           :ids-only? false})
       (fn [workbook]
         (let [metadata-sheet    (.getSheetAt workbook 0)
               application-sheet (.getSheetAt workbook 1)]
           (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
           (verify-row metadata-sheet 1 ["Form name" "123" "form_123_key" "2016-06-14 15:34:56" "SEPPO PAPUNEN"])
           (verify-row metadata-sheet 2 nil)
           (verify-cols application-sheet [["Hakemusnumero" "application_9432_key"]
                                           ["Hakemuksen tallennusaika" "2016-06-15 15:30:00"]
                                           ["Hakemuksen viimeisimmän muokkauksen aika" "2016-06-15 15:34:56"]
                                           ["Hakemuksen tila" "Aktiivinen"]
                                           ["Hakukohteen käsittelyn tila" "Käsittelemättä"]
                                           ["Kielitaitovaatimus" "Tarkastamatta"]
                                           ["Tutkinnon kelpoisuus" "Täyttyy"]
                                           ["Hakukelpoisuus" "Ei hakukelpoinen"]
                                           ["Hakukelpoisuus asetettu automaattisesti" nil]
                                           ["Hylkäyksen syy" nil]
                                           ["Lukuvuosimaksuvelvollisuus" "Tarkastamatta"]
                                           ["Valinnan tila" "Kesken"]
                                           ["Ehdollinen" "ei"]
                                           ["Pisteet" "12"]
                                           ["Oppijanumero" nil]
                                           ["Hakijan henkilö-OID" nil]
                                           ["Turvakielto" "ei"]
                                           ["Muistiinpanot" nil]
                                           ["Hakemusmaksuvelvollisuus" "Tarkastamatta"]
                                           ["Hakemusmaksun tila" nil]
                                           ["Kysymys 1" "Vastaus 1"]
                                           ["Kysymys 2" "Vastaus 2"]
                                           ["Etunimi" "Lomake-etunimi"]
                                           ["Kysymys 3" "Vastaus 3"]])
           (verify-pane-information application-sheet)))))

 (it "should export applications for a hakukohde with haku"
     (with-excel-workbook
       (export-test-excel [fixtures/application-for-hakukohde] {:skip-answers? false
                                                                :ids-only? false})
       (fn [workbook]
         (let [metadata-sheet    (.getSheetAt workbook 0)
               application-sheet (.getSheetAt workbook 1)]
           (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
           (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
           (verify-row metadata-sheet 2 nil)
           (verify-cols application-sheet [["Hakemusnumero" "application_3424_key"]
                                           ["Hakemuksen tallennusaika" "2016-06-15 15:30:00"]
                                           ["Hakemuksen viimeisimmän muokkauksen aika" "2016-06-15 15:34:56"]
                                           ["Hakemuksen tila" "Aktiivinen"]
                                           ["Hakukohteen käsittelyn tila" "Käsittelyssä"]
                                           ["Kielitaitovaatimus" "Tarkastamatta"]
                                           ["Tutkinnon kelpoisuus" "Tarkastamatta"]
                                           ["Hakukelpoisuus" "Ei hakukelpoinen"]
                                           ["Hakukelpoisuus asetettu automaattisesti" nil]
                                           ["Hylkäyksen syy" "2018-07-30 18:12:14 Tarkastaja Järvinen: Ei käy"]
                                           ["Lukuvuosimaksuvelvollisuus" "Tarkastamatta"]
                                           ["Valinnan tila" "Hyväksytty"]
                                           ["Ehdollinen" "ei"]
                                           ["Pisteet" "12"]
                                           ["Oppijanumero" nil]
                                           ["Hakijan henkilö-OID" "1.123.345456567123"]
                                           ["Turvakielto" "kyllä"]
                                           ["Muistiinpanot" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari"]
                                           ["Hakemusmaksuvelvollisuus" "Tarkastamatta"]
                                           ["Hakemusmaksun tila" "Maksettu"]
                                           ["Kysymys 4" "Vastaus 4"]
                                           ["Etunimi" "Person-etunimi"]
                                           ["Kysymys 5" "Vastaus 5"]
                                           ["Hakukohteet" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)"]])))))

 (it "should export applications for a hakukohde with haku without uneligible reasons"
     (with-excel-workbook
       (export-test-excel [(assoc fixtures/application-for-hakukohde
                                  :application-hakukohde-reviews
                                  [{:requirement "selection-state" :state "selected" :hakukohde "hakukohde.oid"}
                                   {:requirement "processing-state" :state "processing" :hakukohde "hakukohde.oid"}
                                   {:requirement "eligibility-state" :state "eligible" :hakukohde "hakukohde.oid"}])]
                          {:skip-answers? false
                           :ids-only? false})
       (fn [workbook]
         (let [metadata-sheet    (.getSheetAt workbook 0)
               application-sheet (.getSheetAt workbook 1)]
           (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
           (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
           (verify-row metadata-sheet 2 nil)
           (verify-cols application-sheet [["Hakemusnumero" "application_3424_key"]
                                           ["Hakemuksen tallennusaika" "2016-06-15 15:30:00"]
                                           ["Hakemuksen viimeisimmän muokkauksen aika" "2016-06-15 15:34:56"]
                                           ["Hakemuksen tila" "Aktiivinen"]
                                           ["Hakukohteen käsittelyn tila" "Käsittelyssä"]
                                           ["Kielitaitovaatimus" "Tarkastamatta"]
                                           ["Tutkinnon kelpoisuus" "Tarkastamatta"]
                                           ["Hakukelpoisuus" "Hakukelpoinen"]
                                           ["Hakukelpoisuus asetettu automaattisesti" nil]
                                           ["Hylkäyksen syy" nil]
                                           ["Lukuvuosimaksuvelvollisuus" "Tarkastamatta"]
                                           ["Valinnan tila" "Hyväksytty"]
                                           ["Ehdollinen" "ei"]
                                           ["Pisteet" "12"]
                                           ["Oppijanumero" nil]
                                           ["Hakijan henkilö-OID" "1.123.345456567123"]
                                           ["Turvakielto" "kyllä"]
                                           ["Muistiinpanot" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari"]
                                           ["Hakemusmaksuvelvollisuus" "Tarkastamatta"]
                                           ["Hakemusmaksun tila" "Maksettu"]
                                           ["Kysymys 4" "Vastaus 4"]
                                           ["Etunimi" "Person-etunimi"]
                                           ["Kysymys 5" "Vastaus 5"]
                                           ["Hakukohteet" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)"]])))))

 (it "should export applications to separate sheets, grouped by form"
     (with-excel-workbook
       (export-test-excel [fixtures/application-for-form fixtures/application-for-hakukohde]
                          {:skip-answers? false
                           :ids-only? false})
       (fn [workbook]
         (let [metadata-sheet              (.getSheetAt workbook 0)
               form-application-sheet      (.getSheetAt workbook 1)
               hakukohde-application-sheet (.getSheetAt workbook 2)]
           (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
           (verify-row metadata-sheet 1 ["Form name" "123" "form_123_key" "2016-06-14 15:34:56" "SEPPO PAPUNEN"])
           (verify-row metadata-sheet 2 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
           (verify-row metadata-sheet 3 nil)
           (verify-cols form-application-sheet [["Hakemusnumero" "application_9432_key"]
                                                ["Hakemuksen tallennusaika" "2016-06-15 15:30:00"]
                                                ["Hakemuksen viimeisimmän muokkauksen aika" "2016-06-15 15:34:56"]
                                                ["Hakemuksen tila" "Aktiivinen"]
                                                ["Hakukohteen käsittelyn tila" "Käsittelemättä"]
                                                ["Kielitaitovaatimus" "Tarkastamatta"]
                                                ["Tutkinnon kelpoisuus" "Täyttyy"]
                                                ["Hakukelpoisuus" "Ei hakukelpoinen"]
                                                ["Hakukelpoisuus asetettu automaattisesti" nil]
                                                ["Hylkäyksen syy" nil]
                                                ["Lukuvuosimaksuvelvollisuus" "Tarkastamatta"]
                                                ["Valinnan tila" "Kesken"]
                                                ["Ehdollinen" "ei"]
                                                ["Pisteet" "12"]
                                                ["Oppijanumero" nil]
                                                ["Hakijan henkilö-OID" nil]
                                                ["Turvakielto" "ei"]
                                                ["Muistiinpanot" nil]
                                                ["Hakemusmaksuvelvollisuus" "Tarkastamatta"]
                                                ["Hakemusmaksun tila" nil]
                                                ["Kysymys 1" "Vastaus 1"]
                                                ["Kysymys 2" "Vastaus 2"]
                                                ["Etunimi" "Lomake-etunimi"]
                                                ["Kysymys 3" "Vastaus 3"]])
           (verify-cols hakukohde-application-sheet [["Hakemusnumero" "application_3424_key"]
                                                     ["Hakemuksen tallennusaika" "2016-06-15 15:30:00"]
                                                     ["Hakemuksen viimeisimmän muokkauksen aika" "2016-06-15 15:34:56"]
                                                     ["Hakemuksen tila" "Aktiivinen"]
                                                     ["Hakukohteen käsittelyn tila" "Käsittelyssä"]
                                                     ["Kielitaitovaatimus" "Tarkastamatta"]
                                                     ["Tutkinnon kelpoisuus" "Tarkastamatta"]
                                                     ["Hakukelpoisuus" "Ei hakukelpoinen"]
                                                     ["Hakukelpoisuus asetettu automaattisesti" nil]
                                                     ["Hylkäyksen syy" "2018-07-30 18:12:14 Tarkastaja Järvinen: Ei käy"]
                                                     ["Lukuvuosimaksuvelvollisuus" "Tarkastamatta"]
                                                     ["Valinnan tila" "Hyväksytty"]
                                                     ["Ehdollinen" "ei"]
                                                     ["Pisteet" "12"]
                                                     ["Oppijanumero" nil]
                                                     ["Hakijan henkilö-OID" "1.123.345456567123"]
                                                     ["Turvakielto" "kyllä"]
                                                     ["Muistiinpanot" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari"]
                                                     ["Hakemusmaksuvelvollisuus" "Tarkastamatta"]
                                                     ["Hakemusmaksun tila" "Maksettu"]
                                                     ["Kysymys 4" "Vastaus 4"]
                                                     ["Etunimi" "Person-etunimi"]
                                                     ["Kysymys 5" "Vastaus 5"]
                                                     ["Hakukohteet" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)"]])))))

 (it "should always export answers to special questions"
     (with-redefs [form-store/fetch-by-id  (fn [_] fixtures/form-with-special-questions)
                   form-store/fetch-by-key (fn [_] fixtures/form-with-special-questions)]
       (with-excel-workbook
         (export-test-excel [fixtures/application-with-special-answers]
                            {:skip-answers? true
                             :ids-only? false})
         (fn [workbook]
           (let [metadata-sheet    (.getSheetAt workbook 0)
                 application-sheet (.getSheetAt workbook 1)]
             (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
             (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
             (verify-row metadata-sheet 2 nil)
             (verify-cols application-sheet [["Hakemusnumero" "application_3424_key"]
                                             ["Hakemuksen tallennusaika" "2016-06-15 15:30:00"]
                                             ["Hakemuksen viimeisimmän muokkauksen aika" "2016-06-15 15:34:56"]
                                             ["Hakemuksen tila" "Aktiivinen"]
                                             ["Hakukohteen käsittelyn tila" "Käsittelyssä"]
                                             ["Kielitaitovaatimus" "Tarkastamatta"]
                                             ["Tutkinnon kelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus asetettu automaattisesti" nil]
                                             ["Hylkäyksen syy" nil]
                                             ["Lukuvuosimaksuvelvollisuus" "Tarkastamatta"]
                                             ["Valinnan tila" "Hyväksytty"]
                                             ["Ehdollinen" "ei"]
                                             ["Pisteet" "12"]
                                             ["Oppijanumero" nil]
                                             ["Hakijan henkilö-OID" "1.123.345456567123"]
                                             ["Turvakielto" "kyllä"]
                                             ["Muistiinpanot" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari"]
                                             ["Hakemusmaksuvelvollisuus" "Migrin tarkastuksessa"]
                                             ["Hakemusmaksun tila" "Maksettu toisessa haussa"]
                                             ["Etunimi" "Person-etunimi"]
                                             ["Hakukohteet" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)"]]))))))

 (it "should not include Kysymys 4 which does not belong to selected-hakukohde"
     (with-redefs [form-store/fetch-by-id  (fn [_] fixtures/form-for-multiple-hakukohde)
                   form-store/fetch-by-key (fn [_] fixtures/form-for-multiple-hakukohde)]
       (with-excel-workbook
         (export-test-excel [fixtures/application-with-special-answers]
                            {:skip-answers? false
                             :selected-hakukohde "hakukohde.oid"
                             :ids-only? false})
         (fn [workbook]
           (let [metadata-sheet    (.getSheetAt workbook 0)
                 application-sheet (.getSheetAt workbook 1)]
             (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
             (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
             (verify-row metadata-sheet 2 nil)
             (verify-cols application-sheet [["Hakemusnumero" "application_3424_key"]
                                             ["Hakemuksen tallennusaika" "2016-06-15 15:30:00"]
                                             ["Hakemuksen viimeisimmän muokkauksen aika" "2016-06-15 15:34:56"]
                                             ["Hakemuksen tila" "Aktiivinen"]
                                             ["Hakukohteen käsittelyn tila" "Käsittelyssä"]
                                             ["Kielitaitovaatimus" "Tarkastamatta"]
                                             ["Tutkinnon kelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus asetettu automaattisesti" nil]
                                             ["Hylkäyksen syy" nil]
                                             ["Lukuvuosimaksuvelvollisuus" "Tarkastamatta"]
                                             ["Valinnan tila" "Hyväksytty"]
                                             ["Ehdollinen" "ei"]
                                             ["Pisteet" "12"]
                                             ["Oppijanumero" nil]
                                             ["Hakijan henkilö-OID" "1.123.345456567123"]
                                             ["Turvakielto" "kyllä"]
                                             ["Muistiinpanot" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari"]
                                             ["Hakemusmaksuvelvollisuus" "Migrin tarkastuksessa"]
                                             ["Hakemusmaksun tila" "Maksettu toisessa haussa"]
                                             ["Visible from form" nil]
                                             ["Should be visible because belongs-to-hakukohde is not specified" nil]
                                             ["Etunimi" "Person-etunimi"]
                                             ["Kysymys 5" "Vastaus 5 will be included only when skip-answers? == false"]
                                             ["Jos tulen hyväksytyksi, oppilaitos voi julkaista nimeni omilla verkkosivuillaan." "Ei"]
                                             ["Hakukohteet" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)"]]))))))

 (it "should include questions when hakukohde belongs to hakukohderyhma"
     (with-redefs [form-store/fetch-by-id  (fn [_] fixtures/form-for-multiple-hakukohde)
                   form-store/fetch-by-key (fn [_] fixtures/form-for-multiple-hakukohde)]
       (with-excel-workbook
         (export-test-excel [fixtures/application-with-special-answers-2]
                            {:skip-answers? false
                             :selected-hakukohde "hakukohde-in-ryhma.oid"
                             :ids-only? false})
         (fn [workbook]
           (let [metadata-sheet    (.getSheetAt workbook 0)
                 application-sheet (.getSheetAt workbook 1)]
             (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
             (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
             (verify-row metadata-sheet 2 nil)
             (verify-cols application-sheet [["Hakemusnumero" "application_3424_key"]
                                             ["Hakemuksen tallennusaika" "2016-06-15 15:30:00"]
                                             ["Hakemuksen viimeisimmän muokkauksen aika" "2016-06-15 15:34:56"]
                                             ["Hakemuksen tila" "Aktiivinen"]
                                             ["Hakukohteen käsittelyn tila" "Käsittelyssä"]
                                             ["Kielitaitovaatimus" "Tarkastamatta"]
                                             ["Tutkinnon kelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus asetettu automaattisesti" nil]
                                             ["Hylkäyksen syy" nil]
                                             ["Lukuvuosimaksuvelvollisuus" "Tarkastamatta"]
                                             ["Valinnan tila" "Hyväksytty"]
                                             ["Ehdollinen" "ei"]
                                             ["Pisteet" "12"]
                                             ["Oppijanumero" nil]
                                             ["Hakijan henkilö-OID" "1.123.345456567123"]
                                             ["Turvakielto" "kyllä"]
                                             ["Muistiinpanot" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari"]
                                             ["Hakemusmaksuvelvollisuus" "Tarkastamatta"]
                                             ["Hakemusmaksun tila" "Maksettu toisessa haussa"]
                                             ["Visible from form 2" nil]
                                             ["Visible only if belongs to hakukohderyhmä1" nil]
                                             ["Visible because of parent's hakukohderyhmä" nil]
                                             ["Should be visible because belongs-to-hakukohde is not specified" nil]
                                             ["Etunimi" "Person-etunimi"]
                                             ["Kysymys 5" "Vastaus 5 will be included only when skip-answers? == false"]
                                             ["Jos tulen hyväksytyksi, oppilaitos voi julkaista nimeni omilla verkkosivuillaan." "Ei"]
                                             ["Hakukohteet" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde-in-ryhma.oid)"]]))))))

 (it "should not include questions belonging to hakukohderyhma"
     (with-redefs [form-store/fetch-by-id  (fn [_] fixtures/form-for-multiple-hakukohde)
                   form-store/fetch-by-key (fn [_] fixtures/form-for-multiple-hakukohde)]
       (with-excel-workbook
         (export-test-excel [fixtures/application-with-special-answers-2]
                            {:skip-answers? false
                             :selected-hakukohderyhma "1.2.246.562.28.00000000001"
                             :ids-only? false})
         (fn [workbook]
           (let [metadata-sheet    (.getSheetAt workbook 0)
                 application-sheet (.getSheetAt workbook 1)]
             (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
             (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
             (verify-row metadata-sheet 2 nil)
             (verify-cols application-sheet [["Hakemusnumero" "application_3424_key"]
                                             ["Hakemuksen tallennusaika" "2016-06-15 15:30:00"]
                                             ["Hakemuksen viimeisimmän muokkauksen aika" "2016-06-15 15:34:56"]
                                             ["Hakemuksen tila" "Aktiivinen"]
                                             ["Hakukohteen käsittelyn tila" "Käsittelyssä"]
                                             ["Kielitaitovaatimus" "Tarkastamatta"]
                                             ["Tutkinnon kelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus asetettu automaattisesti" nil]
                                             ["Hylkäyksen syy" nil]
                                             ["Lukuvuosimaksuvelvollisuus" "Tarkastamatta"]
                                             ["Valinnan tila" "Hyväksytty"]
                                             ["Ehdollinen" "ei"]
                                             ["Pisteet" "12"]
                                             ["Oppijanumero" nil]
                                             ["Hakijan henkilö-OID" "1.123.345456567123"]
                                             ["Turvakielto" "kyllä"]
                                             ["Muistiinpanot" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari"]
                                             ["Hakemusmaksuvelvollisuus" "Tarkastamatta"]
                                             ["Hakemusmaksun tila" "Maksettu toisessa haussa"]
                                             ["Visible from form 2" nil]
                                             ["Visible only if belongs to hakukohderyhmä1" nil]
                                             ["Visible because of parent's hakukohderyhmä" nil]
                                             ["Should be visible because belongs-to-hakukohde is not specified" nil]
                                             ["Etunimi" "Person-etunimi"]
                                             ["Kysymys 5" "Vastaus 5 will be included only when skip-answers? == false"]
                                             ["Jos tulen hyväksytyksi, oppilaitos voi julkaista nimeni omilla verkkosivuillaan." "Ei"]
                                             ["Hakukohteet" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde-in-ryhma.oid)"]]))))))

 (it "should not include questions for different hakukohderyhma"
     (with-redefs [form-store/fetch-by-id  (fn [_] fixtures/form-for-multiple-hakukohde)
                   form-store/fetch-by-key (fn [_] fixtures/form-for-multiple-hakukohde)]
       (with-excel-workbook
         (export-test-excel [fixtures/application-with-special-answers]
                            {:skip-answers? false
                             :selected-hakukohderyhma "unknown-hakukohderyhma"
                             :ids-only? false})
         (fn [workbook]
           (let [metadata-sheet    (.getSheetAt workbook 0)
                 application-sheet (.getSheetAt workbook 1)]
             (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
             (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
             (verify-row metadata-sheet 2 nil)
             (verify-cols application-sheet [["Hakemusnumero" "application_3424_key"]
                                             ["Hakemuksen tallennusaika" "2016-06-15 15:30:00"]
                                             ["Hakemuksen viimeisimmän muokkauksen aika" "2016-06-15 15:34:56"]
                                             ["Hakemuksen tila" "Aktiivinen"]
                                             ["Hakukohteen käsittelyn tila" "Käsittelyssä"]
                                             ["Kielitaitovaatimus" "Tarkastamatta"]
                                             ["Tutkinnon kelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus asetettu automaattisesti" nil]
                                             ["Hylkäyksen syy" nil]
                                             ["Lukuvuosimaksuvelvollisuus" "Tarkastamatta"]
                                             ["Valinnan tila" "Hyväksytty"]
                                             ["Ehdollinen" "ei"]
                                             ["Pisteet" "12"]
                                             ["Oppijanumero" nil]
                                             ["Hakijan henkilö-OID" "1.123.345456567123"]
                                             ["Turvakielto" "kyllä"]
                                             ["Muistiinpanot" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari"]
                                             ["Hakemusmaksuvelvollisuus" "Migrin tarkastuksessa"]
                                             ["Hakemusmaksun tila" "Maksettu toisessa haussa"]
                                             ["Should be visible because belongs-to-hakukohde is not specified" nil]
                                             ["Etunimi" "Person-etunimi"]
                                             ["Kysymys 5" "Vastaus 5 will be included only when skip-answers? == false"]
                                             ["Jos tulen hyväksytyksi, oppilaitos voi julkaista nimeni omilla verkkosivuillaan." "Ei"]
                                             ["Hakukohteet" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)"]]))))))

 (it "should not include Kysymys 4 when not including everything"
     (with-redefs [form-store/fetch-by-id  (fn [_] fixtures/form-with-special-questions)
                   form-store/fetch-by-key (fn [_] fixtures/form-with-special-questions)]
       (with-excel-workbook
         (export-test-excel [fixtures/application-with-special-answers]
                            {:skip-answers? true
                             :included-ids #{"joku-kysymys-vaan"}
                             :ids-only? false})
         (fn [workbook]
           (let [metadata-sheet    (.getSheetAt workbook 0)
                 application-sheet (.getSheetAt workbook 1)]
             (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
             (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
             (verify-row metadata-sheet 2 nil)
             (verify-cols application-sheet [["Hakemusnumero" "application_3424_key"]
                                             ["Hakemuksen tallennusaika" "2016-06-15 15:30:00"]
                                             ["Hakemuksen viimeisimmän muokkauksen aika" "2016-06-15 15:34:56"]
                                             ["Hakemuksen tila" "Aktiivinen"]
                                             ["Hakukohteen käsittelyn tila" "Käsittelyssä"]
                                             ["Kielitaitovaatimus" "Tarkastamatta"]
                                             ["Tutkinnon kelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus asetettu automaattisesti" nil]
                                             ["Hylkäyksen syy" nil]
                                             ["Lukuvuosimaksuvelvollisuus" "Tarkastamatta"]
                                             ["Valinnan tila" "Hyväksytty"]
                                             ["Ehdollinen" "ei"]
                                             ["Pisteet" "12"]
                                             ["Oppijanumero" nil]
                                             ["Hakijan henkilö-OID" "1.123.345456567123"]
                                             ["Turvakielto" "kyllä"]
                                             ["Muistiinpanot" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari"]
                                             ["Hakemusmaksuvelvollisuus" "Migrin tarkastuksessa"]
                                             ["Hakemusmaksun tila" "Maksettu toisessa haussa"]
                                             ["Etunimi" "Person-etunimi"]
                                             ["Hakukohteet" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)"]]))))))

 (it "should include nothing when ids-only?=true and included-ids empty"
     (with-excel-workbook
       (export-test-excel [fixtures/application-for-hakukohde]
                          {:ids-only? true
                           :included-ids #{}})
       (fn [workbook]
         (let [metadata-sheet    (.getSheetAt workbook 0)
               application-sheet (.getSheetAt workbook 1)]
           (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
           (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
           (verify-row metadata-sheet 2 nil)
           (verify-row application-sheet 0 nil)))))

  (it "should export applications to separate sheets when ids-only?=true and included-ids, grouped by form and only with columns valid for each form"
     (with-excel-workbook
       (export-test-excel [fixtures/application-for-form fixtures/application-for-hakukohde]
                          {:skip-answers? false
                           :ids-only? true
                           :included-ids #{"application-number"
                                           "kysymys_5"}})
       (fn [workbook]
         (let [metadata-sheet              (.getSheetAt workbook 0)
               form-application-sheet      (.getSheetAt workbook 1)
               hakukohde-application-sheet (.getSheetAt workbook 2)]
           (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
           (verify-row metadata-sheet 1 ["Form name" "123" "form_123_key" "2016-06-14 15:34:56" "SEPPO PAPUNEN"])
           (verify-row metadata-sheet 2 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
           (verify-row metadata-sheet 3 nil)
           (verify-cols form-application-sheet [["Hakemusnumero" "application_9432_key"]])
           (verify-cols hakukohde-application-sheet [["Hakemusnumero" "application_3424_key"]
                                                     ["Kysymys 5" "Vastaus 5"]])))))

 (it "should include only 'included-ids' when ids-only?=true"
     (with-excel-workbook
       (export-test-excel [fixtures/application-for-hakukohde]
                          {:ids-only? true
                           :included-ids #{"application-number"
                                           "application-submitted-time"
                                           "application-modified-time"
                                           "application-state"
                                           "student-number"
                                           "applicant-oid"
                                           "turvakielto"
                                           "hakukohde-handling-state"
                                           "kielitaitovaatimus"
                                           "tutkinnon-kelpoisuus"
                                           "hakukelpoisuus"
                                           "eligibility-set-automatically"
                                           "ineligibility-reason"
                                           "maksuvelvollisuus"
                                           "valinnan-tila"
                                           "ehdollinen"
                                           "pisteet"
                                           "application-review-notes"
                                           "hakukohteet"}})
       (fn [workbook]
         (let [metadata-sheet    (.getSheetAt workbook 0)
               application-sheet (.getSheetAt workbook 1)]
           (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
           (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
           (verify-row metadata-sheet 2 nil)
           (verify-cols application-sheet [["Hakemusnumero" "application_3424_key"]
                                           ["Hakemuksen tallennusaika" "2016-06-15 15:30:00"]
                                           ["Hakemuksen viimeisimmän muokkauksen aika" "2016-06-15 15:34:56"]
                                           ["Hakemuksen tila" "Aktiivinen"]
                                           ["Oppijanumero" nil]
                                           ["Hakijan henkilö-OID" "1.123.345456567123"]
                                           ["Turvakielto" "kyllä"]
                                           ["Hakukohteen käsittelyn tila" "Käsittelyssä"]
                                           ["Kielitaitovaatimus" "Tarkastamatta"]
                                           ["Tutkinnon kelpoisuus" "Tarkastamatta"]
                                           ["Hakukelpoisuus" "Ei hakukelpoinen"]
                                           ["Hakukelpoisuus asetettu automaattisesti" nil]
                                           ["Hylkäyksen syy" "2018-07-30 18:12:14 Tarkastaja Järvinen: Ei käy"]
                                           ["Lukuvuosimaksuvelvollisuus" "Tarkastamatta"]
                                           ["Valinnan tila" "Hyväksytty"]
                                           ["Ehdollinen" "ei"]
                                           ["Pisteet" "12"]
                                           ["Muistiinpanot" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari"]
                                           ["Hakukohteet" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)"]]))))))
