(ns ataru.tarjonta-service.hakukohde
  (:require [ataru.util :as util :refer [non-blank-val]]
            [clojure.string :refer [join blank?]]
            [ataru.tarjonta-service.hakuaika :as hakuaika]
            [ataru.attachment-deadline.attachment-deadline :as attachment-deadline]
            [clojure.walk :as walk]))

(defn- koulutus->str
  [koulutus lang]
  (if-let [classic-name (->> [(-> koulutus :koulutuskoodi-name lang)
                              (->> koulutus
                                   :tutkintonimike-names
                                   (mapv lang)
                                   (remove clojure.string/blank?)
                                   distinct
                                   (clojure.string/join ", "))
                              (:tarkenne koulutus)]
                             (remove clojure.string/blank?)
                             distinct
                             seq)]
    (clojure.string/join " | " classic-name)
    (-> koulutus :koulutusohjelma-name lang)))

(defn- koulutus->str-map
  [koulutus]
  (->> [:fi :sv :en]
       (map (juxt identity (partial koulutus->str koulutus)))
       (remove (comp blank? second))
       (into {})))

(defn- koulutukset->str
  "Produces a condensed string to better identify a hakukohde by its koulutukset"
  [koulutukset]
  (->> koulutukset
       (map koulutus->str-map)
       (apply merge-with conj {:fi [] :sv [] :en []})
       (reduce-kv #(assoc %1 %2 (join "; " %3)) {})
       (remove (comp blank? second))
       (into {})))

(defn- as-hakukohde-name [name tarjoaja-name lang]
  (let [ks [lang :fi :en :sv]]
    [lang (str (non-blank-val name ks) " – " (non-blank-val tarjoaja-name ks))]))

(defn- hakukohde->option
  [{:keys [oid name koulutukset tarjoaja-name kohdejoukko-korkeakoulu?]}]
  (let [langs (distinct (concat [:fi] (keys name) (keys tarjoaja-name)))]
    {:value       oid
     :label       (into {} (map (partial as-hakukohde-name name tarjoaja-name) langs))
     :description (if kohdejoukko-korkeakoulu?
                    {}
                    (koulutukset->str koulutukset))}))

(defn- populate-hakukohteet-field
  [field tarjonta-info]
  (-> field
      (assoc :options (into []
                            (comp
                             (filter #(or (:can-be-applied-to? %) (:archived %)))
                             (map hakukohde->option))
                            (get-in tarjonta-info [:tarjonta :hakukohteet])))
      (assoc-in [:params :max-hakukohteet] (get-in tarjonta-info [:tarjonta :max-hakukohteet]))))

(defn- update-hakukohde-question-on-top-level
  [form tarjonta-info]
  (when-let [hakukohteet-field-idx (util/first-index-of #(= (:fieldType %) "hakukohteet") (:content form))]
    (let [hakukohteet-field (nth (:content form) hakukohteet-field-idx)
          updated-field     (populate-hakukohteet-field hakukohteet-field tarjonta-info)]
      (assoc-in form [:content hakukohteet-field-idx] updated-field))))

(defn- populate-attachment-deadline
  [now hakuajat field-deadlines field]
  (if-let [label (and (= (:fieldType field) "attachment")
                      (or (some-> (get field-deadlines (:id field))
                                  :deadline
                                  hakuaika/date-time->localized-date-time)
                          (some-> (-> field :params :deadline)
                                  (hakuaika/str->date-time)
                                  (hakuaika/date-time->localized-date-time))
                          (some-> (hakuaika/select-hakuaika-for-field now field hakuajat)
                                   attachment-deadline/attachment-deadline-for-hakuaika
                                  (hakuaika/date-time->localized-date-time))))]
    (assoc-in field [:params :deadline-label] label)
    field))

(defn populate-attachment-deadlines [form now hakuajat field-deadlines]
  (update form :content (partial util/map-form-fields
                                 (partial populate-attachment-deadline
                                          now
                                          hakuajat
                                          field-deadlines))))

(defn populate-hakukohde-answer-options [form tarjonta-info]
  ; walking through entire content is very slow for large forms, so try a naive optimization first
  (or
    (update-hakukohde-question-on-top-level form tarjonta-info)
    (update form :content
            (fn [content]
              (walk/prewalk
                (fn [field]
                  (if (= (:fieldType field) "hakukohteet")
                    (populate-hakukohteet-field field tarjonta-info)
                    field))
                content)))))
