(ns ataru.application.review-states-spec
  (:require [ataru.application.review-states :as review-states]
            [speclj.core :refer [describe tags it should=]]))

(defn- checked-label [attachment-key multiple-values?]
  (:fi (some (fn [[state labels]] (when (= "checked" state) labels))
             (review-states/attachment-review-types-for attachment-key multiple-values?))))

(defn- states [attachment-key multiple-values?]
  (map first (review-states/attachment-review-types-for attachment-key multiple-values?)))

(describe "attachment-review-types-for"
          (tags :unit :review-states)

  (it "names the checked state of an attachment exempting from the application fee as an exempting attachment"
      (should= "Tarkastettu, vapauttava liite" (checked-label "continuous-residence-permit-front" false))
      (should= "Tarkastettu, vapauttava liite" (checked-label "passport-attachment" false)))

  (it "accepts the attachment key as a keyword"
      (should= "Tarkastettu, vapauttava liite" (checked-label :permanent-residence-permit false)))

  (it "names the checked state of other attachments as checked"
      (should= "Tarkastettu" (checked-label "other-attachment" false))
      (should= "Tarkastettu" (checked-label nil false)))

  (it "names the checked state of a passport attachment of a residence permit option as checked"
      (should= "Tarkastettu" (checked-label "continuous-residence-passport-attachment" false))
      (should= "Tarkastettu" (checked-label "none-passport-attachment" false)))

  (it "keeps the states of the attachment reviews"
      (should= (map first review-states/attachment-hakukohde-review-types)
               (states "continuous-residence-permit-front" false))
      (should= (map first review-states/attachment-hakukohde-review-types)
               (states "other-attachment" false)))

  (it "adds the multiple values state when requested"
      (should= (map first review-states/attachment-hakukohde-review-types-with-multiple-values)
               (states "continuous-residence-permit-front" true))
      (should= "Tarkastettu, vapauttava liite" (checked-label "continuous-residence-permit-front" true))))
