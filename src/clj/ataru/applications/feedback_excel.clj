(ns ataru.applications.feedback-excel
  (:require [ataru.applications.excel-export :as excel-export]
            [ataru.translations.texts :refer [feedback-excel-texts]])
  (:import [java.io ByteArrayOutputStream]
           [org.apache.poi.xssf.usermodel XSSFCell XSSFRow XSSFSheet XSSFWorkbook]))

(def ^:private columns
  [{:header         :timestamp
    :width-in-chars 20
    :value          (fn [feedback] (some-> (:created-time feedback) excel-export/time-formatter))}
   {:header         :rating
    :width-in-chars 10
    :value          :stars}
   {:header         :feedback
    :width-in-chars 100
    :value          :feedback}
   {:header         :user-agent
    :width-in-chars 60
    :value          :user-agent}
   {:header         :haku
    :width-in-chars 32
    :value          :haku-oid}])

(defn- create-styles [^XSSFWorkbook workbook]
  {:text           (doto (.createCellStyle workbook)
                     (.setWrapText true))
   :quote-prefixed (doto (.createCellStyle workbook)
                     (.setQuotePrefixed true)
                     (.setWrapText true))})

(defn- formula-like?
  "Hakijan kirjoittama teksti ei saa päätyä Exceliin kaavana. Kaavaksi
   tulkitaan myös teksti, jossa kaavamerkkiä edeltää tyhjää."
  [value]
  (and (string? value)
       (some? (re-find #"^\s*[=+\-@]" value))))

(defn- truncate
  "Excelin solu ei voi sisältää rajattoman pitkää tekstiä, ja esimerkiksi
   käyttäjäagentin pituutta ei rajoiteta tallennettaessa."
  [^String value]
  (if (< excel-export/max-value-length (count value))
    (str (subs value 0 (dec excel-export/max-value-length)) "…")
    value))

(defn- write-cell!
  "Kirjoittaa arvon soluun. Tyhjälle arvolle ei luoda solua lainkaan."
  [styles ^XSSFRow row column value]
  (when (some? value)
    (let [^XSSFCell cell (.createCell row (int column))]
      (if (number? value)
        (.setCellValue cell (double value))
        (do (.setCellStyle cell (if (formula-like? value)
                                  (:quote-prefixed styles)
                                  (:text styles)))
            (.setCellValue cell ^String (truncate (str value)))))
      cell)))

(defn- text [lang text-key]
  (or (get-in feedback-excel-texts [text-key lang])
      (get-in feedback-excel-texts [text-key :fi])))

(defn- set-column-widths!
  "Leveydet ovat kiinteät, koska sisällön mukaan mitoittaminen on hidasta,
   kun palautteita on paljon."
  [^XSSFSheet sheet]
  (doseq [[column {:keys [width-in-chars]}] (map-indexed vector columns)]
    (.setColumnWidth sheet (int column) (int (* width-in-chars 256)))))

(defn export-feedback
  "Muodostaa lomakkeen palautteista Excel-tiedoston, jossa on otsikkorivi ja
   yksi rivi per palaute. Palauttaa tiedoston sisällön tavuina."
  ^bytes [feedback lang]
  (let [workbook   (XSSFWorkbook.)
        styles     (create-styles workbook)
        sheet      (.createSheet workbook (text lang :sheet-name))
        header-row (.createRow sheet 0)]
    (doseq [[column {:keys [header]}] (map-indexed vector columns)]
      (write-cell! styles header-row column (text lang header)))
    (doseq [[row-index item] (map-indexed vector feedback)
            :let [row (.createRow sheet (inc row-index))]
            [column {:keys [value]}] (map-indexed vector columns)]
      (write-cell! styles row column (value item)))
    (.createFreezePane sheet 0 1 0 1)
    (set-column-widths! sheet)
    (with-open [stream (ByteArrayOutputStream.)]
      (.write workbook stream)
      (.toByteArray stream))))
