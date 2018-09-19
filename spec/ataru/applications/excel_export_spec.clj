(ns ataru.applications.excel-export-spec
  (:require [ataru.applications.application-store :as application-store]
            [ataru.applications.excel-export :as j2ee]
            [ataru.fixtures.excel-fixtures :as fixtures]
            [ataru.forms.form-store :as form-store]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]
            [speclj.core :refer :all]
            [ataru.ohjausparametrit.ohjausparametrit-service :as ohjausparametrit-service]
            [ataru.organization-service.organization-service :as organization-service])
  (:import [java.io FileOutputStream File]
           [java.util UUID]
           [org.apache.poi.ss.usermodel WorkbookFactory]))

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

(defmacro with-excel [bindings & body]
  `(let [~(first bindings) (File/createTempFile (str "excel-" (UUID/randomUUID)) ".xlsx")]
     (try
       (with-open [output# (FileOutputStream. (.getPath ~(first bindings)))]
         (->> (j2ee/export-applications ~(second bindings)
                                        (reduce #(assoc %1 (:key %2) fixtures/application-review)
                                                {}
                                                ~(second bindings))
                                        fixtures/application-review-notes
                                        nil
                                        nil
                                        false
                                        :fi
                                        (tarjonta-service/new-tarjonta-service)
                                        (organization-service/new-organization-service)
                                        (ohjausparametrit-service/new-ohjausparametrit-service))
              (.write output#)))
       ~@body
       (finally
         (.delete ~(first bindings))))))

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
    (with-excel [file [fixtures/application-for-form]]
      (let [workbook          (WorkbookFactory/create file)
            metadata-sheet    (.getSheetAt workbook 0)
            application-sheet (.getSheetAt workbook 1)]
        (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
        (verify-row metadata-sheet 1 ["Form name" "123" "form_123_key" "2016-06-14 15:34:56" "SEPPO PAPUNEN"])
        (verify-row metadata-sheet 2 nil)
        (verify-row application-sheet 0 ["Id" "Lähetysaika" "Hakemuksen tila" "Hakukohteen käsittelyn tila" "Kielitaitovaatimus" "Tutkinnon kelpoisuus" "Hakukelpoisuus" "Maksuvelvollisuus" "Valinnan tila" "Pisteet" "Hakijan henkilö-OID" "Turvakielto" "Muistiinpanot" "Kysymys 1" "Kysymys 2" "Etunimi" "Kysymys 3"])
        (verify-row application-sheet 1 ["application_9432_key" "2016-06-15 15:34:56" "Aktiivinen" "Käsittelemättä" "Tarkastamatta" "Täyttyy" "Ei hakukelpoinen" "Tarkastamatta" "Kesken" "12" nil "ei" nil "Vastaus 1" "Vastaus 2" "Lomake-etunimi" "Vastaus 3"])
        (verify-row application-sheet 2 nil)
        (verify-pane-information application-sheet))))

  (it "should export applications for a hakukohde with haku"
    (with-excel [file [fixtures/application-for-hakukohde]]
      (let [workbook          (WorkbookFactory/create file)
            metadata-sheet    (.getSheetAt workbook 0)
            application-sheet (.getSheetAt workbook 1)]
        (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
        (verify-row metadata-sheet 1 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
        (verify-row metadata-sheet 2 nil)
        (verify-row application-sheet 0 ["Id" "Lähetysaika" "Hakemuksen tila" "Hakukohteen käsittelyn tila" "Kielitaitovaatimus" "Tutkinnon kelpoisuus" "Hakukelpoisuus" "Maksuvelvollisuus" "Valinnan tila" "Pisteet" "Hakijan henkilö-OID" "Turvakielto" "Muistiinpanot" "Kysymys 4" "Etunimi" "Kysymys 5" "Hakukohteet"])
        (verify-row application-sheet 1 ["application_3424_key" "2016-06-15 15:34:56" "Aktiivinen" "Käsittelyssä" "Tarkastamatta" "Tarkastamatta" "Tarkastamatta" "Tarkastamatta" "Hyväksytty" "12" "1.123.345456567123" "kyllä" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari" "Vastaus 4" "Person-etunimi" "Vastaus 5" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)"])
        (verify-row application-sheet 2 nil))))

  (it "should export applications to separate sheets, grouped by form"
    (with-excel [file [fixtures/application-for-form fixtures/application-for-hakukohde]]
      (let [workbook                    (WorkbookFactory/create file)
            metadata-sheet              (.getSheetAt workbook 0)
            form-application-sheet      (.getSheetAt workbook 1)
            hakukohde-application-sheet (.getSheetAt workbook 2)]
        (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
        (verify-row metadata-sheet 1 ["Form name" "123" "form_123_key" "2016-06-14 15:34:56" "SEPPO PAPUNEN"])
        (verify-row metadata-sheet 2 ["Form name" "321" "form_321_key" "2016-06-14 15:34:56" "IRMELI KUIKELOINEN"])
        (verify-row metadata-sheet 3 nil)
        (verify-row form-application-sheet 0 ["Id" "Lähetysaika" "Hakemuksen tila" "Hakukohteen käsittelyn tila" "Kielitaitovaatimus" "Tutkinnon kelpoisuus" "Hakukelpoisuus" "Maksuvelvollisuus" "Valinnan tila" "Pisteet" "Hakijan henkilö-OID" "Turvakielto" "Muistiinpanot" "Kysymys 1" "Kysymys 2" "Etunimi" "Kysymys 3"])
        (verify-row form-application-sheet 1 ["application_9432_key" "2016-06-15 15:34:56" "Aktiivinen" "Käsittelemättä" "Tarkastamatta" "Täyttyy" "Ei hakukelpoinen" "Tarkastamatta" "Kesken" "12" nil "ei" nil "Vastaus 1" "Vastaus 2" "Lomake-etunimi" "Vastaus 3"])
        (verify-row form-application-sheet 2 nil)
        (verify-row hakukohde-application-sheet 0 ["Id" "Lähetysaika" "Hakemuksen tila" "Hakukohteen käsittelyn tila" "Kielitaitovaatimus" "Tutkinnon kelpoisuus" "Hakukelpoisuus" "Maksuvelvollisuus" "Valinnan tila" "Pisteet" "Hakijan henkilö-OID" "Turvakielto" "Muistiinpanot" "Kysymys 4" "Etunimi" "Kysymys 5" "Hakukohteet"])
        (verify-row hakukohde-application-sheet 1 ["application_3424_key" "2016-06-15 15:34:56" "Aktiivinen" "Käsittelyssä" "Tarkastamatta" "Tarkastamatta" "Tarkastamatta" "Tarkastamatta" "Hyväksytty" "12" "1.123.345456567123" "kyllä" "2018-07-29 17:11:12 Virk Ailija: Asia kunnossa,\n2018-07-30 18:12:13 Ajilia Kriv: Muikkari" "Vastaus 4" "Person-etunimi" "Vastaus 5" "(1) Ajoneuvonosturinkuljettajan ammattitutkinto - Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (hakukohde.oid)"])
        (verify-row hakukohde-application-sheet 2 nil)))))
