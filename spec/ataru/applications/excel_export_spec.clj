(ns ataru.applications.excel-export-spec
  (:require [ataru.applications.excel-export :as j2ee]
            [ataru.fixtures.excel-fixtures :as fixtures]
            [ataru.cache.cache-service :as cache-service]
            [ataru.forms.form-store :as form-store]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]
            [speclj.core :refer [around should-be-nil should== should= should it describe tags]]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.ohjausparametrit.ohjausparametrit-service :as ohjausparametrit-service])
  (:import [java.io FileOutputStream File]
           [java.util UUID]
           [org.apache.poi.ss.usermodel WorkbookFactory]))

(def koodisto-cache (reify cache-service/Cache
                      #_{:clj-kondo/ignore [:unused-binding]}
                      (get-from [this key])
                      #_{:clj-kondo/ignore [:unused-binding]}
                      (get-many-from [this keys])
                      #_{:clj-kondo/ignore [:unused-binding]}
                      (remove-from [this key])
                      #_{:clj-kondo/ignore [:unused-binding]}
                      (clear-all [this])))

(defn- verify-row
  [sheet row-num expected-values]
  (let [row (.getRow sheet row-num)]
    (if (nil? expected-values)
      (should-be-nil row)
      (should== expected-values
                (map (fn [col] (some-> (.getCell row col)
                                       .getStringCellValue))
                     (range (.getLastCellNum row)))))))

(defn- verify-pane-information
  [sheet]
  (let [info (.getPaneInformation sheet)]
    (should (.isFreezePane info))
    (should= 1 (.getHorizontalSplitPosition info))
    (should= 0 (.getVerticalSplitPosition info))
    (should= 1 (.getHorizontalSplitTopRow info))
    (should= 0 (.getVerticalSplitLeftColumn info))))

(def liiteri-cas-client nil)

(defmacro with-excel [input-params bindings & body]
  `(let [~(first bindings) (File/createTempFile (str "excel-" (UUID/randomUUID)) ".xlsx")
         applications#     ~(second bindings)]
     (try
       (with-open [output# (FileOutputStream. (.getPath ~(first bindings)))]
         (->> (j2ee/export-applications liiteri-cas-client
                                        applications#
                                        (reduce #(assoc %1 (:key %2) fixtures/application-review)
                                                {}
                                                applications#)
                                        fixtures/application-review-notes
                                        (~input-params :selected-hakukohde)
                                        (~input-params :selected-hakukohderyhma)
                                        (~input-params :skip-answers?)
                                        (or (~input-params :included-ids) #{})
                                        true
                                        :fi
                                        {}
                                        (tarjonta-service/new-tarjonta-service)
                                        koodisto-cache
                                        (organization-service/new-organization-service)
                                        (ohjausparametrit-service/new-ohjausparametrit-service))
              (.write output#)))
       ~@body
       (finally
         (.delete ~(first bindings))))))
(comment 
(describe "excel export"
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
    (with-excel {:skip-answers? false} [file [fixtures/application-for-form]]
      (let [workbook          (WorkbookFactory/create file)
            metadata-sheet    (.getSheetAt workbook 0)
            application-sheet (.getSheetAt workbook 1)]
        (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
        (verify-row metadata-sheet 1 ["Form name" "123" "form_123_key" "2016-06-14 15:34:56" "SEPPO PAPUNEN"])
        (verify-row metadata-sheet 2 nil)
        (verify-row application-sheet 0 ["Hakemusnumero" "Lähetysaika" "Hakemuksen tila" "Hakukohteen käsittelyn tila" "Kielitaitovaatimus" "Tutkinnon kelpoisuus" "Hakukelpoisuus" "Hakukelpoisuus asetettu automaattisesti" "Hylkäyksen syy" "Maksuvelvollisuus" "Valinnan tila" "Ehdollinen" "Pisteet" "Oppijanumero" "Hakijan henkilö-OID" "Turvakielto" "Muistiinpanot" "Kysymys 1" "Kysymys 2" "Etunimi" "Kysymys 3"])
        (verify-row application-sheet 1 ["application_9432_key" "2016-06-15 15:34:56" "Aktiivinen" "Käsittelemättä" "Tarkastamatta" "Täyttyy" "Ei hakukelpoinen" nil nil "Tarkastamatta" "Kesken" "ei" "12" nil nil "ei" nil "Vastaus 1" "Vastaus 2" "Lomake-etunimi" "Vastaus 3"])
        (verify-row application-sheet 2 nil)
        (verify-pane-information application-sheet))))

  (it "should export applications for a hakukohde with haku"
    (with-excel {:skip-answers? false} [file [fixtures/application-for-hakukohde]]
      (let [workbook          (WorkbookFactory/create file)
            metadata-sheet    (.getSheetAt workbook 0)
            application-sheet (.getSheetAt workbook 1)]
        (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
        (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
        (verify-row metadata-sheet 2 nil)
        (verify-row application-sheet 0 ["Hakemusnumero" "Lähetysaika" "Hakemuksen tila" "Hakukohteen käsittelyn tila" "Kielitaitovaatimus" "Tutkinnon kelpoisuus" "Hakukelpoisuus" "Hakukelpoisuus asetettu automaattisesti" "Hylkäyksen syy" "Maksuvelvollisuus" "Valinnan tila" "Ehdollinen" "Pisteet" "Oppijanumero" "Hakijan henkilö-OID" "Turvakielto" "Muistiinpanot" "Kysymys 4" "Etunimi" "Kysymys 5" "Hakukohteet"])
        (verify-row application-sheet 1 ["application_3424_key" "2016-06-15 15:34:56" "Aktiivinen" "Käsittelyssä" "Tarkastamatta" "Tarkastamatta" "Tarkastamatta" nil nil "Tarkastamatta" "Hyväksytty" "ei" "12" nil "1.123.345456567123" "kyllä" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari" "Vastaus 4" "Person-etunimi" "Vastaus 5" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)"])
        (verify-row application-sheet 2 nil))))

  (it "should export applications to separate sheets, grouped by form"
    (with-excel {:skip-answers? false} [file [fixtures/application-for-form fixtures/application-for-hakukohde]]
      (let [workbook                    (WorkbookFactory/create file)
            metadata-sheet              (.getSheetAt workbook 0)
            form-application-sheet      (.getSheetAt workbook 1)
            hakukohde-application-sheet (.getSheetAt workbook 2)]
        (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
        (verify-row metadata-sheet 1 ["Form name" "123" "form_123_key" "2016-06-14 15:34:56" "SEPPO PAPUNEN"])
        (verify-row metadata-sheet 2 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
        (verify-row metadata-sheet 3 nil)
        (verify-row form-application-sheet 0 ["Hakemusnumero" "Lähetysaika" "Hakemuksen tila" "Hakukohteen käsittelyn tila" "Kielitaitovaatimus" "Tutkinnon kelpoisuus" "Hakukelpoisuus" "Hakukelpoisuus asetettu automaattisesti" "Hylkäyksen syy" "Maksuvelvollisuus" "Valinnan tila" "Ehdollinen" "Pisteet" "Oppijanumero" "Hakijan henkilö-OID" "Turvakielto" "Muistiinpanot" "Kysymys 1" "Kysymys 2" "Etunimi" "Kysymys 3"])
        (verify-row form-application-sheet 1 ["application_9432_key" "2016-06-15 15:34:56" "Aktiivinen" "Käsittelemättä" "Tarkastamatta" "Täyttyy" "Ei hakukelpoinen" nil nil "Tarkastamatta" "Kesken" "ei" "12" nil nil "ei" nil "Vastaus 1" "Vastaus 2" "Lomake-etunimi" "Vastaus 3"])
        (verify-row form-application-sheet 2 nil)
        (verify-row hakukohde-application-sheet 0 ["Hakemusnumero" "Lähetysaika" "Hakemuksen tila" "Hakukohteen käsittelyn tila" "Kielitaitovaatimus" "Tutkinnon kelpoisuus" "Hakukelpoisuus" "Hakukelpoisuus asetettu automaattisesti" "Hylkäyksen syy" "Maksuvelvollisuus" "Valinnan tila" "Ehdollinen" "Pisteet" "Oppijanumero" "Hakijan henkilö-OID" "Turvakielto" "Muistiinpanot" "Kysymys 4" "Etunimi" "Kysymys 5" "Hakukohteet"])
        (verify-row hakukohde-application-sheet 1 ["application_3424_key" "2016-06-15 15:34:56" "Aktiivinen" "Käsittelyssä" "Tarkastamatta" "Tarkastamatta" "Tarkastamatta" nil nil "Tarkastamatta" "Hyväksytty" "ei" "12" nil "1.123.345456567123" "kyllä" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari" "Vastaus 4" "Person-etunimi" "Vastaus 5" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)"])
        (verify-row hakukohde-application-sheet 2 nil))))

  (it "should always export answers to special questions"
    (with-redefs [form-store/fetch-by-id  (fn [_] fixtures/form-with-special-questions)
                  form-store/fetch-by-key (fn [_] fixtures/form-with-special-questions)]
      (with-excel {:skip-answers? true :included-ids nil} [file [fixtures/application-with-special-answers]]
        (let [workbook          (WorkbookFactory/create file)
              metadata-sheet    (.getSheetAt workbook 0)
              application-sheet (.getSheetAt workbook 1)]
          (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
          (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
          (verify-row metadata-sheet 2 nil)
          (verify-row application-sheet 0 ["Hakemusnumero" "Lähetysaika" "Hakemuksen tila" "Hakukohteen käsittelyn tila" "Kielitaitovaatimus" "Tutkinnon kelpoisuus" "Hakukelpoisuus" "Hakukelpoisuus asetettu automaattisesti" "Hylkäyksen syy" "Maksuvelvollisuus" "Valinnan tila" "Ehdollinen" "Pisteet" "Oppijanumero" "Hakijan henkilö-OID" "Turvakielto" "Muistiinpanot" "Etunimi" "Hakukohteet"])
          (verify-row application-sheet 1 ["application_3424_key" "2016-06-15 15:34:56" "Aktiivinen" "Käsittelyssä" "Tarkastamatta" "Tarkastamatta" "Tarkastamatta" nil nil "Tarkastamatta" "Hyväksytty" "ei" "12" nil "1.123.345456567123" "kyllä" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari" "Person-etunimi" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)"])
          (verify-row application-sheet 2 nil)))))

  (it "should not include Kysymys 4 which does not belong to selected-hakukohde"
      (with-redefs [form-store/fetch-by-id  (fn [_] fixtures/form-for-multiple-hakukohde)
                    form-store/fetch-by-key (fn [_] fixtures/form-for-multiple-hakukohde)]
        (with-excel {:skip-answers? false :included-ids nil :selected-hakukohde "hakukohde.oid"} [file [fixtures/application-with-special-answers]]
          (let [workbook          (WorkbookFactory/create file)
                metadata-sheet    (.getSheetAt workbook 0)
                application-sheet (.getSheetAt workbook 1)]
            (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
            (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
            (verify-row metadata-sheet 2 nil)
            (verify-row application-sheet 0 ["Hakemusnumero" "Lähetysaika" "Hakemuksen tila" "Hakukohteen käsittelyn tila" "Kielitaitovaatimus" "Tutkinnon kelpoisuus" "Hakukelpoisuus" "Hakukelpoisuus asetettu automaattisesti" "Hylkäyksen syy" "Maksuvelvollisuus" "Valinnan tila" "Ehdollinen" "Pisteet" "Oppijanumero" "Hakijan henkilö-OID" "Turvakielto" "Muistiinpanot" "Jos tulen hyväksytyksi, oppilaitos voi julkaista nimeni omilla verkkosivuillaan." "Etunimi" "Hakukohteet" "Kysymys 5" "Visible from form" "Should be visible because belongs-to-hakukohde is not specified"])
            (verify-row application-sheet 1 ["application_3424_key" "2016-06-15 15:34:56" "Aktiivinen" "Käsittelyssä" "Tarkastamatta" "Tarkastamatta" "Tarkastamatta" nil nil "Tarkastamatta" "Hyväksytty" "ei" "12" nil "1.123.345456567123" "kyllä" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari" "Ei" "Person-etunimi" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)" "Vastaus 5 will be included only when skip-answers? == false" nil nil])
            (verify-row application-sheet 2 nil)))))

  (it "should include questions when hakukohde belongs to hakukohderyhma"
      (with-redefs [form-store/fetch-by-id  (fn [_] fixtures/form-for-multiple-hakukohde)
                    form-store/fetch-by-key (fn [_] fixtures/form-for-multiple-hakukohde)]
        (with-excel {:skip-answers? false :included-ids nil :selected-hakukohde "hakukohde-in-ryhma.oid"} [file [fixtures/application-with-special-answers-2]]
          (let [workbook          (WorkbookFactory/create file)
                metadata-sheet    (.getSheetAt workbook 0)
                application-sheet (.getSheetAt workbook 1)]
            (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
            (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
            (verify-row metadata-sheet 2 nil)
            (verify-row application-sheet 0 ["Hakemusnumero" "Lähetysaika" "Hakemuksen tila" "Hakukohteen käsittelyn tila" "Kielitaitovaatimus" "Tutkinnon kelpoisuus" "Hakukelpoisuus" "Hakukelpoisuus asetettu automaattisesti" "Hylkäyksen syy" "Maksuvelvollisuus" "Valinnan tila" "Ehdollinen" "Pisteet" "Oppijanumero" "Hakijan henkilö-OID" "Turvakielto" "Muistiinpanot" "Jos tulen hyväksytyksi, oppilaitos voi julkaista nimeni omilla verkkosivuillaan." "Etunimi" "Hakukohteet" "Kysymys 5" "Visible from form 2" "Should be visible because belongs-to-hakukohde is not specified" "Visible only if belongs to hakukohderyhmä1" "Visible because of parent's hakukohderyhmä"])
            (verify-row application-sheet 1 ["application_3424_key" "2016-06-15 15:34:56" "Aktiivinen" "Käsittelyssä" "Tarkastamatta" "Tarkastamatta" "Tarkastamatta" nil nil "Tarkastamatta" "Hyväksytty" "ei" "12" nil "1.123.345456567123" "kyllä" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari" "Ei" "Person-etunimi" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde-in-ryhma.oid)" "Vastaus 5 will be included only when skip-answers? == false" nil nil nil nil])
            (verify-row application-sheet 2 nil)))))

  (it "should not include questions belonging to hakukohderyhma"
      (with-redefs [form-store/fetch-by-id  (fn [_] fixtures/form-for-multiple-hakukohde)
                    form-store/fetch-by-key (fn [_] fixtures/form-for-multiple-hakukohde)]
        (with-excel {:skip-answers? false :included-ids nil :selected-hakukohderyhma "1.2.246.562.28.00000000001"} [file [fixtures/application-with-special-answers-2]]
                    (let [workbook          (WorkbookFactory/create file)
                          metadata-sheet    (.getSheetAt workbook 0)
                          application-sheet (.getSheetAt workbook 1)]
                      (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
                      (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
                      (verify-row metadata-sheet 2 nil)
                      (verify-row application-sheet 0 ["Hakemusnumero" "Lähetysaika" "Hakemuksen tila" "Hakukohteen käsittelyn tila" "Kielitaitovaatimus" "Tutkinnon kelpoisuus" "Hakukelpoisuus" "Hakukelpoisuus asetettu automaattisesti" "Hylkäyksen syy" "Maksuvelvollisuus" "Valinnan tila" "Ehdollinen" "Pisteet" "Oppijanumero" "Hakijan henkilö-OID" "Turvakielto" "Muistiinpanot" "Jos tulen hyväksytyksi, oppilaitos voi julkaista nimeni omilla verkkosivuillaan." "Etunimi" "Hakukohteet" "Kysymys 5" "Should be visible because belongs-to-hakukohde is not specified" "Visible from form 2" "Visible only if belongs to hakukohderyhmä1" "Visible because of parent's hakukohderyhmä"])
                      (verify-row application-sheet 1 ["application_3424_key" "2016-06-15 15:34:56" "Aktiivinen" "Käsittelyssä" "Tarkastamatta" "Tarkastamatta" "Tarkastamatta" nil nil "Tarkastamatta" "Hyväksytty" "ei" "12" nil "1.123.345456567123" "kyllä" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari" "Ei" "Person-etunimi" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde-in-ryhma.oid)" "Vastaus 5 will be included only when skip-answers? == false" nil nil nil nil])
                      (verify-row application-sheet 2 nil)))))

  (it "should not include questions for different hakukohderyhma"
      (with-redefs [form-store/fetch-by-id  (fn [_] fixtures/form-for-multiple-hakukohde)
                    form-store/fetch-by-key (fn [_] fixtures/form-for-multiple-hakukohde)]
        (with-excel {:skip-answers? false :included-ids nil :selected-hakukohderyhma "unknown-hakukohderyhma"} [file [fixtures/application-with-special-answers]]
          (let [workbook          (WorkbookFactory/create file)
                metadata-sheet    (.getSheetAt workbook 0)
                application-sheet (.getSheetAt workbook 1)]
            (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
            (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
            (verify-row metadata-sheet 2 nil)
            (verify-row application-sheet 0 ["Hakemusnumero" "Lähetysaika" "Hakemuksen tila" "Hakukohteen käsittelyn tila" "Kielitaitovaatimus" "Tutkinnon kelpoisuus" "Hakukelpoisuus" "Hakukelpoisuus asetettu automaattisesti" "Hylkäyksen syy" "Maksuvelvollisuus" "Valinnan tila" "Ehdollinen" "Pisteet" "Oppijanumero" "Hakijan henkilö-OID" "Turvakielto" "Muistiinpanot" "Jos tulen hyväksytyksi, oppilaitos voi julkaista nimeni omilla verkkosivuillaan." "Etunimi" "Hakukohteet" "Kysymys 5" "Should be visible because belongs-to-hakukohde is not specified"])
            (verify-row application-sheet 1 ["application_3424_key" "2016-06-15 15:34:56" "Aktiivinen" "Käsittelyssä" "Tarkastamatta" "Tarkastamatta" "Tarkastamatta" nil nil "Tarkastamatta" "Hyväksytty" "ei" "12" nil "1.123.345456567123" "kyllä" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari" "Ei" "Person-etunimi" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)" "Vastaus 5 will be included only when skip-answers? == false" nil])
            (verify-row application-sheet 2 nil)))))


  (it "should not include Kysymys 4 when not including everything"
      (with-redefs [form-store/fetch-by-id  (fn [_] fixtures/form-with-special-questions)
                    form-store/fetch-by-key (fn [_] fixtures/form-with-special-questions)]
        (with-excel {:skip-answers? true :included-ids #{"joku-kysymys-vaan"}} [file [fixtures/application-with-special-answers]]
                    (let [workbook          (WorkbookFactory/create file)
                          metadata-sheet    (.getSheetAt workbook 0)
                          application-sheet (.getSheetAt workbook 1)]
                      (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
                      (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
                      (verify-row metadata-sheet 2 nil)
                      (verify-row application-sheet 0 ["Hakemusnumero" "Lähetysaika" "Hakemuksen tila" "Hakukohteen käsittelyn tila" "Kielitaitovaatimus" "Tutkinnon kelpoisuus" "Hakukelpoisuus" "Hakukelpoisuus asetettu automaattisesti" "Hylkäyksen syy" "Maksuvelvollisuus" "Valinnan tila" "Ehdollinen" "Pisteet" "Oppijanumero" "Hakijan henkilö-OID" "Turvakielto" "Muistiinpanot" "Etunimi" "Hakukohteet"])
                      (verify-row application-sheet 1 ["application_3424_key" "2016-06-15 15:34:56" "Aktiivinen" "Käsittelyssä" "Tarkastamatta" "Tarkastamatta" "Tarkastamatta" nil nil "Tarkastamatta" "Hyväksytty" "ei" "12" nil "1.123.345456567123" "kyllä" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari" "Person-etunimi" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)"])
                      (verify-row application-sheet 2 nil))))))
)