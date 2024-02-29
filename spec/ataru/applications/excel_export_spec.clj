(ns ataru.applications.excel-export-spec
  (:require [ataru.applications.excel-export :as j2ee]
            [ataru.cache.cache-service :as cache-service]
            [ataru.fixtures.excel-fixtures :as fixtures]
            [ataru.forms.form-store :as form-store]
            [ataru.ohjausparametrit.ohjausparametrit-protocol :as ohjausparametrit-protocol :refer [OhjausparametritService]]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]
            [speclj.core :refer [around describe it should
                                 should-be-nil should= tags]])

  (:import [java.io File FileOutputStream]
           [java.util UUID]
           [org.apache.poi.ss.usermodel WorkbookFactory]))

(def koodisto-cache (reify cache-service/Cache
                      (get-from [_this _key])
                      (get-many-from [_this _keys])
                      (remove-from [_this _key])
                      (clear-all [_this])))

(defn- verify-row
  [sheet row-num expected-values]
  (let [row (.getRow sheet row-num)]
    (if (nil? expected-values)
      (should-be-nil row)
      (should= expected-values
               (map (fn [col] (some-> (.getCell row col)
                                      .getStringCellValue))
                    (range (.getLastCellNum row)))))))

(defn- transpose [m]
  (apply mapv vector m))

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

(def liiteri-cas-client nil)

(defrecord MockOhjausparametritServiceWithGetParametri [get-param]
  OhjausparametritService
  (get-parametri [this haku-oid] (get-param this haku-oid)))

(defn- default-get-parametri [_ _] {:jarjestetytHakutoiveet true})

(defn export-test-excel
  ([applications input-params get-parametri]
   (j2ee/export-applications liiteri-cas-client
                             applications
                             (reduce #(assoc %1 (:key %2) fixtures/application-review)
                                     {}
                                     applications)
                             fixtures/application-review-notes
                             (:selected-hakukohde input-params)
                             (:selected-hakukohderyhma input-params)
                             (:skip-answers? input-params)
                             (or (:included-ids input-params) #{})
                             true
                             :fi
                             (delay {})
                             (tarjonta-service/new-tarjonta-service)
                             koodisto-cache
                             (organization-service/new-organization-service)
                             (->MockOhjausparametritServiceWithGetParametri get-parametri)))
  ([applications input-params]
   (export-test-excel applications input-params default-get-parametri))
  ([applications]
   (export-test-excel applications {} default-get-parametri)))

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
                                        (delay {})
                                        (tarjonta-service/new-tarjonta-service)
                                        koodisto-cache
                                        (organization-service/new-organization-service)
                                        (->MockOhjausparametritServiceWithGetParametri default-get-parametri))
              (.write output#)))
       ~@body
       (finally
         (.delete ~(first bindings))))))

(defn with-excel-file [excel-data run-test]
  (let [file (File/createTempFile (str "excel-" (UUID/randomUUID)) ".xlsx")]
    (try
      (with-open [output (FileOutputStream. (.getPath file))]
        (->> excel-data
             (.write output)))
      (run-test file)
      (finally (.delete file)))))

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
     (with-excel-file
       (export-test-excel [fixtures/application-for-form] {:skip-answers? false})
       (fn [file]
         (let [workbook          (WorkbookFactory/create file)
               metadata-sheet    (.getSheetAt workbook 0)
               application-sheet (.getSheetAt workbook 1)]
           (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
           (verify-row metadata-sheet 1 ["Form name" "123" "form_123_key" "2016-06-14 15:34:56" "SEPPO PAPUNEN"])
           (verify-row metadata-sheet 2 nil)
           (verify-cols application-sheet [["Hakemusnumero" "application_9432_key"]
                                           ["Lähetysaika" "2016-06-15 15:34:56"]
                                           ["Hakemuksen tila" "Aktiivinen"]
                                           ["Oppijanumero" nil]
                                           ["Hakijan henkilö-OID" nil]
                                           ["Turvakielto" "ei"]
                                           ["Hakukohteen käsittelyn tila" "Käsittelemättä"]
                                           ["Kielitaitovaatimus" "Tarkastamatta"]
                                           ["Tutkinnon kelpoisuus" "Täyttyy"]
                                           ["Hakukelpoisuus" "Ei hakukelpoinen"]
                                           ["Hakukelpoisuus asetettu automaattisesti" nil]
                                           ["Hylkäyksen syy" nil]
                                           ["Maksuvelvollisuus" "Tarkastamatta"]
                                           ["Valinnan tila" "Kesken"]
                                           ["Ehdollinen" "ei"]
                                           ["Pisteet" "12"]
                                           ["Muistiinpanot" nil]
                                           ["Kysymys 1" "Vastaus 1"]
                                           ["Kysymys 2" "Vastaus 2"]
                                           ["Etunimi" "Lomake-etunimi"]
                                           ["Kysymys 3" "Vastaus 3"]])
           (verify-pane-information application-sheet)))))

 (it "should export applications for a hakukohde with haku"
     (with-excel {:skip-answers? false} [file [fixtures/application-for-hakukohde]]
       (let [workbook          (WorkbookFactory/create file)
             metadata-sheet    (.getSheetAt workbook 0)
             application-sheet (.getSheetAt workbook 1)]
         (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
         (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
         (verify-row metadata-sheet 2 nil)
         (verify-cols application-sheet [["Hakemusnumero" "application_3424_key"]
                                         ["Lähetysaika" "2016-06-15 15:34:56"]
                                         ["Hakemuksen tila" "Aktiivinen"]
                                         ["Oppijanumero" nil]
                                         ["Hakijan henkilö-OID" "1.123.345456567123"]
                                         ["Turvakielto" "kyllä"]
                                         ["Hakukohteen käsittelyn tila" "Käsittelyssä"]
                                         ["Kielitaitovaatimus" "Tarkastamatta"]
                                         ["Tutkinnon kelpoisuus" "Tarkastamatta"]
                                         ["Hakukelpoisuus" "Tarkastamatta"]
                                         ["Hakukelpoisuus asetettu automaattisesti" nil]
                                         ["Hylkäyksen syy" nil]
                                         ["Maksuvelvollisuus" "Tarkastamatta"]
                                         ["Valinnan tila" "Hyväksytty"]
                                         ["Ehdollinen" "ei"]
                                         ["Pisteet" "12"]
                                         ["Muistiinpanot" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari"]
                                         ["Kysymys 4" "Vastaus 4"]
                                         ["Etunimi" "Person-etunimi"]
                                         ["Kysymys 5" "Vastaus 5"]
                                         ["Hakukohteet" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)"]]))))

 (it "should export applications to separate sheets, grouped by form"
     (with-excel-file
       (export-test-excel [fixtures/application-for-form fixtures/application-for-hakukohde] {:skip-answers? false})
       (fn [file]
         (let [workbook                    (WorkbookFactory/create file)
               metadata-sheet              (.getSheetAt workbook 0)
               form-application-sheet      (.getSheetAt workbook 1)
               hakukohde-application-sheet (.getSheetAt workbook 2)]
           (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
           (verify-row metadata-sheet 1 ["Form name" "123" "form_123_key" "2016-06-14 15:34:56" "SEPPO PAPUNEN"])
           (verify-row metadata-sheet 2 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
           (verify-row metadata-sheet 3 nil)
           (verify-cols form-application-sheet [["Hakemusnumero" "application_9432_key"]
                                                ["Lähetysaika" "2016-06-15 15:34:56"]
                                                ["Hakemuksen tila" "Aktiivinen"]
                                                ["Oppijanumero" nil]
                                                ["Hakijan henkilö-OID" nil]
                                                ["Turvakielto" "ei"]
                                                ["Hakukohteen käsittelyn tila" "Käsittelemättä"]
                                                ["Kielitaitovaatimus" "Tarkastamatta"]
                                                ["Tutkinnon kelpoisuus" "Täyttyy"]
                                                ["Hakukelpoisuus" "Ei hakukelpoinen"]
                                                ["Hakukelpoisuus asetettu automaattisesti" nil]
                                                ["Hylkäyksen syy" nil]
                                                ["Maksuvelvollisuus" "Tarkastamatta"]
                                                ["Valinnan tila" "Kesken"]
                                                ["Ehdollinen" "ei"]
                                                ["Pisteet" "12"]
                                                ["Muistiinpanot" nil]
                                                ["Kysymys 1" "Vastaus 1"]
                                                ["Kysymys 2" "Vastaus 2"]
                                                ["Etunimi" "Lomake-etunimi"]
                                                ["Kysymys 3" "Vastaus 3"]])
           (verify-cols hakukohde-application-sheet [["Hakemusnumero" "application_3424_key"]
                                                     ["Lähetysaika" "2016-06-15 15:34:56"]
                                                     ["Hakemuksen tila" "Aktiivinen"]
                                                     ["Oppijanumero" nil]
                                                     ["Hakijan henkilö-OID" "1.123.345456567123"]
                                                     ["Turvakielto" "kyllä"]
                                                     ["Hakukohteen käsittelyn tila" "Käsittelyssä"]
                                                     ["Kielitaitovaatimus" "Tarkastamatta"]
                                                     ["Tutkinnon kelpoisuus" "Tarkastamatta"]
                                                     ["Hakukelpoisuus" "Tarkastamatta"]
                                                     ["Hakukelpoisuus asetettu automaattisesti" nil]
                                                     ["Hylkäyksen syy" nil]
                                                     ["Maksuvelvollisuus" "Tarkastamatta"]
                                                     ["Valinnan tila" "Hyväksytty"]
                                                     ["Ehdollinen" "ei"]
                                                     ["Pisteet" "12"]
                                                     ["Muistiinpanot" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari"]
                                                     ["Kysymys 4" "Vastaus 4"]
                                                     ["Etunimi" "Person-etunimi"]
                                                     ["Kysymys 5" "Vastaus 5"]
                                                     ["Hakukohteet" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)"]])))))

 (it "should always export answers to special questions"
     (with-redefs [form-store/fetch-by-id  (fn [_] fixtures/form-with-special-questions)
                   form-store/fetch-by-key (fn [_] fixtures/form-with-special-questions)]
       (with-excel-file
         (export-test-excel [fixtures/application-with-special-answers] {:skip-answers? true })
         (fn [file]
           (let [workbook          (WorkbookFactory/create file)
                 metadata-sheet    (.getSheetAt workbook 0)
                 application-sheet (.getSheetAt workbook 1)]
             (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
             (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
             (verify-row metadata-sheet 2 nil)
             (verify-cols application-sheet [["Hakemusnumero" "application_3424_key"]
                                             ["Lähetysaika" "2016-06-15 15:34:56"]
                                             ["Hakemuksen tila" "Aktiivinen"]
                                             ["Oppijanumero" nil]
                                             ["Hakijan henkilö-OID" "1.123.345456567123"]
                                             ["Turvakielto" "kyllä"]
                                             ["Hakukohteen käsittelyn tila" "Käsittelyssä"]
                                             ["Kielitaitovaatimus" "Tarkastamatta"]
                                             ["Tutkinnon kelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus asetettu automaattisesti" nil]
                                             ["Hylkäyksen syy" nil]
                                             ["Maksuvelvollisuus" "Tarkastamatta"]
                                             ["Valinnan tila" "Hyväksytty"]
                                             ["Ehdollinen" "ei"]
                                             ["Pisteet" "12"]
                                             ["Muistiinpanot" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari"]
                                             ["Etunimi" "Person-etunimi"]
                                             ["Hakukohteet" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)"]]))))))

 (it "should not include Kysymys 4 which does not belong to selected-hakukohde"
     (with-redefs [form-store/fetch-by-id  (fn [_] fixtures/form-for-multiple-hakukohde)
                   form-store/fetch-by-key (fn [_] fixtures/form-for-multiple-hakukohde)]
       (with-excel-file
         (export-test-excel [fixtures/application-with-special-answers] {:skip-answers? false :selected-hakukohde "hakukohde.oid"})
         (fn [file]
           (let [workbook          (WorkbookFactory/create file)
                 metadata-sheet    (.getSheetAt workbook 0)
                 application-sheet (.getSheetAt workbook 1)]
             (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
             (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
             (verify-row metadata-sheet 2 nil)
             (verify-cols application-sheet [["Hakemusnumero" "application_3424_key"]
                                             ["Lähetysaika" "2016-06-15 15:34:56"]
                                             ["Hakemuksen tila" "Aktiivinen"]
                                             ["Oppijanumero" nil]
                                             ["Hakijan henkilö-OID" "1.123.345456567123"]
                                             ["Turvakielto" "kyllä"]
                                             ["Hakukohteen käsittelyn tila" "Käsittelyssä"]
                                             ["Kielitaitovaatimus" "Tarkastamatta"]
                                             ["Tutkinnon kelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus asetettu automaattisesti" nil]
                                             ["Hylkäyksen syy" nil]
                                             ["Maksuvelvollisuus" "Tarkastamatta"]
                                             ["Valinnan tila" "Hyväksytty"]
                                             ["Ehdollinen" "ei"]
                                             ["Pisteet" "12"]
                                             ["Muistiinpanot" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari"]
                                             ["Visible from form" nil]
                                             ["Should be visible because belongs-to-hakukohde is not specified" nil]
                                             ["Etunimi" "Person-etunimi"]
                                             ["Kysymys 5" "Vastaus 5 will be included only when skip-answers? == false"]
                                             ["Jos tulen hyväksytyksi, oppilaitos voi julkaista nimeni omilla verkkosivuillaan." "Ei"]
                                             ["Hakukohteet" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)"]]))))))

 (it "should include questions when hakukohde belongs to hakukohderyhma"
     (with-redefs [form-store/fetch-by-id  (fn [_] fixtures/form-for-multiple-hakukohde)
                   form-store/fetch-by-key (fn [_] fixtures/form-for-multiple-hakukohde)]
       (with-excel-file
         (export-test-excel [fixtures/application-with-special-answers-2] {:skip-answers? false :selected-hakukohde "hakukohde-in-ryhma.oid"})
         (fn [file]
           (let [workbook          (WorkbookFactory/create file)
                 metadata-sheet    (.getSheetAt workbook 0)
                 application-sheet (.getSheetAt workbook 1)]
             (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
             (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
             (verify-row metadata-sheet 2 nil)
             (verify-cols application-sheet [["Hakemusnumero" "application_3424_key"]
                                             ["Lähetysaika" "2016-06-15 15:34:56"]
                                             ["Hakemuksen tila" "Aktiivinen"]
                                             ["Oppijanumero" nil]
                                             ["Hakijan henkilö-OID" "1.123.345456567123"]
                                             ["Turvakielto" "kyllä"]
                                             ["Hakukohteen käsittelyn tila" "Käsittelyssä"]
                                             ["Kielitaitovaatimus" "Tarkastamatta"]
                                             ["Tutkinnon kelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus asetettu automaattisesti" nil]
                                             ["Hylkäyksen syy" nil]
                                             ["Maksuvelvollisuus" "Tarkastamatta"]
                                             ["Valinnan tila" "Hyväksytty"]
                                             ["Ehdollinen" "ei"]
                                             ["Pisteet" "12"]
                                             ["Muistiinpanot" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari"]
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
       (with-excel-file
         (export-test-excel [fixtures/application-with-special-answers-2] {:skip-answers? false :selected-hakukohderyhma "1.2.246.562.28.00000000001"})
         (fn [file]
           (let [workbook          (WorkbookFactory/create file)
                 metadata-sheet    (.getSheetAt workbook 0)
                 application-sheet (.getSheetAt workbook 1)]
             (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
             (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
             (verify-row metadata-sheet 2 nil)
             (verify-cols application-sheet [["Hakemusnumero" "application_3424_key"]
                                             ["Lähetysaika" "2016-06-15 15:34:56"]
                                             ["Hakemuksen tila" "Aktiivinen"]
                                             ["Oppijanumero" nil]
                                             ["Hakijan henkilö-OID" "1.123.345456567123"]
                                             ["Turvakielto" "kyllä"]
                                             ["Hakukohteen käsittelyn tila" "Käsittelyssä"]
                                             ["Kielitaitovaatimus" "Tarkastamatta"]
                                             ["Tutkinnon kelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus asetettu automaattisesti" nil]
                                             ["Hylkäyksen syy" nil]
                                             ["Maksuvelvollisuus" "Tarkastamatta"]
                                             ["Valinnan tila" "Hyväksytty"]
                                             ["Ehdollinen" "ei"]
                                             ["Pisteet" "12"]
                                             ["Muistiinpanot" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari"]
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
       (with-excel-file
         (export-test-excel [fixtures/application-with-special-answers] {:skip-answers? false :selected-hakukohderyhma "unknown-hakukohderyhma"})
         (fn [file]
           (let [workbook          (WorkbookFactory/create file)
                 metadata-sheet    (.getSheetAt workbook 0)
                 application-sheet (.getSheetAt workbook 1)]
             (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
             (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
             (verify-row metadata-sheet 2 nil)
             (verify-cols application-sheet [["Hakemusnumero" "application_3424_key"]
                                             ["Lähetysaika" "2016-06-15 15:34:56"]
                                             ["Hakemuksen tila" "Aktiivinen"]
                                             ["Oppijanumero" nil]
                                             ["Hakijan henkilö-OID" "1.123.345456567123"]
                                             ["Turvakielto" "kyllä"]
                                             ["Hakukohteen käsittelyn tila" "Käsittelyssä"]
                                             ["Kielitaitovaatimus" "Tarkastamatta"]
                                             ["Tutkinnon kelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus asetettu automaattisesti" nil]
                                             ["Hylkäyksen syy" nil]
                                             ["Maksuvelvollisuus" "Tarkastamatta"]
                                             ["Valinnan tila" "Hyväksytty"]
                                             ["Ehdollinen" "ei"]
                                             ["Pisteet" "12"]
                                             ["Muistiinpanot" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari"]
                                             ["Should be visible because belongs-to-hakukohde is not specified" nil]
                                             ["Etunimi" "Person-etunimi"]
                                             ["Kysymys 5" "Vastaus 5 will be included only when skip-answers? == false"]
                                             ["Jos tulen hyväksytyksi, oppilaitos voi julkaista nimeni omilla verkkosivuillaan." "Ei"]
                                             ["Hakukohteet" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)"]]))))))

 (it "should not include Kysymys 4 when not including everything"
     (with-redefs [form-store/fetch-by-id  (fn [_] fixtures/form-with-special-questions)
                   form-store/fetch-by-key (fn [_] fixtures/form-with-special-questions)]
       (with-excel-file
         (export-test-excel [fixtures/application-with-special-answers] {:skip-answers? true :included-ids #{"joku-kysymys-vaan"}})
         (fn [file]
           (let [workbook          (WorkbookFactory/create file)
                 metadata-sheet    (.getSheetAt workbook 0)
                 application-sheet (.getSheetAt workbook 1)]
             (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
             (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
             (verify-row metadata-sheet 2 nil)
             (verify-cols application-sheet [["Hakemusnumero" "application_3424_key"]
                                             ["Lähetysaika" "2016-06-15 15:34:56"]
                                             ["Hakemuksen tila" "Aktiivinen"]
                                             ["Oppijanumero" nil]
                                             ["Hakijan henkilö-OID" "1.123.345456567123"]
                                             ["Turvakielto" "kyllä"]
                                             ["Hakukohteen käsittelyn tila" "Käsittelyssä"]
                                             ["Kielitaitovaatimus" "Tarkastamatta"]
                                             ["Tutkinnon kelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus" "Tarkastamatta"]
                                             ["Hakukelpoisuus asetettu automaattisesti" nil]
                                             ["Hylkäyksen syy" nil]
                                             ["Maksuvelvollisuus" "Tarkastamatta"]
                                             ["Valinnan tila" "Hyväksytty"]
                                             ["Ehdollinen" "ei"]
                                             ["Pisteet" "12"]
                                             ["Muistiinpanot" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari"]
                                             ["Etunimi" "Person-etunimi"]
                                             ["Hakukohteet" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)"]])))))))
